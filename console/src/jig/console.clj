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
   [clojure.string :as string]
   [clojure.java.io :as io]
   jig
   [jig
    [reset :as reset]]
   [jig.web
    [app :refer (add-routes)]
    [stencil :refer (get-template link-to-stencil-loader)]]
   [jig.console
    [codox :as codox]]
   [io.pedestal.service.interceptor :as interceptor :refer (defbefore definterceptorfn before defhandler)]
   [ring.util.response :as ring-resp]
   [ring.util.codec :as codec]
   [io.pedestal.service.http.body-params :as body-params]
   [io.pedestal.service.http :as bootstrap]
   [stencil.core :as stencil]
   [garden.core :refer (css)]
   [garden.units :refer (px pt em percent)]
   [garden.color :refer (hsl rgb)]
   [hiccup.core :refer (html h)]
   [liberator.core :refer (defresource resource)]
   [endophile.core :refer (mp to-clj)]
   [jig.console
    [util :refer (emit-element)]])
  (:import (jig Lifecycle)))

(defn has-examples? [system]
  (not (empty? (:jig/examples system))))

(defn menu [system]
  (remove nil?
          [["System" ::system]
;;           ["Config" nil]
;;           ["Components" nil]
           ["Codox" ::codox-page]
           [(str \T \O \D \O \s) ::todos-page]
;;           ["Logs" nil]
;;           ["Testing" nil]
;;           ["Structure" nil]
;;           ["Stats" nil]
;;           ["Tracing" nil]
;;           ["Profiling" nil]
;;           ["Visualisations" nil]
;;           ["Help" nil]
           (when (has-examples? system) ["Examples" ::examples-page])]))

(defn render-page [system component route-name url-for web-context content]
  (stencil/render
   (get-template system component "templates/page.html")
   {:ctx web-context
    :content (constantly content)
    :menu (for [[name link] (menu system)]
            {:listitem
             (html (cond
                    (= name "Codox")
                    [:li.dropdown
                     [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} name [:b.caret]]
                     [:ul {:class "dropdown-menu"}
                      (for [proj (->> system :jig/projects (map :name))]
                        [:li [:a {:href (url-for :jig.console.codox/codox-project-index-page
                                                 :params {:project proj})} proj]]
                        )]]
                    :otherwise
                    [:li (when (= route-name link) {:class "active"})
                     [:a {:href (if link (url-for link) "/")} name]]))})}))

(defn page-response [context content]
  (assoc context :response
         (ring-resp/response
          (render-page
           (:system context)
           (:component context)
           (-> context :route :route-name)
           (:url-for context)
           (let [ctx (get-in context [:component :jig.web/context])]
             (if (= ctx "/") "" ctx))
           content))))

(defbefore css-page [context]
  (assoc context
    :response
    (-> (ring-resp/response
         (css
          [:body {:padding (px 30)}]
          [:h2 {:margin-top (px 40)}]))
        (ring-resp/content-type "text/css"))))

(defbefore index-page [context]
  (page-response context (->> "README.md" slurp mp to-clj
                              (map emit-element) dorun with-out-str)))

(defmulti inline-include (fn [o] (cond (map? o) :map (coll? o) :coll (string? o) :str)))

(defmethod inline-include :map [o]
  (when (and (<  (count o) 20)
             (every? (fn [[k v]] (not (coll? v))) (seq o)))
    [:table
     (for [[k v] (seq o)]

       [:tr
        [:td {:style "border: 1px solid black; padding: 2px 4px"} (str (if-let [ns (namespace k)] (str ns "/")) (name k))]
        [:td {:style "border: 1px solid black; padding: 2px 4px"} [:code v]]])]))


(defmethod inline-include :string [o]
  (when (< (count o) 400)
    [:code (str o)]))

(defmethod inline-include :default [o]
  nil)

