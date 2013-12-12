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

(ns jig.web.stencil
  (:refer-clojure :exclude (load))
  (:require
   jig
   [clojure.java.io :refer (resource)]
   [clojure.tools.logging :refer (debugf)]
   [clojure.core.cache :as cache]
   [stencil.loader :as loader]
   [stencil.core :as stencil]
   [stencil.parser :refer (parse)])
  (:import (jig Lifecycle)))

;; These functions need to be redefined to allow multiple instances of a
;; stencil cache to co-exist, so stencil caches can be used by multiple
;; independent components.
(defn find-file
  "Given a name of a mustache template, attempts to find the corresponding
   file. Returns a URL if found, nil if not. First tries to find
   filename.mustache on the classpath. Failing that, looks for filename on the
   classpath. Note that you can use slashes as path separators to find a file
   in a subdirectory."
  [template-name classloader]
  (debugf "Finding template %s, classloader is %s" template-name (apply str (interpose "," (.getURLs classloader))))
  (if-let [file-url (resource (str template-name ".mustache") classloader)]
    file-url
    (if-let [file-url (resource template-name classloader)]
      file-url)))

(defn cache
  ([parsed-template-cache template-name template-variant template-src]
     (cache parsed-template-cache template-name template-variant template-src (parse template-src)))
  ([parsed-template-cache template-name template-variant template-src parsed-template]
     (swap! parsed-template-cache
            assoc-in [template-name template-variant]
            (loader/template-cache-entry template-src
                                  parsed-template))
     parsed-template))

(defn cache-get
  ([parsed-template-cache template-name]
     (cache-get parsed-template-cache template-name :default))
  ([parsed-template-cache template-name template-variant]
     (get-in @parsed-template-cache [template-name template-variant])))

(defn load
  ([dynamic-template-store parsed-template-cache loader template-name]
     (load dynamic-template-store parsed-template-cache loader template-name nil identity))
  ([dynamic-template-store parsed-template-cache loader template-name template-variant variant-fn]
     (debugf "Loading template %s" template-name)
     (if-let [cached (cache-get parsed-template-cache template-name template-variant)]
       (do
         (debugf "Found in cache-get")
         (:parsed cached))
       ;; It wasn't cached, so we have to load it. Try dynamic store first.
       (if-let [dynamic-src (get @dynamic-template-store template-name)]
         ;; If found, parse and cache it, then return it.
         (do
           (debugf "Found in dynamic store")
           (cache parsed-template-cache template-name template-variant (variant-fn dynamic-src))
           )
         ;; Otherwise, try to load it from disk.
         (if-let [file-url (find-file template-name loader)]
           (do
             (debugf "Template resource found was %s" file-url)
             (let [template-src (slurp file-url)]
               (cache parsed-template-cache
                      template-name
                      template-variant
                      (variant-fn template-src))))
           (debugf "Template not found: %s" template-name))))))

;; Stencil is a little painful in development because it caches
;; templates for performance (which is great in production). But we can
;; renew the cache on init.
(deftype StencilLoader [config]
  Lifecycle
  (init [_ system]
    ;; We will use the project classloader for finding templates in this
    ;; project. This ensures that we do not load the wrong templates due
    ;; to template name conflicts with templates in other projects.
    (let [loader (-> config :jig/project :classloader)]
      (->
       system
       (assoc-in
        [(:jig/id config)
         :jig/stencil-loader]
        (let [dynamic-template-store (atom {})
              parsed-template-cache (atom (cache/lru-cache-factory {}))]
          (fn
            ([template-name]
               (debugf "Loading template %s using component %s" template-name (:jig/id config))
               (load dynamic-template-store parsed-template-cache loader template-name))
            ([template-name template-variant variant-fn]
               (debugf "Loading template %s -- using component %s" template-name (:jig/id config))
               (load dynamic-template-store parsed-template-cache loader
                     template-name template-name variant-fn))))))))
  (start [_ system] system)
  (stop [_ system] system))

(defn get-template [system component template-name]
  (let [f (get-in system [(:jig/id component) :jig/stencil-loader])]
    (if f
      (f template-name)
      (throw (ex-info
              (format "No stencil loader for component: %s" (:jig/id component))
              {:id (:jig/id component)})))))

(defn link-to-stencil-loader
  "There can be multiple stencil caches in a given system. Therefore we
  have to ensure that we specify, in the component that wants to use
  stencil caches, which stencil loader we want to use. We do that by
  specifying a :jig/stencil-loader key pointing to the key of the
  component that provides the StencilLoader."
  [system config]
  (assoc-in system
            [(:jig/id config) :jig/stencil-loader]
            (get-in system [(:jig/stencil-loader config) :jig/stencil-loader])))
