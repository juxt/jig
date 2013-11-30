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
   [clojure.core.cache :as cache]
   [stencil.loader :as loader]
   [stencil.core :as stencil]
   [stencil.parser :refer (parse)])
  (:import (jig Lifecycle)))

;; These functions need to be redefined to allow multiple instances of a
;; stencil cache to co-exist, so stencil caches can be used by multiple
;; independent components.
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
  ([dynamic-template-store parsed-template-cache template-name]
     (load dynamic-template-store parsed-template-cache template-name nil identity))
  ([dynamic-template-store parsed-template-cache template-name template-variant variant-fn]
     (if-let [cached (cache-get parsed-template-cache template-name template-variant)]
       (:parsed cached)
       ;; It wasn't cached, so we have to load it. Try dynamic store first.
       (if-let [dynamic-src (get @dynamic-template-store template-name)]
         ;; If found, parse and cache it, then return it.
         (cache parsed-template-cache template-name template-variant (variant-fn dynamic-src))
         ;; Otherwise, try to load it from disk.
         (if-let [file-url (loader/find-file template-name)]
           (let [template-src (slurp file-url)]
             (cache parsed-template-cache
                    template-name
                    template-variant
                    (variant-fn template-src))))))))

;; Stencil is a little painful in development because it caches
;; templates for performance (which is great in production). But we can
;; renew the cache on init.
(deftype StencilLoader [config]
  Lifecycle
  (init [_ system]
    (->
     system
     (assoc-in
      [(:jig/id config)
       :jig/stencil-loader]
      (let [dynamic-template-store (atom {})
            parsed-template-cache (atom (cache/lru-cache-factory {}))]
        (fn
          ([template-name]
             (load dynamic-template-store parsed-template-cache template-name))
          ([template-name template-variant variant-fn]
             (load dynamic-template-store parsed-template-cache
                   template-name template-name variant-fn)))))))
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
