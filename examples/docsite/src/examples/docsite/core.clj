(ns examples.docsite.core
  (:require
   jig
   [jig.web
    [app :refer (add-routes)]]
   [io.pedestal.service.interceptor :refer (defbefore)]
   [ring.util.response :as ring-resp]
   [io.pedestal.service.http.body-params :as body-params]
   [io.pedestal.service.http :as bootstrap]
   [clojure.tools.logging :refer :all])
  (:import (jig Lifecycle)))

(defbefore readme-page
  [{:keys [request system url-for] :as context}]
  (assoc context
    :response
    (ring-resp/response
     "Hello World!")))

(defbefore root-page
  [{:keys [request system url-for] :as context}]
  (assoc context :response
         (ring-resp/redirect (url-for ::readme-page))))

(deftype ReadmeComponent [config]
  Lifecycle
  (init [_ system]
    (infof "Initialising docsite with config %s" config)
    (add-routes system config
                [^:interceptors [(body-params/body-params)
                                 bootstrap/html-body]
                 ["/" {:get root-page}]
                 ["/readme.html" {:get readme-page}]]))

  (start [_ system] system)

  (stop [_ system] system)
  )
