(ns snake.game
  (:require
   [org.httpkit.server :refer (with-channel websocket? on-close on-receive send!)]
   [hiccup.page :refer [html5 include-css include-js]]
   [compojure.core :refer [routes GET]]
   [compojure.route :refer [files]]
   [ring.util.response :refer [response resource-response file-response content-type]]
   [snake.game-model :refer (connect-client)])
  )

(defn page-frame []
  (html5
   [:head
    [:title "Snake"]
    (include-js "/jquery.js")
    (include-js "/bootstrap.js")
    (include-css "/bootstrap.css")
    (include-js "/js/snake.js")]
   [:body
    [:h1 "Snake"]
    [:div.container
     [:div#content]
     ]]))

(defn snake-websocket [!state]
  (fn [req]
    (with-channel req conn
      (connect-client !state conn))))

(defn create-handler [!state]
  (routes
   (GET "/" [] (response (page-frame)))
   (GET "/bootstrap.css" [] (resource-response "assets/bootstrap/css/bootstrap.min.css"))
   (GET "/bootstrap.js" [] (resource-response "assets/bootstrap/js/bootstrap.min.js"))
   (GET "/jquery.js" [] (resource-response "assets/jquery/jquery-1.10.2.min.js"))
   (GET "/snake" [] (snake-websocket !state))
   (GET "/js/snake.js.map" []  (-> (file-response "examples/snake/target/js/snake.js.map")
                               (content-type "application/javascript")))
   (files "/js" {:root "examples/snake/target/js"})))
