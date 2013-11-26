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

(ns jig.mqtt
  (:require
   jig
   [clojurewerkz.machine-head.client :as mh]
   [clojure.core.async :refer (chan >!! close!)]
   [clojure.tools.logging :refer :all])
  (:import (jig Lifecycle)))

(deftype MqttClient [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (assoc system ::machine-head-client (mh/connect (:uri config) (mh/generate-id))))
  (stop [_ system]
    (when-let [client (::machine-head-client system)]
      (mh/disconnect client))
    (dissoc system ::machine-head-client)))

(deftype MqttSubscriber [config]
  Lifecycle
  (init [_ system]
    (let [ch (chan (or (:channel-size config) 100))]
      (assoc-in system [:jig/channels (:channel config)] ch)))
  (start [_ system]
    (let [ch (get-in system [:jig/channels (:channel config)])]
      (infof "MQTT, client is %s, topics are %s" (::machine-head-client system) (:topics config))
      (mh/subscribe
       (::machine-head-client system)
       (:topics config)
       (fn [topic meta payload]
         (infof "Received message on topic %s: %s" topic (String. payload))
         (>!! ch {:topic topic :meta meta :payload payload}))))
    system)
  (stop [_ system]
    (let [client (::machine-head-client system)]
      (mh/unsubscribe client (:topics config)))
    (close! (get-in system [:jig/channels (:channel config)]))
    (update-in system [:jig/channels] dissoc (:channel config))))

(alter-meta!
 (find-ns 'jig.mqtt)
 assoc
 :doc "MQTT components"
 :jig/components
 [{:component 'jig.mqtt/MqttClient
   :doc "Set up a 'Machine Head' MQTT client"
   :configuration {:uri {:doc "URI to the MQTT broker"
                         :required true}}
   :provides {:init [::machine-head-client]}
   }
  {:component 'jig.mqtt/MqttSubscriber
   :doc "Subscribe to MQTT topics and place messages on a channel"
   :configuration {:channel-size {:doc "The size of the core.async channel"
                                  :required false}
                   :topics {:doc "Topics to subscribe to"
                            :required true}
                   :channel {:doc "A key within :jig/channels"}}
   :requires {:start [::machine-head-client]}
   }])
