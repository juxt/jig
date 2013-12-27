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

(defproject jig/console version
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   ;; Hiccup
   [hiccup "1.0.4"]
   ;; CSS for examples
   [garden "0.1.0-beta6"]
   ;; Markdown
   [endophile "0.1.0"]
   ;; Ring for responses
   ;; excluding tools.reader for now because it's out of date (clojurescript needs a newer one)
   [ring/ring-core "1.2.1" :exclusions [org.clojure/tools.reader]]
   ;; here's the newer one
   [org.clojure/tools.reader "0.8.1"]

   [jig/stencil ~version]
   [jig/bidi ~version]
   [jig/http-kit ~version]

   ;; Until Jig components can be used
   #_[stencil "0.3.2"]
   #_[bidi "1.8.0"]
   #_[org.clojure/core.match "0.2.0"] ; needed by bidi src, because we don't have the transitive dependency when we depend on src
   #_[http-kit "2.1.13"]
   ]

  #_:source-paths
  #_["src"
   "../extensions/stencil/src"
   "../extensions/bidi/src"
   "../extensions/http-kit/src"
   ]
  )
