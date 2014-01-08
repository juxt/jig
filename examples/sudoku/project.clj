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

(def jig-version (get-version))

(defproject sudoku "0.1.0-SNAPSHOT"
  :description "An example to demonstrate a simple jig running a website with Jetty and Compojure routing."
  :url "https://github.com/juxt/jig/tree/master/examples/sudoku"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.logic "0.8.4"]
                 [hiccup "1.0.4"]
                 [garden "1.1.4"]

                 ;; These are pre-built optional components that come
                 ;; with Jig. Some projects include many Jig components,
                 ;; others have none.
                 [jig/ring ~jig-version]
                 [jig/compojure ~jig-version]
                 [jig/jetty ~jig-version]])
