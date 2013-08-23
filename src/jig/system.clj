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

(ns jig.system
  (:require
   [clojure
    [edn :as edn]]
   [clojure.java
    [io :as io]]
   [clojure.tools
    [logging :refer :all]]
   [clojure.pprint :refer (pprint)]
   [jig :as jig]
   [loom.graph :refer (graph digraph)]
   [loom.io :refer (view)]
   [loom.alg :refer (post-traverse dag? topsort)]))

(defn instantiate [{component :jig/component :as config}]
  (when (nil? component)
    (throw (ex-info (format "Component is nil in config: %s" config) config)))
  (infof "Resolving namespace: %s" (symbol (namespace component)))

;; TODO
  (when (nil? (symbol (namespace component)))
    (throw (ex-info (format "Namespace not found: %s" (symbol (namespace component)))
                    {})))

  (require (symbol (namespace component)))
  (infof "Instantiating component: %s" component)
  (let [typ (ns-resolve (symbol (namespace component)) (symbol (name component)))]
    (when (nil? typ) (throw (Exception. (format "Cannot find component: %s" component))))
    (let [ctr (.getConstructor typ (into-array Class [Object]))]
      (when (nil? ctr)
        (throw (Exception. (format "Component must have a no-arg constructor: %s" component))))
      (.newInstance ctr (into-array [config])))))

(defn get-digraph [components]
  (->> components
       (map (fn [[k v]] (if-let [deps (:jig/dependencies v)] [k deps] [k []])))
       (into {}) digraph))

(defn get-dependency-order [components]
  (let [g (get-digraph components)]
    (if (dag? g)
      (reverse (topsort g))
      (throw (ex-info "Components must form a directed acyclic graph"
                      {:components components})))))

(defn init-components
  "Instantiate components and return a map for each, in dependency order, dependants last"
  [{:keys [components] :as config}]
  (for [id (get-dependency-order components)]
    (if-let [c (get components id)]
      (let [c++ (-> c
                    (assoc :jig/id id)
                    (assoc :jig/config config))]
        (assoc c++ :jig/instance (instantiate c++)))
      (throw
       (ex-info
        (format "Component '%s' referenced as a dependency but is not contained in the map" id)
        {:id id})))))

(defn validate-system [system component phase]
  (if (nil? system)
    (throw (ex-info (format "Bad component returned nil for system on %s: %s" phase component) component)))
  system)

(defn init
  "Initialize the system components"
  [{:keys [components] :as config}]
  (infof "Initializing system with config :-\n%s" (with-out-str (pprint config)))
  (let [component-instances (init-components config)]
    (infof "Components order is %s" (apply str (interpose ", " (map :jig/id component-instances))))
    (infof "Components to init are :-\n%s" (with-out-str (pprint component-instances)))
    (let [system
          (reduce (fn [system component]
                    (try
                      (-> (jig/init (:jig/instance component) system)
                          (validate-system component "init")
                          (update-in [:jig/components] conj component))
                      (catch Exception e
                        (errorf e "Failed to initialize component: %s" component)
                        ;; Tell the repl
                        (println "Component failed to initialize (check the logs):" component)
                        (update-in system [:jig/components-failed-init] conj component)
                        )))
                  {:jig/components []
                   :jig/config config} component-instances)]
      (debugf "After system initialization, system keys are %s" (apply str (interpose ", " (keys system))))
      system)))

(defn start
  "Start the system components"
  [{components :jig/components :as system}]
  (let [system
        (reduce (fn [system component]
                  (try
                    (infof "Starting component '%s' :-\n%s"
                           (:jig/id component) (with-out-str (pprint component)))
                    (-> (jig/start (:jig/instance component) system)
                        (validate-system component "start")
                        (update-in [:jig/components] conj component))
                    (catch Exception e
                      (errorf e "Failed to start component: %s" component)
                      ;; Tell the repl
                      (println "Component failed to start (check the logs):" component)
                      (update-in system [:jig/components-failed-start] conj component)
                      )))
                (assoc system :jig/components []) components)]
    (debugf "After system start, system keys are %s" (apply str (interpose ", " (keys system))))
    system
    ))

(defn stop [{components :jig/components :as system}]
  (info "Stop the system components")
  (->> components
       reverse ;; Components are stopped in reverse order
       (reduce (fn [system component]
                 (try
                   (infof "Stopping component '%s' :-\n%s"
                          (:jig/id component) (with-out-str (pprint component)))
                   (jig/stop (:jig/instance component) system)
                   (catch Exception e
                     (errorf e "Failed to stop component (check the logs): %s"
                             component)
                     ;; Tell the repl
                     (println "Component failed to stop:" component)
                     ;; Return system, the :jig/components-failed-stop may be
                     ;; used by other components (unlikely)
                     (update-in system [:jig/components-failed-stop] conj component))
                   ))
               ;; Seed the reduce with the system
               system))
  ;; Return nil which will become the new system value.
  nil)
