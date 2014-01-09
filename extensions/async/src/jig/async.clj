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

(ns jig.async
  (:require
   jig
   [clojure.core.async :refer (chan >!! close!) :as async]
   [clojure.tools.logging :refer :all])
  (:import
   (jig Lifecycle)))

(defn make-channel [{:keys [buffer size]}]
  (cond
   buffer (chan
           (case buffer
             :sliding (async/sliding-buffer size)
             :dropping (async/dropping-buffer size)
             (throw (ex-info "Unknown buffer type" {:buffer buffer}))))
   size (chan (async/buffer size))
   :otherwise (chan)))

(deftype Channel [config]
  Lifecycle
  (init [_ system]
    (assoc-in system
              [(:jig/id config) :channel]
              (make-channel config)))
  (start [_ system] system)
  (stop [_ system]
    (close! (get-in system [(:jig/id config) :channel]))
    system))
