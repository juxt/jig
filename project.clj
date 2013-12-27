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

  :plugins [[lein-sub "0.2.3"]]

  :sub ["extensions/bidi"
        "extensions/cljs-builder"
        "extensions/http-kit"
        "extensions/stencil"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; core.async
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]
                 ;; Leiningen
                 [leiningen-core "2.3.2" :exclusions [org.clojure/tools.nrepl]]
                 ;; Tracing
                 [org.clojure/tools.trace "0.7.5"]
                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.2"]
                 [org.slf4j/jcl-over-slf4j "1.7.2"]
                 [org.slf4j/log4j-over-slf4j "1.7.2"]
                 ;; Graph algorithms for dependency graphs
                 [jkkramer/loom "0.2.0"]
                 ;; JMX
;;                 [org.clojure/java.jmx "0.2.0"]
                 ;; nREPL
                 [org.clojure/tools.nrepl "0.2.3"]
                 ;; Tools namespace
                 [org.clojure/tools.namespace "0.2.4"]
                 ;; Back, by popular demand, Ring!
                 ;;[ring "1.2.0"]
                 ;;[compojure "1.1.5"]
                 ;; MQTT for messaging
                 [clojurewerkz/machine_head "1.0.0-beta4"]

                 [org.clojure/java.classpath "0.2.0"]
                 ]

  :profiles {:dev {:resource-paths ["config"]}}

  :repl-options {:prompt (fn [ns] (str "Jig " ns "> "))
                 :welcome (user/welcome)}
  )
