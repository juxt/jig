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

(ns mqtt-broker
  (:require
   jig
   [clojure.tools.logging :refer :all])
  (:import
   (io.netty.channel ChannelHandlerAdapter)
   (jig Lifecycle)))

(defn reply [ctx msg]
  (infof "Replying to MQTT message with response: %s" msg)
  (doto ctx
    (.write msg)
    (.flush)))

(deftype MqttHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (debugf "Adding netty handler to system at %s" [(:jig/id config) :jig.netty/handler-factory])
    (assoc-in
     system
     [(:jig/id config) :jig.netty/handler-factory]
     #(proxy [ChannelHandlerAdapter] []
        (channelRead [ctx msg]
          (case (:type msg)
            :connect (reply ctx {:type :connack})
            :pingreq (reply ctx {:type :pingresp})
            :publish (infof "PUBLISH MESSAGE: topic is %s, payload is '%s'" (:topic msg) (String. (:payload msg)))
            :disconnect (.close ctx)
            (throw (ex-info (format "TODO: handle type: %s" (:type msg)) msg))))

        (exceptionCaught [ctx cause]
          (.printStackTrace cause)
          (.close ctx)))))
  (stop [_ system] system))
