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

(ns jig.console.codox
  (:require
   [io.pedestal.service.interceptor :as interceptor :refer (defbefore defhandler)]
   [ring.util.response :as ring-resp]
   [hiccup.core :refer (html h)]
   [clojure.java.io :as io]
   codox.writer.html
   codox.utils
   codox.reader))

(defbefore codox-page [{:keys [system url-for] :as context}]
  (assoc context
    :response
    (-> (ring-resp/response
         (html [:ul (for [x (->> system :jig/projects (map :name))]
                      [:li [:a {:href (url-for ::codox-project-index-page
                                               :params {:project x})} x]])])
         ))))

(defbefore codox-project-index-page [context]
  (assoc context
    :response
    (let [projectname (-> context :request :path-params :project)
          project (->> context :system :jig/projects (filter (comp (partial = projectname) :name)) first)
          lein-project (:project project)

          {:keys [sources include exclude] :as options}
          (-> lein-project
              (select-keys [:name :version :description])
              (merge {:sources ["src"]} ;; Default gets overwritten if set in :codox
                     (get lein-project :codox)))
          namespaces (-> (apply codox.reader/read-namespaces (map (partial io/file (.getParentFile (:project-file project))) sources))
                          (codox.utils/ns-filter include exclude)
                          (codox.utils/add-source-paths sources))]

      (ring-resp/response
       (#'codox.writer.html/index-page (assoc options :namespaces namespaces))))))

(defbefore codox-project-namespace-page [context]
  (assoc context
    :response
    (let [{:keys [project namespace]} (-> context :request :path-params)
          project (->> context :system :jig/projects (filter (comp (partial = project) :name)) first)
          namespace (second (re-matches #"(.*).html" namespace))
          lein-project (:project project)

          {:keys [sources include exclude] :as options}
          (-> lein-project
              (select-keys [:name :version :description])
              (merge {:sources ["src"]} ;; Default gets overwritten if set in :codox
                     (get lein-project :codox)))
          namespaces (-> (apply codox.reader/read-namespaces (map (partial io/file (.getParentFile (:project-file project))) sources))
                          (codox.utils/ns-filter include exclude)
                          (codox.utils/add-source-paths sources))]

      (ring-resp/response
       (#'codox.writer.html/namespace-page (assoc options :namespaces namespaces) (first (filter (comp (partial = namespace) name :name) namespaces)))
       #_(html [:pre (with-out-str (pprint (first (filter (comp (partial = namespace) name :name) namespaces))))])))))

(defhandler codox-page-effects [request]
  (->
   (ring-resp/resource-response "codox/js/page_effects.js")
   (ring-resp/content-type "text/javascript")))

(defhandler codox-jquery [request]
  (->
   (ring-resp/resource-response "codox/js/jquery.min.js")
   (ring-resp/content-type "text/javascript")
   ))

(defhandler codox-css [request]
  (->
   (ring-resp/resource-response "codox/css/default.css")
   (ring-resp/content-type "text/css")))
