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

(ns jig.httpkit
  (:require
   jig
   [org.httpkit.server :refer (run-server)])
  (:import
   (jig Lifecycle)))

(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (println "Getting handler in system under " (:handler config))
    (let [handler (get-in system [(:handler config)])
          _ (println "handler is " handler)
          server (run-server handler {:port (:port config)})]
      (assoc-in system [(:jig/id config) :server] server)))
  (stop [_ system]
    ;; Stop the server by calling the function
    ((get-in system [(:jig/id config) :server]))
    system))
