;; Copyright © 2013 - 2014, JUXT LTD. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns jig.netty.mqtt
  (:require
   [clojure.tools.logging :refer :all]
   jig
   [mqtt.decoder :refer (make-decoder)]
   [mqtt.encoder :refer (make-encoder)])
  (:import
   (jig Lifecycle)))

(def handler-factory-key :jig.netty/handler-factory)

(deftype MqttDecoder [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (debugf "Adding netty handler to system at %s" [(:jig/id config) handler-factory-key])
    (assoc-in system [(:jig/id config) handler-factory-key] #(make-decoder)))
  (stop [_ system] system))

(deftype MqttEncoder [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (debugf "Adding netty handler to system at %s" [(:jig/id config) handler-factory-key])
    (assoc-in system [(:jig/id config) handler-factory-key] #(make-encoder)))
  (stop [_ system] system))
