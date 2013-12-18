(ns snake.cljs.board-widget
  (:require [snake.board :as b]
            [cljs.core.async :as a]
            [dommy.core :as d]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go]]))

(defprotocol BoardComponent
  (board->node [_])
  (focus! [_])

  ;; TODO what else does the board need to do?
  
  )

(def key->command
  {kc/UP :up
   kc/DOWN :down
   kc/LEFT :left
   kc/RIGHT :right})

(defn canvas-board-component []
  (let [canvas-size (* b/block-size-px b/board-size)
        $canvas (node [:canvas {:height canvas-size
                                :width canvas-size
                                :style {:border "1px solid black"}
                                :tabindex 0}])]
    (reify BoardComponent
      (board->node [_]
        (node
         [:div {:style {:margin-top "5em"}}
          $canvas]))
      (focus! [_]
        (go
         (a/<! (a/timeout 200))
         (.focus $canvas)))
      
      ;; TODO implementations of any functions you put in BoardComponent
      
      )))

(defn watch-game! [board !game]
  ;; TODO changes to !game to be reflected on screen
  
  )

(defn bind-commands! [board model-command-ch]
  ;; TODO business-logic commands to be put onto model-command-ch
  
  )

(defn make-board-widget [!game model-command-ch]
  (let [board (doto (canvas-board-component)
                (watch-game! !game)
                (bind-commands! model-command-ch)
                (focus!))]

    ;; TODO you can test your component by putting test commands in here (e.g. try rendering a snake!)
    
    (board->node board)))
