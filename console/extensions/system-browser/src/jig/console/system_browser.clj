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

(ns jig.console.system-browser
  (:require
   [clojure.string :as string]
   [jig.console :refer (add-extension ->Boilerplate)]
   [liberator.core :refer (defresource resource)]
   [ring.util.codec :as codec]
   [ring.middleware.params :refer (wrap-params)]
   [bidi.bidi :refer (path-for ->WrapMiddleware)]
   [hiccup.core :refer (html h)]
   [clojure.pprint :refer (pprint)]
   jig)
  (:import (jig Lifecycle)))

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

(defprotocol Renderable
  (render [_ path]))

;; TODO Need to use get-in*
(defn get-in* [m ks]
  (if ks
    (get-in* (cond (map? m) (get m (first ks))
                   (coll? m) (nth m (first ks))
                   :otherwise nil)
             (next ks))
    m))

(defn render-system-map [path mm]
  (if (every? (comp keyword? first) mm)
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
              {:href (str "?path=" (codec/url-encode (str path (when-not (empty? path) "/") (str section ":" m))))}
              m]
             " "
             [:small
              (let [t (type o)]
                (if t (.getName t) "none"))]]
            [:p (inline-include o)])))))
    [:p (str mm)]))

(extend-protocol Renderable
  clojure.lang.PersistentArrayMap
  (render [m path] (render-system-map path m))
  clojure.lang.PersistentHashMap
  (render [m path] (render-system-map path m))
  clojure.lang.PersistentList
  (render [l path] (list (interpose [:hr] (map #(render % path) l))))
  clojure.lang.PersistentVector
  (render [l path] (list (interpose [:hr] (map #(render % path) l))))
  clojure.lang.LazySeq
  (render [l path] (list (interpose [:hr] (map #(render %1 (str path (when-not (empty? path) "/") %2)) l (range)))))
  clojure.lang.Atom
  (render [l path] [:div
                    [:p "Atom containing " (.getName (type @l))]
                    [:p (inline-include @l)]
                    [:pre (with-out-str (pprint @l))]])
  String
  (render [s path] [:p s])
  bidi.bidi.WrapMiddleware
  (render [o path] (render (:matched o) path))
  bidi.bidi.Redirect
  (render [o path] [:p "Redirect to " (render (:target o) path)])
  nil
  (render [o path] [:p "empty"])
  Object
  (render [o path] [:p (type o) " : " (str o)]))

(defresource index
  :available-media-types ["text/html"]
  :handle-ok
  (fn [{{:keys [media-type]} :representation
        {route-params :route-params :as request} :request
        :as ctx}]
    (let [
          path (some-> request :query-params (get "path"))
          path-toks (some-> path codec/url-decode (string/split #"/"))
          keyword-path (when path-toks
                         (map (comp
                               (partial apply keyword)
                               (partial remove empty?)
                               #(string/split % #":"))
                              path-toks))]
      (case media-type
        "text/html"
        (html
         [:h1 "System"]
         ;; This is a special case where we need the (finally
         ;; manifested) system in the user namespace
         (render (get-in user/system keyword-path) path)
         ;;[:pre (with-out-str (pprint (get-in user/system keyword-path)))]
         )))))

(deftype SystemBrowser [config]
    Lifecycle
    (init [_ system]
      (add-extension system config
                     :route ["/" [["index" (->Boilerplate (->WrapMiddleware index wrap-params))]]]
                     :menuitems [["System" index]]))
    (start [_ system] system)
    (stop [_ system] system)
    )
