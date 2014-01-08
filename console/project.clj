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
   ;; Markdown for converting README.md
   [endophile "0.1.0"]
   ;; CSS
   [garden "0.1.0-beta6"]

   ;; Ring for responses.  We MUST first load an up-to-date
   ;; tools.reader, ring-core 1.2.1 relies on a 0.7.x version which
   ;; isn't compatible with clojurescript. This is the error revealing
   ;; the incompatibility -
   ;;
   ;; Unable to resolve var: reader/*alias-map* in this context,
   ;; compiling:(cljs/analyzer.clj:1498:11)>
   ;;
   ;; When ring-core 1.2.2 is available, remove the explicit dependency on tools.reader
   [org.clojure/tools.reader "0.8.1"]
   [ring/ring-core "1.2.1"]

   [jig/stencil ~version]
   [jig/bidi ~version]
   [jig/http-kit ~version]

   ]

  )
