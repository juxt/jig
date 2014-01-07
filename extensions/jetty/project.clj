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

(load-file (str (System/getProperty "leiningen.original.pwd") "/../../project-header.clj"))

(defproject jig/jetty (get-version)
  :description "A Jig extension that provides support for Jetty"
  :url "https://github.com/juxt/jig/tree/master/extensions/jetty"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [ring/ring-jetty-adapter "1.2.1" ]
                 ;;[org.eclipse.jetty/jetty-server "7.6.8.v20121106"]
                 ;; :exclusions [javax.servlet/servlet-api]
                 ])