(ns snake.cljs.home
  (:require [dommy.core :as d]
            [snake.cljs.board-widget :refer [make-board-widget]]
            [snake.cljs.multiplayer-model :refer [wire-up-model!]]
            [cljs.core.async :as a])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go]]))

(defn watch-hash! [!hash]
  (add-watch !hash :home-page
             (fn [_ _ _ hash]
               (when (= "#/" hash)
                 (let [!game (atom nil)
                       command-ch (a/chan)]
                   (d/replace-contents! (sel1 :#content)
                                        (make-board-widget !game command-ch))
                   (wire-up-model! !game command-ch))))))
