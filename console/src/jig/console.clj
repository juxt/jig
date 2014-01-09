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

(ns jig.console
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.pprint :refer (pprint)]
   [clojure.java.io :as io]
   jig
   [jig
    [reset :as reset]]
   [jig.util :refer (satisfying-dependency)]
   [jig.bidi :refer (add-bidi-routes)]
   [bidi.bidi
    :refer (->Redirect ->Resources ->ResourcesMaybe ->WrapMiddleware path-for resolve-handler unresolve-handler)
    :as bidi]
   [ring.util.response :as ring-resp]
   [stencil.core :as stencil]
   [garden.core :refer (css)]
   [garden.units :refer (px pt em percent)]
   [garden.color :refer (hsl rgb)]
   [hiccup.core :refer (html h)]
   [endophile.core :refer (mp to-clj)]
   [jig.console
    [util :refer (emit-element)]])
  (:import
   (jig Lifecycle)
   (bidi.bidi Matched)))

;; Note: This console is not an ideal application to copy since it needs
;; strong coupling to the Jig system. Applications using Jig should
;; avoid tight coupling to Jig-specific entries in the system, which may
;; not be present in production.

(defn has-examples? [system]
  (not (empty? (:jig/examples system))))

(defn menu [system component]
  (remove nil?
          (concat
           (mapcat :menuitems (get-in system [(:jig/id component) :extensions]))
           [(when (has-examples? system) ["Examples" ::examples-page])])))

;; We need to declare the index-page handler now, so we can form a path
;; to it in the boilerplate function below.
(declare index-page)

(defn boilerplate [{loader :template-loader system :jig/system routes :jig.bidi/routes ctx :jig.web/context :as request} content]
  (infof "Request keys are %s" (keys request))
  (stencil/render
           (loader "templates/page.html")
           {:ctx ctx
            :content (constantly content)
            :title "Jig Console"
            :href-home (path-for routes index-page)
            :menu (for [[label handler] (:menu request)]
                    {:listitem
                     (html (cond
                            #_(= label "Codox")
                            #_[:li.dropdown
                               [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} label [:b.caret]]
                               [:ul {:class "dropdown-menu"}
                                (for [proj (->> system :jig/projects (map :name))]
                                  [:li [:a #_{:href (url-for :jig.console.codox/codox-project-index-page
                                                             :params {:project proj})} proj]]
                                  )]]
                            :otherwise
                            [:li (when (= (:uri request) (path-for routes handler)) {:class "active"})
                             [:a (when-let [href (path-for routes handler)] {:href href}) label]]))})}))

(defn css-page [_]
  (-> (ring-resp/response
       (css
        [:body {:padding (px 30)}]
        [:h2 {:margin-top (px 40)}]))
      (ring-resp/content-type "text/css")))

(defn index-page [request]
  (ring-resp/response
   (boilerplate request
                (->> "README.md" slurp mp to-clj
                     (map emit-element) dorun with-out-str))))

