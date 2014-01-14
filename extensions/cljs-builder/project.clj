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

(load-file "project-header.clj")

(def version (get-version))

(defproject jig/cljs-builder version
  :description "A Jig extension that provides ClojureScript compilation support"
  :url "https://github.com/juxt/jig/tree/master/extensions/cljs-builder"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[jig/protocols ~version]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/java.classpath "0.2.1"]])
