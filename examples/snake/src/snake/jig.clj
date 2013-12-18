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

(ns snake.jig
  (:require
   jig
   [snake.game :refer (create-handler)]
   [snake.game-model :refer (create-game-state)])
  (:import (jig Lifecycle)))

(deftype SnakeExample [config]
  Lifecycle
  (init [_ system]
    (println "Placing handler in system under " (:handler config))
    (let [!state (create-game-state)]
      (-> system
          (assoc-in [(:jig/id config) :state] !state)
          (assoc-in [(:handler config)] (create-handler !state))
          (update-in [:jig/examples] conj config))))
  (start [_ system]
    system)
  (stop [_ system] system))