#_(defbefore examples-page [{:keys [system url-for] :as context}]
  (page-response
   context
   (html
    [:h1 "Examples"]
    [:dl
     (for [{:keys [name description] uri :jig.example/uri :as conf} (:jig/examples system)]
       (list
        [:h2 [:a {:href uri :target "example"} name]]
        [:dd description]
        [:p "Location: " [:a {:href uri} uri]]
        [:h4 "Configuration"]
        [:pre (with-out-str (pprint (into {} (remove (fn [[k v]] (or (#{"jig" "jig.example"} (namespace k)))) conf))))]))])))

#_(defbefore admin-page [{:keys [url-for system] :as context}]
  (page-response context
                 (html
                  [:h1 "Jig Console"]
                  [:p "Press this button to perform a system reset"]
                  [:form {:method "POST" :action (url-for ::post-reset)} [:input {:type "submit" :value "RESET"}]])))

#_(defbefore post-reset [{:keys [url-for system] :as context}]
  (reset/reset-via-nrepl system 40) ;; 40ms should be plenty to return a response
  (assoc context :response
         (ring-resp/redirect (url-for ::admin-page))))

(def console-builtin-routes
  [""
   [["/" (->Redirect 307 index-page)]
    ["/index" index-page]

    ["/jig.css" css-page]
    ["/bootstrap/" (->Resources {:prefix "assets/bootstrap/"})]
    ["/console/resources/assets/" (->Resources {:prefix "assets/"})]
    ["/debug" (fn [req] {:status 200 :body (with-out-str (pprint (-> req :jig.bidi/routes)))})]
    ["" (->ResourcesMaybe {:prefix "assets/"})]
    ]])

(defn wrap-template-loader [template-loader]
  (fn [h]
    (fn [req]
      (h (assoc req :template-loader template-loader)))))

(defn wrap-menu [system config]
  (fn [h]
    (fn [req]
      (h (assoc req
           :menu (remove nil?
                         (concat
                          (mapcat :menuitems (get-in system [(:jig/id config) :extensions]))
                          [(when (has-examples? system) ["Examples" ::examples-page])])))))))

(deftype Console [config]
  Lifecycle
  (init [_ system] system)

  (start [_ system]
    ;; Look at routes that have been already conj'd into this component's :extensions system entry
    (let [extensions (get-in system [(:jig/id config) :extensions])
          routes (into []
                       (concat [console-builtin-routes]
                               (for [ext extensions
                                     :let [routes (:route ext)] :when routes]
                                 [(or (-> ext :jig/config :jig.web/context) "") [routes]])
                               ;; Here's a good place to add the console-wide 404
                               [[true (fn [req] {:status 404 :body "Console 404: not found"})]]))]

      ;; Look up our stencil loader
      (let [route
            (if-let [{id :jig/id} (satisfying-dependency system config 'jig.stencil/StencilLoader)]
              (if-let [template-loader (get-in system [id :jig.stencil/loader])]
                ["" (->WrapMiddleware routes (wrap-template-loader template-loader))]
                (throw (ex-info (format "Failed to find lookup template loader in system at path %s"
                                        [id :jig.stencil/loader])
                                {:path [id :jig.stencil/loader]})))
              (throw (ex-info (format "Component must depend on a %s component" 'jig.stencil/StencilLoader) {}))
              )]

        (-> system
            (add-bidi-routes config ["" (->WrapMiddleware [route] (wrap-menu system config))]))))

    #_(-> system
          (add-routes
           config
           ["/" {:get root-page}
            ["/examples" ^:interceptors [bootstrap/html-body] {:get examples-page}]
            ["/reset" {:post post-reset}]])))

  (stop [_ system] system))

(defn console-entry?
  "Determine whether a map entry relates to this console component"
  [[k v]]
  (when (= (:jig/component v) 'jig.console/Console) k))

(defn add-extension
  "Options may optionally contain a :route entry for a bidi route
  structure and a :menuitems key to contribute to the main menu."
  [system config & {:as options}]
  (if-let [console-id (->> config :jig/dependencies
                           ;; Get the config of each of this extension's dependencies
                           (select-keys (-> system :jig/config :jig/components))
                           ;; Return the one that matches a console
                           (some console-entry?))]
    (update-in system [console-id :extensions] conj (merge options {:jig/config config}))
    system))

(defn wrap-boilerplate [h]
  (fn [req]
    (if h
      (-> req h (update-in [:body] (partial boilerplate req)))
      (throw (ex-info "No handler" {})))))

;; This record allows us to extend bidi so that we can specify that the
;; output of a route handler be wrapped in the console boilerplate. A
;; usage example can be found in jig.console.todo. We could have used
;; bidi's built-in WrapMiddleware type but this builds in the middleware
;; we want, making it very easy to use.
(defrecord Boilerplate [delegate]
  Matched
  (resolve-handler [this m]
    (when-let [m2 (resolve-handler delegate m)]
      (update-in m2 [:handler] wrap-boilerplate)))
  (unresolve-handler [this m] (unresolve-handler delegate m)))
