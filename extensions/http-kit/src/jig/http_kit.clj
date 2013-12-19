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

(ns jig.http-kit
  (:require
   jig
   [clojure.java.io :refer (resource)]
   [clojure.tools.logging :refer (debugf)]
   [clojure.core.cache :as cache]
   [clojure.tools.logging :refer :all]
   [org.httpkit.server :refer (run-server)])
  (:import (jig Lifecycle)))

(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (if-let [handler (some (comp :jig.ring/handler user/system) (:jig/dependencies config))]
      (let [server (run-server handler {:port (:port config)})]
        (assoc-in system [(:jig/id config) :server] server))
      system))
  (stop [_ system]
    (when-let [server (get-in system [(:jig/id config) :server])]
      ;; Stop the server by calling the function
      (infof "Stopping http-kit server")
      (server))
    system))

(keys user/system)
