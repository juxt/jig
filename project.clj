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

(defproject jig (get-version)

  :description "A jig for developing systems using component composition. Based on Stuart Sierra's 'reloaded' workflow."

  :url "https://juxt.pro/jig"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;; lein-sub is used for deploying the sub-projects

  :plugins [[lein-sub "0.2.3"]]

  ;; Jig comes with a library of optional pre-built ready-to-go Jig
  ;; components that are available for projects to make use of.

  :sub [
        "extensions/async" ; core.async channels that can be shared by dependants
        "extensions/bidi" ; URI routing library
        "extensions/cljs-builder" ; ClojureScript compilation
        "extensions/compojure" ; URI routing library
        "extensions/http-kit" ; HTTP server (with client library)
        "extensions/jetty" ; HTTP server
        "extensions/netty" ; Generic network server library
        "extensions/netty-mqtt" ; MQTT support for Netty
        "extensions/ring" ; Ring utilities
        "extensions/stencil" ; Templating library
        ]

  :exclusions
  [
   ;; tools.reader comes in through Ring, but older versions are incompatible with
   ;; newer releases of ClojureScript.
   org.clojure/tools.reader
   ]

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   ;; tools.namespace is what provides the foundation for the reset functionality
   [org.clojure/tools.namespace "0.2.4"]
   ;; leiningen-core helps us find the classpath for projects referenced in the configuration files
   [leiningen-core "2.3.2" :exclusions [org.clojure/tools.nrepl]]
   ;; tools.logging provides our logging API...
   [org.clojure/tools.logging "0.2.6"]
   ;; ... which uses logback as the logging back-end
   [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
   [org.slf4j/jul-to-slf4j "1.7.2"]
   [org.slf4j/jcl-over-slf4j "1.7.2"]
   [org.slf4j/log4j-over-slf4j "1.7.2"]

   ;; Graph algorithms for dependency graphs
   [jkkramer/loom "0.2.0"]]

  ;; Dev logging configuration and default config that loads the examples
  :profiles {:dev {:resource-paths ["config"]}}

  :repl-options {:prompt (fn [ns] (str "Jig " ns "> "))
                 :welcome (user/welcome)}

  :aliases {"deploy-all" ["do" "deploy" "clojars," "sub" "deploy" "clojars"]
            "install-all" ["do" "install," "sub" "install"]}
  )
