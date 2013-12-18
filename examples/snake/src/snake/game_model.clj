(ns snake.game-model
  (:require [clojure.core.async :as a :refer [go go-loop] ]
            [snake.board :as b]))

(defn new-apple []
  (repeatedly 2 #(rand-int b/board-size)))

(defn new-apples []
  (set (repeatedly 10 new-apple)))

(defn new-game []
  (let [middle (/ b/board-size 2)]
    {:snake (list [middle middle] [(inc middle) middle])
     :direction :left}))

(def movement-vector
  {:left [-1 0]
   :up [0 -1]
   :right [1 0]
   :down [0 1]})

(defn wrap-around-board [n]
  ;; assuming square board.
  (mod n b/board-size))

(defn new-head [head direction]
  ;; (map + vec1 vec2) is vector addition
  (map (comp wrap-around-board +)
       head
       (movement-vector direction)))

(defn move-snake [{:keys [snake direction] :as game}]
  (let [[head & tail] snake]
    (assoc game
      :snake (cons (new-head head direction) (butlast snake))
      :last-tail (last tail))))

(defn move-snakes [game]
  (update-in game [:clients] (fn [clients]
                               (->> (for [[user-id client] clients]
                                      [user-id (move-snake client)])
                                    (into {})))))

(defn check-apple-collisions [{:keys [clients] :as game}]
  (reduce (fn [{:keys [apples] :as game} user-id]
            (let [[head & _] (get-in game [:clients user-id :snake])]
              (cond-> game
                (contains? apples head)
                (-> (update-in [:apples] disj head)
                    (update-in [:apples] conj (new-apple))
                    (update-in [:clients user-id :snake]
                               concat [(get-in game [:clients user-id :last-tail])])))))
          game
          (keys clients)))


(defn send-state! [{:keys [client-conns clients apples]}]
  (doseq [[user-id conn] client-conns]
    (a/put! conn (pr-str {:clients clients
                          :my-id user-id
                          :apples apples}))))

(defn apply-tick [game]
  (-> game
      move-snakes
      check-apple-collisions
      (doto send-state!)))

(def valid-directions
  {:left #{:up :down}
   :right #{:up :down}
   :up #{:left :right}
   :down #{:left :right}})

(defn valid-direction? [old new]
  ((valid-directions old) new))

(defn apply-command [game user-id command]
  (let [direction (get-in game [:clients user-id :direction])]
    (if-let [new-direction (valid-direction? direction command)]
      (assoc-in game [:clients user-id :direction] new-direction)
      game)))

(defn repeatedly-tick! [!game]
  (go-loop []
    (a/<! (a/timeout 100))
    (swap! !game apply-tick)
    (recur)))

(defn remove-client [game user-id client-conn]
  (-> game
      (update-in [:clients] dissoc user-id)
      (update-in [:client-conns] dissoc user-id)))

(defn apply-commands! [!game user-id client-conn]
  (go-loop []
    (if-let [{:keys [message]} (a/<! client-conn)]
      (let [command (read-string message)]
        (swap! !game apply-command user-id command)
        (recur))

      (swap! !game remove-client user-id client-conn))))

(defn add-client [game user-id client-conn]
  (-> game
      (assoc-in [:clients user-id] (new-game))
      (assoc-in [:client-conns user-id] client-conn)))

(defn create-state []
  (doto (atom {:apples (new-apples)})
    (repeatedly-tick!)))

(defn connect-client [!state client-conn]
  (let [user-id (str (java.util.UUID/randomUUID))]
    (doto !state
      (swap! add-client user-id client-conn)
      (apply-commands! user-id client-conn))))

#_(defn wire-up-model! []
    (fn client-joined! [client-conn]
    (let [user-id (str (java.util.UUID/randomUUID))]
      (doto !game
        (swap! add-client user-id client-conn)
        (apply-commands! user-id client-conn)))))
