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

(ns jig.web.sse
  (:require
   [io.pedestal.service.http.sse :as sse]
   [jig.web.app :refer (add-routes)]
   [clojure.core
    [async :refer :all]]
   [clojure.tools
    [logging :refer :all]]
   jig)
  (:import (jig Lifecycle)))

(defn bridge-events-to-sse-subscribers
  "Continuously take events from the channel and send the event to the
  subscribers. The subscribers are supplied in an atom and those which
  return a java.io.IOException are removed."
  [channels subscribers]
  (loop []
    (when-let [[msg _] (alts!! channels)]
      (debugf "Delivering message to %d SSE subscribers: %s"
              (count @subscribers) (:payload msg))
      (swap! subscribers
             (fn [subs]
               (debugf "Sending message to SSE subscribers: %s" msg)
               (doall ;; don't be lazy, otherwise events can be sent through in batches
                (keep ;; those subscribers which don't barf
                 #(try
                    (sse/send-event % "message" (pr-str msg))
                    % ;; return the subscriber to keep it
                    (catch java.io.IOException ioe
                      ;; return nil to remove it
                      ))
                 subs))))
      (recur))))

(defn get-channel [system k]
  (let [ch (get-in system [:jig/channels k])]
    (assert ch (format "Expecting to see input channel registered in system at path %s, but found none, channels are [%s]" [:jig/channels k] (interpose "," (keys (:jig/channels system)))))
    ch))

(deftype ServerSentEventBridge [config]
  Lifecycle
  (init [_ system]
    (let [channels (map (partial get-channel system) (or (:channel-keys config) [(:channel-key config)] []))
          subscribers (atom [])
          sse-bridge-thread (when (pos? (count channels))
                              (Thread. ^Runnable #(bridge-events-to-sse-subscribers channels subscribers)))]
      (-> system
          (add-routes
           config
           [["/events" {:get [::events (sse/start-event-stream (partial swap! subscribers conj))]}]])
          ;; Put messages on this channel to send to SSE subscribers
          (assoc ::subscribers subscribers)
          ;; For starting and stopping the service
          (assoc ::bridge-thread sse-bridge-thread))))

  (start [_ {t ::bridge-thread :as system}]
    (when t (.start t))
    system)

  (stop [_ {t ::bridge-thread subscribers ::subscribers :as system}]
    (when t (.stop t))
    (doseq [sub @subscribers]
      (sse/end-event-stream sub))
    system))
