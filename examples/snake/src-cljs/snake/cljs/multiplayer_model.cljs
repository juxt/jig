(ns snake.cljs.multiplayer-model
  (:require [cljs.core.async :as a]
            [chord.client :refer [ws-ch]]
            [cljs.reader :refer [read-string]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn ws-url [path]
  (str "ws://" (.-host js/location) path))

(defn watch-state! [server-conn !game]
  ;; TODO every time we receive a message from the server, update the game state
  
  )

(defn send-commands! [server-conn command-ch]
  ;; TODO send our commands to the server
  
  )

(defn wire-up-model! [!game command-ch]
  (go
   (doto (a/<! (ws-ch (ws-url "/snake")))
     (watch-state! !game)
     (send-commands! command-ch))))

(comment
  "Example !game value:"

  {:clients {"0b9a9cf8-abd2-47e0-b241-4de37312edde"
             {:snake [[10 4] [10 5] [10 6]],
              :direction :up},
    
             "2f594c2a-123e-4352-98a5-7e9621da9ec2"
             {:snake [[16 26] [17 26]],
              :direction :up}},

   :my-id "2f594c2a-123e-4352-98a5-7e9621da9ec2"

   :apples (set [[11 22] [24 9] [7 3] [34 0] [0 28] [18 17] [30 34] [13 13] [6 13] [4 13]])})
