(ns snake.game
  (:require
   [org.httpkit.server :refer (with-channel websocket? on-close on-receive send!)]
   [hiccup.page :refer [html5 include-css include-js]]
   [compojure.core :refer [defroutes GET]]
   [compojure.route :refer [resources]]
   [ring.util.response :refer [response]]
   [snake.game-model :refer (connect-client)])
  )

(defn page-frame []
  (html5
   [:head
    [:title "Snake - Likely Clojure School"]
    (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
    (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

    (include-js "/js/snake.js")]
   [:body
    [:div.container
     [:div#content]
     ]]))

(defn snake-websocket [!state]
  (fn [req]
    (with-channel req client-conn
      (fn [conn]
        (connect-client !state conn)
        ))))

(defn create-handler [!state]
  (GET "/" [] (response (page-frame)))
  (GET "/snake" [] (snake-websocket !state))
  (resources "/js" {:root "js"}))
