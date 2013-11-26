;; Copyright © 2013, JUXT LTD. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns jig.web.ring
  (:require
   jig
   [clojure.tools.logging :refer :all]
   [compojure.core :refer (routes)]
   [ring.adapter.jetty :refer (run-jetty)])
  (:import (jig Lifecycle)))

(defn wrap-system
  "Add the system map to the request so it's available to Ring handlers."
  [h system]
  (fn [req]
    (h (assoc req :jig/system system))))

(defn wrap-config
  "Add the handler config to the request so it's available to Ring handlers."
  [h config]
  (fn [req]
    (h (assoc req :jig/config config))))

(defn add-handler [system handler config]
  ;; Only conj onto the list, if it exists. Otherwise we may overwrite
  ;; existing handlers if multiple components are using this compojure instance.
  (update-in system [:jig.web.ring/handlers] conj (wrap-config handler config)))


(deftype Jetty [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (if-let [handler (::handler system)]
      (assoc system
        ::server (-> handler
                     (wrap-system system)
                     (run-jetty (merge {:port 8080 :join? false} config))))
      (throw
       (ex-info "Jetty won't start because ::handler missing, one of its dependencies should add it"
                config))))
  (stop [_ system]
    (when-let [server (::server system)]
      (.stop server))
    (dissoc system ::server)))

(deftype Compojure [config]
  Lifecycle
  (init [_ system]
    (if-let [handlers (::handlers system)]
      (assoc system ::handler (apply routes handlers))
      (ex-info "Compojure won't initialise without handlers available. The Compojure component must depend on other components that supply handlers by conj'd (or concat'd) to ::handlers" config)))
  (start [_ system] system)
  (stop [_ system] system))
