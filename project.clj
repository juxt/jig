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

  :sub ["extensions/bidi"
        "extensions/cljs-builder"
        "extensions/http-kit"
        "extensions/stencil"]

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   ;; Leiningen
   [leiningen-core "2.3.2" :exclusions [org.clojure/tools.nrepl]]
   ;; Logging
   [org.clojure/tools.logging "0.2.6"]
   ;; Graph algorithms for dependency graphs
   [jkkramer/loom "0.2.0"]
   ;; Tools namespace
   [org.clojure/tools.namespace "0.2.4"]
   ]

  :profiles {:dev {:resource-paths ["config"]}}

  :repl-options {:prompt (fn [ns] (str "Jig " ns "> "))
                 :welcome (user/welcome)}
  )
