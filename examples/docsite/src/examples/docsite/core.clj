(ns examples.docsite.core
  (:require
   [clojure.tools.logging :refer :all]
   jig
   [jig.web
    [app :refer (add-routes)]]
   [io.pedestal.service.interceptor :as interceptor :refer (defbefore definterceptorfn)]
   [ring.util.response :as ring-resp]
   [ring.util.codec :as codec]
   [io.pedestal.service.http.body-params :as body-params]
   [io.pedestal.service.http :as bootstrap]
   [stencil.core :as stencil]
   [garden.core :refer (css)]
   [garden.units :refer (px pt em percent)]
   [garden.color :refer (hsl rgb)]
   [examples.docsite
    [markdown :refer (markdown)]]
   )
  (:import (jig Lifecycle)))

(defn page-response [context content]
  (assoc context :response
         (ring-resp/response
          (stencil/render-file
           "templates/page.html"
           {:ctx (let [ctx (get-in context [:app :jig.web/context])]
                   (if (= ctx "/") "" ctx))
            :content (constantly content)}))))

(defbefore css-page [context]
  (assoc context
    :response
    (-> (ring-resp/response
         (css
          [:body {:padding (px 30)}]
          [:.navbar {:margin-bottom (px 30)}]
          ))
        (ring-resp/content-type "text/css"))))

(defbefore index-page [context]
  (page-response context (markdown (slurp "README.md"))))

(defbefore root-page
  [{:keys [request system url-for] :as context}]
  (assoc context :response
         (ring-resp/redirect (url-for ::index-page))))

(definterceptorfn static
  [root-path & [opts]]
  (interceptor/handler
   ::static
   (fn [req]
     (infof "Request for static is %s" req)
     (ring-resp/file-response
      (codec/url-decode (get-in req [:path-params :static]))
      {:root root-path, :index-files? true, :allow-symlinks? false}))))

(deftype ReadmeComponent [config]
  Lifecycle
  (init [_ system]
    (add-routes system config
                ["/" {:get root-page}
                 ^:interceptors [(body-params/body-params)
                                 bootstrap/html-body]
                 ["/index.html" {:get index-page}]
                 ["/resources/assets/*static" {:get (static "resources/assets")}]
                 ["/jig.css" {:get css-page}]]))

  (start [_ system]
    (let [cache (clojure.core.cache/lru-cache-factory {})]
      (stencil.loader/set-cache cache)
      (assoc system :stencil-cache cache)))

  (stop [_ system] system)
  )
