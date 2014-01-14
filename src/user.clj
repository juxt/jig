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

;; This is Jig's user namespace, the entry point.

;; We need to ensure that this namespace is loaded first, because we'll
;; use some of its keywords in our ns declaration.
(require 'clojure.tools.namespace.repl)

(ns ^{:doc "Tools for interactive development with the REPL. This file might not
be included in a production build of the application."
      ;; We must be careful not to reload it.
      :clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false} user
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

(defmulti read-config (fn [t s] t))

(defmethod read-config "edn" [_ s]
  (edn/read-string s))

(defmethod read-config "clj" [_ s]
  (read-string s)) ; respecting system setting of *read-eval*

(defprotocol ConfigurationSource
  (get-config-map [_]))

(defmulti merge-config #(cond (map? %1) :map (coll? %2) :coll))
(defmethod merge-config :map [a b] (merge a b))
(defmethod merge-config :coll [a b] (concat a b))

(defn follow [m]
  (if-let [includes (:jig/include m)]
    (apply merge-with merge-config (map get-config-map includes))
    m))

(extend-protocol ConfigurationSource
  java.lang.String
  (get-config-map [s] (get-config-map (io/file s)))
  java.io.File
  (get-config-map [f] (when (and f (.exists f))
                        (follow
                         (read-config
                          (second (re-matches #".*\.(.*)" (.getName f)))
                          (slurp f)))))
  java.net.URL
  (get-config-map [res] (when res
                          (follow
                           (read-config
                            (second (re-matches #".*\.(.*)" (.getFile res)))
                            (slurp res)))))
  nil
  (get-config-map [_] nil))

(defn config
  "Read the config from the config-resources. Usually this is
   config.edn (or config.clj if evaluation is desired). To to avoid
   merge issues with others' configuration, config.* files are ignored
   by git. To bootstrap, we use sample.config.edn."
  []
  {:post [(not (nil? %))]}
  (apply merge-with merge-config
         (map #(first (keep get-config-map %))
              [
               ;; Pick the Jig built-ins first so that they can overridden later
               ;; The console is added by default.
               [(io/file "console/config.clj")
                ]

               ;; Pick one from this list
               [(io/resource "config.edn")
                (io/resource "config.clj")

                (io/file (System/getProperty "user.home") ".jig/config.edn")
                (io/file (System/getProperty "user.home") ".jig/config.clj")

                ;; If none is found, we run the examples, to make a nicer
                ;; 'out-of-the-box' experience.
                ;;(io/resource "examples.clj")
                ]

               ;; Etc.
               ])))

(declare reset)

(defn init
  "Creates and initializes the system in the Var #'system."
  []
  (alter-var-root #'system #(system/init % (config))))

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
  (try
    (init)
    (start)
    (catch Throwable e
      (println e)
      (.printStackTrace e)
      )
    )
  :ready)

;; TODO See https://github.com/juxt/jig/issues/3
(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn graph "View the dependency graph"
  []
  (let [{components :jig/components} (if system (:jig/config system) (config))]
    (view (system/get-digraph components))))

(defn menu []
  (doseq [line
          ["(go)       -- start the system"
           "(reset)    -- reset the system"
           "(refresh)  -- recover if a reset fails due to a compilation error"
           "(menu)     -- show this menu again"]]
    (println line)))

(defn welcome []
  (println "Welcome to Jig!")
  (println)
  (menu)
  )
