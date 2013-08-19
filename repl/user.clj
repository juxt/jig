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

(ns user
  "Tools for interactive development with the REPL. This file might not
be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :as test]
   [loom.io :refer (view)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [jig.system :as system]))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn config
  "Read the config from the config file. Usually this is config.edn (or
  config.clj if evaluation is desired). To to avoid merge issues with
  other's configuration, config.* files are ignored by git. To bootstrap,
  we use sample.config.edn."
  []
  (when-let [res (or
                (io/resource "config.edn")
                (io/resource "config.clj")
                ;; if no jig.edn, we take from sample.jig.edn
                (io/resource "sample.config.edn"))]
    (->
     (case (second (re-matches #".*\.(.*)" (.getFile res)))
       "edn" (edn/read-string (slurp res))
       "clj" (read-string (slurp res)) ; respecting system setting of *read-eval*
       (throw (Exception. (format "No reader for %s" res))))
     (assoc :resource res))))

(defn init
  "Creates and initializes the system in the Var #'system."
  []
  (alter-var-root #'system (constantly (system/init (config)))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system system/start))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root
   #'system
   (fn [s] (when s (system/stop s)))))

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn graph "View the dependency graph"
  []
  (let [{:keys [components]} (if system (:jig/config system) (config))]
    (view (system/get-digraph components))))
