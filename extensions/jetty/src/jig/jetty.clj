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

(ns jig.jetty
  (:require
   jig
   [ring.adapter.jetty :refer (run-jetty)]
   )
  (:import (jig Lifecycle)))

(defn wrap-system
  "Add the system map to the request so it's available to Ring handlers."
  [h system]
  (fn [req]
    (h (assoc req :jig/system system))))

(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (if-let [handler (some (comp :jig.ring/handler system) (:jig/dependencies config))]
      (assoc-in system
                [(:jig/id config) :server]
                (-> handler
                    (wrap-system system)
                    (run-jetty (merge {:port 8080 :join? false} config))))
      (throw
       (ex-info "Jetty won't start because ::handler missing, one of its dependencies should add it"
                config))))
  (stop [_ system]
    (when-let [server (get-in system [(:jig/id config) :server])]
      (.stop server))
    (dissoc system (:jig/id config))))
