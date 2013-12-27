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

(ns jig.cljs.shared
  (:require
   [clojure.tools.namespace.find :as ns-find]
   [clojure.tools.namespace.file :as ns-file]))

(def ^:dynamic *shared-metadata* :shared)

(defn ns-marked-as-shared?
  "Is the namespace of the given file marked as shared?"
  ([jar file-name]
     (when-let [ns-decl (ns-find/read-ns-decl-from-jarfile-entry jar file-name)]
       (*shared-metadata* (meta (second ns-decl)))))
  ([file-name]
     (when-let [ns-decl (ns-file/read-file-ns-decl file-name)]
       (*shared-metadata* (meta (second ns-decl))))))

(defn rename-to-js
  "Rename any Clojure-based file to a JavaScript file."
  [file-str]
  (clojure.string/replace file-str #".clj\w*$" ".js"))

(defn relative-path
  "Given a directory and a file, return the relative path to the file
  from within this directory."
  [dir file]
  (.substring (.getAbsolutePath file)
              (inc (.length (.getAbsolutePath dir)))))

(defn js-file-name
  "Given a directory and file, return the relative path to the
  JavaScript file."
  [dir file]
  (rename-to-js (relative-path dir file)))

(defn cljs-file?
  "Is the given file a ClojureScript file?"
  [f]
  (and (.isFile f)
       (.endsWith (.getName f) ".cljs")))
