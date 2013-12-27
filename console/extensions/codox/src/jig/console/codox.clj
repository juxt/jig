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
   jig
   [jig.console :refer (add-extension ->Boilerplate)]
   [ring.util.response :as ring-resp]
   [bidi.bidi :refer (path-for) :as bidi]
   [hiccup.core :refer (html h)]
   [clojure.java.io :as io]
   codox.writer.html
   codox.utils
   codox.reader)
  (:import (jig Lifecycle)))

(defn codox-project-index-page [request]
  (let [projectname (-> request :route-params :project)

        project (->> request :jig/system :jig/projects (filter (comp (partial = projectname) :name)) first)
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
     (#'codox.writer.html/index-page (assoc options :namespaces namespaces)))))

(defn codox-project-namespace-page [request]
  (let [{:keys [project namespace]} (-> request :route-params)
        project (->> request :jig/system :jig/projects (filter (comp (partial = project) :name)) first)
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
     #_(html [:pre (with-out-str (pprint (first (filter (comp (partial = namespace) name :name) namespaces))))]))))

(defn codox-page-effects [request]
  (->
   (ring-resp/resource-response "codox/js/page_effects.js")
   (ring-resp/content-type "text/javascript")))

(defn codox-jquery [request]
  (->
   (ring-resp/resource-response "codox/js/jquery.min.js")
   (ring-resp/content-type "text/javascript")
   ))

(defn codox-css [request]
  (->
   (ring-resp/resource-response "codox/css/default.css")
   (ring-resp/content-type "text/css")))

(defn codox-page [request]
  (ring-resp/response
   (html [:ul (for [x (->> request :jig/system :jig/projects (map :name))]
                [:li [:a {:href (path-for (:jig.bidi/routes request) codox-project-index-page
                                          :project x)} x]])])))

(deftype JigComponent [config]
  Lifecycle
  (init [_ system]
    (add-extension
     system config
     :route ["" [["" (->Boilerplate codox-page)]
                 [["/" [#"[^/]+" :project] "/"]
                  {"index.html" codox-project-index-page
                   "css/default.css" codox-css
                   "js/page_effects.js" codox-page-effects
                   "js/jquery.min.js" codox-jquery}]
                 [["/" [#"[^/]+" :project] "/" :namespace ".html"] codox-project-namespace-page]]]
     :menuitems [["Codox" codox-page]]
     ))
  (start [_ system] system)
  (stop [_ system] system))