(defbefore examples-page [{:keys [system url-for] :as context}]
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

(defn render-system-map [url-for path mm]
  (for [[section members]
        (->> mm
             keys
             (group-by namespace)
             (sort-by (comp (juxt (partial = "jig") ; jig last
                                  identity)
                            first)))]
    (list
     (when section [:h2 section])
     (for [m (sort (map name members))]
       (let [o (get mm (keyword section m))]
         (list
          [:h3
           [:a
            {:href (url-for ::system :query-params
                            {:path (str path (when-not (empty? path) "/") (str section ":" m))})}
            m]
           " "
           [:small
            (let [t (type o)]
              (if t (.getName t) "none"))]]
          [:p (inline-include o)]))))))

(defn create-system-handler [config]
  (let [handler
        (resource
         :available-media-types ["text/html"]
         :handle-ok
         (fn [{{:keys [media-type]} :representation :as ctx}]
           (let [url-for (-> ctx :request ::url-for)
                 path (some-> ctx :request :query-params :path codec/url-decode (string/split #"/"))
                 keyword-path (when path
                                (map (comp
                                      (partial apply keyword)
                                      (partial remove empty?)
                                      #(string/split % #":"))
                                     path))]
             (case media-type
               "text/html"
               (render-page
                (-> ctx :request ::system)
                (-> ctx :request ::component)
                (-> ctx :request ::route-name)
                (-> ctx :request ::url-for)
                (-> ctx :request ::web-context)
                (html
                 [:h1 "System"]
                 (let [o (-> ctx :request ::system (get-in keyword-path))]
                   (cond (map? o) (render-system-map url-for (some-> ctx :request :query-params :path) o)
                         (coll? o) (list (interpose [:hr] (map (partial render-system-map url-for (some-> ctx :request :query-params :path)) o)))
                         :otherwise [:pre (with-out-str (pprint o))]
                         ))
                 ))))))]
    (before
     ::system
     (fn [context]
       (assoc context :response
              (handler (assoc (:request context)
                         ::system (-> context :system)
                         ::url-for (-> context :url-for)
                         ::component (-> context :component)
                         ::route-name (-> context :route :route-name)
                         ::web-context (let [ctx (get-in context [:app :jig.web/context])]
                                         (if (= ctx "/") "" ctx)))))))))


(defn find-todo-lines [f]
  (filter :todo
        (map
         (partial zipmap [:file :line :todo])
         (map vector
              (repeat (str f))
              (map inc (range))
              (map #(second (re-find (re-pattern (str \T \O \D \O \: \? \\ \s \* \( \. \* \))) %)) (line-seq (io/reader f)))
              ))))

(defn extract-snippet [file line]
  (apply str
         (interpose \newline
                    (->> file (io/reader) line-seq (drop (- line 5)) (take 10)))))

(defn todo-finder [dir]
  (apply concat
         (for [f (.listFiles dir)]
           (cond
            (.isDirectory f) (todo-finder f)
            (.isFile f) (find-todo-lines f)))))


(defbefore todos-page [{:keys [url-for system component] :as context}]
  (page-response context
                 (html
                  [:h1 (str \T \O \D \O \s)]
                  (for [project (-> system :jig/projects)]
                    (list
                     [:h2 (:name project)]
                     (let [paths (->> project :project :source-paths (map (comp (memfn getCanonicalFile) io/file)))
                           todos (mapcat todo-finder paths)]
                       (list
                        [:p (format "%d remaining tasks" (count todos))]
                        [:ul
                         (for [{:keys [file line todo]} todos]
                           [:div
                            [:h4 (str \T \O \D \O)]
                            [:p [:i todo]]
                            [:p "File: " file]
                            [:p "Line: " line]
                            [:pre (extract-snippet file line)]
                            ])]

                        #_[:pre (with-out-str (pprint (mapcat todo-finder paths)))]
                        )

                       )
                     #_[:pre (with-out-str (pprint (-> project :project)))])

                    )

                  )))

(defbefore admin-page [{:keys [url-for system] :as context}]
  (page-response context
                 (html
                  [:h1 "Jig Console"]
                  [:p "Press this button to perform a system reset"]
                  [:form {:method "POST" :action (url-for ::post-reset)} [:input {:type "submit" :value "RESET"}]])))

(defbefore post-reset [{:keys [url-for system] :as context}]
  (reset/reset-via-nrepl system 40) ;; 40ms should be plenty to return a response
  (assoc context :response
         (ring-resp/redirect (url-for ::admin-page))))

(defbefore root-page
  [{:keys [request system url-for] :as context}]
  (assoc context :response
         (ring-resp/redirect (url-for ::index-page))))

(definterceptorfn static
  [name root-path & [opts]]
  (interceptor/handler
   name
   (fn [req]
     (infof "Request for static is %s" req)
     (ring-resp/file-response
      (codec/url-decode (get-in req [:path-params :path]))
      {:root root-path, :index-files? true, :allow-symlinks? false}))))

(deftype Console [config]
  Lifecycle
  (init [_ system]
    (-> system
        (add-routes
         config
         ["/" {:get root-page}
          ["/index" ^:interceptors [bootstrap/html-body] {:get index-page}]
          ["/console" ^:interceptors [bootstrap/html-body] {:get admin-page}]
          ["/todos" ^:interceptors [bootstrap/html-body] {:get todos-page}]

          ["/codox" ^:interceptors [bootstrap/html-body] {:get codox/codox-page}]
          ["/codox/:project/js/page_effects.js" {:get codox/codox-page-effects}]
          ["/codox/:project/js/jquery.min.js" {:get codox/codox-jquery}]
          ["/codox/:project/css/default.css" {:get codox/codox-css}]
          ["/codox/:project/index.html" ^:interceptors [bootstrap/html-body] {:get codox/codox-project-index-page}]
          ["/codox/:project/:namespace" ^:interceptors [bootstrap/html-body] {:get codox/codox-project-namespace-page}]

          ["/system" {:any (create-system-handler config)}]
          ["/examples" ^:interceptors [bootstrap/html-body] {:get examples-page}]
          ["/reset" {:post post-reset}]
          ["/console/resources/assets/*path" {:get (static ::resources "console/resources/assets")}]
          ["/bootstrap/*path" {:get (static ::bootstrap "console/resources/assets/bootstrap")}]
          ["/jig.css" {:get css-page}]])

        (link-to-stencil-loader config)))

  (start [_ system] system)
  (stop [_ system] system))
