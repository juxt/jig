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

(ns jig.cljs
  (:require
   jig
   [jig.web.app :refer (add-routes)]
   [clojure.java.io :refer (file)]
   [clojure.tools
    [logging :refer :all]]
   [clojure.pprint :refer (pprint)]
   [io.pedestal.service.interceptor :as interceptor :refer (definterceptorfn)]
   [ring.util.response :as ring-resp]
   [ring.util.codec :as codec]
   [cljs
    [closure :refer (build)]
    [env :refer (default-compiler-env)]]
   [clojure.java.classpath :as classpath :refer (classpath)]
   [clojure.tools.namespace.find :as ns-find]
   [clojure.tools.namespace.file :as ns-file]
   [io.pedestal.app-tools.compile :as pedcompile]
   [cljs.closure :refer (Compilable dependency-order -compile)])
  (:import
   (jig Lifecycle)))

(defn deleteDir [f]
  (infof "Deleting %s" f)
  (if (.isDirectory f)
    (doseq [f (.listFiles f)]
      (deleteDir f))
    (.delete f)))

;; Replacements for org.clojure/java.classpath:clojure.java.classpath - these ones take classloaders

(defn classpath-directories
  "Returns a sequence of File objects for the directories on classpath."
  [classloader]
  (filter #(.isDirectory ^java.io.File %) (classpath classloader)))

(defn classpath-jarfiles
  "Replacement for classpath/classpath-jarfiles, this one accepts a classloader."
  [classloader]
  (map #(java.util.jar.JarFile. ^java.io.File %) (filter classpath/jar-file? (classpath classloader))))

;; Replacements for io.pedestal/pedestal.app-tools:io.pedestal.app-tools.compile

(defn shared-files-in-jars
  "Return all Clojure files in jars on the classpath which are marked
  as being shared."
  [classloader]
  (for [jar (classpath-jarfiles classloader)
        file-name (ns-find/clojure-sources-in-jar jar)
        :when (pedcompile/ns-marked-as-shared? jar file-name)]
    {:js-file-name (pedcompile/rename-to-js file-name)
     :tag :cljs-shared-lib
     :compile? true
     :source (java.net.URL. (str "jar:file:" (.getName jar) "!/" file-name))}))

(defn shared-files-in-dirs
  "Return all Clojure files in directories on the classpath which are
  marked as being shared."
  [classloader]
  (for [dir (classpath-directories classloader)
        file (ns-find/find-clojure-sources-in-dir dir)
        :when (pedcompile/ns-marked-as-shared? file)]
    {:js-file-name (pedcompile/js-file-name dir file)
     :tag :cljs-shared
     :compile? true
     :source file}))

(defn project-cljs-files
  "Return all ClojureScript files in directories on the classpath."
  [classloader]
  (for [dir (classpath-directories classloader)
        file (file-seq dir)
        :when (pedcompile/cljs-file? file)]
    {:js-file-name (pedcompile/js-file-name dir file)
     :tag :cljs
     :compile? true
     :source file}))

(defn all-cljs-on-classpath
  "Return all files on the classpath which can be compiled to
  ClojureScript."
  [classloader]
  (let [res
        (concat (shared-files-in-jars classloader)
                (shared-files-in-dirs classloader)
                (project-cljs-files classloader))]
    (debugf "Files to compile: %s" (with-out-str (pprint res)))
    res))

(defn build-sources!
  "OK, this is a rip-off of io.pedestal.app-tools.compile, but it's such
  a lovely function. The rationale for including it in Jig is to be able
  to utilize Pedestal's dataflow, which is coded using a ^:shared
  metadata tag on the ns. This seems to be a Pedestal convention,
  not (yet) a ClojureScript one."
  [sources options compiler-env]
  (build (reify Compilable
           (-compile [_ options]
             ;; Not sure what this does yet, let's disable it
             ;; (force-compilation options sources)
             (dependency-order
              (flatten (map (fn [{:keys [js-file-name source]}]
                              (when (= (type source) java.io.File)
                                (.mkdirs source))
                              (-compile source (assoc options :output-file js-file-name)))
                            (filter :compile? sources))))))
         options
         compiler-env))

;; The Jig components

(deftype Builder [config]
  Lifecycle
  (init [_ system]
    (let [compiler-env-path [:jig/safe (:jig/id config) :compiler-env]
          compiler-env (or (get-in system compiler-env-path) (default-compiler-env))]
      (infof "Building cljs to %s, clean build is %b" (:output-dir config) (:clean-build config))
      (when (:clean-build config)
        (when-let [od (file (:output-dir config))]
          (when (and (.exists od) (.isDirectory od))
            (deleteDir od))))
      (build-sources!
       (all-cljs-on-classpath (-> config :jig/project :classloader))
       (select-keys config [:output-dir :output-to :optimizations :pretty-print :source-map])
       (if (:clean-build config) (default-compiler-env) compiler-env))
      (assoc-in system compiler-env-path compiler-env)))
  (start [_ system]
    system)
  (stop [_ system]
    system))

(definterceptorfn static
  [root-path & [opts]]
  (interceptor/handler
   ::cljs-static
   (fn [req]
     (ring-resp/file-response
      (codec/url-decode (get-in req [:path-params :path]))
      {:root root-path, :index-files? true, :allow-symlinks? false}))))

(deftype FileServer [config]
  Lifecycle
  (init [_ system]
    (let [deps (select-keys (-> system :jig/config :jig/components) (:jig/dependencies config))
          ;; This should be a common pattern, find a dependency that matches
          app (first (filter (fn [[k v]] (= 'jig.web.app/Component (:jig/component v))) deps))
          builder (first (filter (fn [[k v]] (= 'jig.cljs/Builder (:jig/component v))) deps))]
      (add-routes
       system
       (assoc config :jig.web/app-name (first app))
       [["/*path" {:get (static (:output-dir (second builder)))}]])))

  (start [_ system]
    system)
  (stop [_ system]
    system))
