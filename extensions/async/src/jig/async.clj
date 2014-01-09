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
   [clojure.core.async :refer (chan >!! close! sliding-buffer dropping-buffer buffer)]
   [clojure.tools.logging :refer :all])
  (:import
   (jig Lifecycle)))

(defn make-buffer [{:keys [buffer size]}]
  (case buffer
    :sliding (sliding-buffer size)
    :dropping (dropping-buffer size)
    (buffer size)))

(deftype Channel [config]
  Lifecycle
  (init [_ system]
    (assoc-in system
              [(:jig/id config) :channel]
              (chan (make-buffer config))))
  (start [_ system] system)
  (stop [_ system]
    (close! (get-in system [(:jig/id config) :channel]))
    system))
