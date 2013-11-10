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

(ns jig.console.core
  (:require
   [clojure.tools.logging :refer :all]
   jig
   [jig
    [reset :as reset]]
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
   [hiccup.core :refer (html)]
   [jig.console
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

(defbefore admin-page [{:keys [url-for system] :as context}]
  (page-response context
                 (html
                  [:h1 "Jig system administration"]
                  [:form {:method "POST" :action (url-for ::post-reset)} [:input {:type "submit" :value "Reload"}]])))

(defbefore post-reset [{:keys [url-for system] :as context}]
  (reset/reset-via-nrepl system 40) ;; 40ms should be plenty to return a response
  (assoc context :response
         (ring-resp/redirect (url-for ::admin-page))))

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
                 ["/admin.html" {:get admin-page}]
                 ["/reset" {:post post-reset}]
                 ["/resources/assets/*static" {:get (static "console/resources/assets")}]
                 ["/jig.css" {:get css-page}]]))

  (start [_ system] system)

  (stop [_ system] system)
  )
