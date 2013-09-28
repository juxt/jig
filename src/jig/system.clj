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
   [leiningen.core.project :as project]
   [classlojure.core :as cl]
   [clojure.tools.namespace.reload :as reload] ; hell yeah!
   [clojure.tools.namespace.dir :refer (scan)]
   [clojure.tools.namespace.track :as track]
   [leiningen.core.classpath :as classpath]
   [loom.graph :refer (graph digraph)]
   [loom.io :refer (view)]
   [loom.alg :refer (post-traverse dag? topsort)]))

;; This is like classlojure's with-classloader in that it sets the
;; context classloader. Unlike classlojure, it will only do so if the
;; given classloader isn't nil.
(defmacro with-context-classloader [cl & body]
  `(if ~cl
     (binding [*use-context-classloader* true]
       (let [cl# (.getContextClassLoader (Thread/currentThread))]
         (try (.setContextClassLoader (Thread/currentThread) ~cl)
              ~@body
              (finally
                (.setContextClassLoader (Thread/currentThread) cl#)))))
     (do
       ~@body)))

;; Clojure will first check the binding in clojure.lang.Compiler/LOADER,
;; and only if that is empty will it try the context classloader. To
;; ensure that classes are loaded from a particular classloader, we must
;; bind it to clojure.lang.Compiler/LOADER. The function allows for a
;; nil classloader.
(defmacro with-classloader [cl & body]
  `((fn []
      (when ~cl
        (. clojure.lang.Var (pushThreadBindings {clojure.lang.Compiler/LOADER ~cl})))
      (try
        ~@body
          (finally
            (when ~cl
              (. clojure.lang.Var (popThreadBindings))))))))

(defmacro with-classloaders [cl & body]
  `(with-classloader ~cl
    (with-context-classloader ~cl
      ~@body)))

(defn instantiate [{id :jig/id component :jig/component project :jig/project :as config}]
  (when (nil? component)
    (throw (ex-info (format "Component is nil in config: %s" config) config)))
  (debugf "Resolving namespace: %s" (symbol (namespace component)))

  (when (nil? (symbol (namespace component)))
    (throw (ex-info (format "Namespace not found: %s" (symbol (namespace component)))
                    {})))
  (with-classloader (:classloader project)
    (require (symbol (namespace component)))
    (debugf "Instantiating component: %s" id)
    (let [typ (ns-resolve (symbol (namespace component)) (symbol (name component)))]
      (when (nil? typ) (throw (Exception. (format "Cannot find component: %s" component))))
      (let [ctr (.getConstructor typ (into-array Class [Object]))]
        (when (nil? ctr)
          (throw (Exception. (format "Component must have a no-arg constructor: %s" component))))
        (.newInstance ctr (into-array [config]))))))

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

(defn validate-system [system component phase]
  (if (nil? system)
    (throw (ex-info (format "Bad component returned nil for system on %s: %s" phase component) component)))
  system)

(defn project-struct
  "Create a project map containing interesting details about a project,
helpful in avoiding repeated expensive analysis of project files"
  [f components {pinned? :jig/pinned? :as conf}]
  (infof "Creating project struct for %s with config %s" f conf)
  (let [p (->> f str project/read)
        cp (->> p classpath/get-classpath (map io/as-file))]
    {:name (:name p)
     :components components
     :project-file f
     :last-modified (.lastModified f)
     :project p
     :classpath cp
     :pinned? pinned?
     :classloader (->> cp
                       (map io/as-url)
                       into-array (java.net.URLClassLoader.))
     :dirs (->> cp (filter (memfn isDirectory)))
     :tracker (track/tracker)}))

(defn announce-reload
  "Annouce on the REPL (or, rather, *out*) that a reload is happening"
  [{:keys [name tracker] :as project}]
  (println (format ":reloading %s (%s)" name (apply str (interpose " " (:clojure.tools.namespace.track/load tracker)))))
  project)

(defn reload-tracker-in-classloader [tracker ldr]
  (with-classloader ldr
    (reload/track-reload tracker)))

(defn refresh-project [project]
  (if (not (:pinned? project))
    (project-struct (:project-file project) (:components project) false)
    (do
      (warnf "Project (%s) should be refreshed but is configured as pinned, so will not refresh it."
             (:project-file project))
      project)))

(defn ensure-fresh-project
  "If the project.clj file of a project has been modified, reload the project info"
  [project]
  (if (> (.lastModified (:project-file project)) (:last-modified project))
    (do
      (debugf "Project (%s) changed, refreshing project %s"
             (:project-file project)
             (:name project))
      (refresh-project project))
    project))

(defn reload-project
  "Reload a project such that recently modified libs are reloaded along with any dependants"
  [project]
  (-> project
      (update-in [:tracker] #(apply scan % (:dirs project)))
      announce-reload
      (update-in [:tracker] reload-tracker-in-classloader (:classloader project))))

(defn init
  "Reset the projects, (re-)initialize the system components"
  [{projects :jig/projects :as system}
   {project-confs :jig/projects components :jig/components :as config}]

  (let [extract-prj (comp :jig/project second)
        extract-cfile (comp (memfn getCanonicalFile)
                            io/file extract-prj)
        projects
        (cond
         (nil? projects)
         (->> components
              (filter extract-prj)
              (group-by extract-cfile)
              (map (fn [[file entries]]
                     (project-struct file (map first entries)
                                     (first (filter #(= file (some->> % :jig/project io/file (.getCanonicalFile)))
                         project-confs))))))

         (not= config (:jig/config system))
         (doall (map (comp reload-project refresh-project) projects))

         :otherwise
         (doall (map (comp reload-project ensure-fresh-project) projects)))

        compid->project
        (->> projects
             (map (juxt :components repeat))
             (mapcat (partial apply map vector))
             (into {}))]

    (debugf "Projects are: %s" projects)

    (let [component-instances (for [id (get-dependency-order components)]
                                (if-let [c (get components id)]
                                  (let [project (compid->project id)
                                        c++ (assoc c
                                              :jig/id id
                                              :jig/project project)]
                                    (assoc c++ :jig/instance (instantiate c++)))
                                  (throw
                                   (ex-info
                                    (format "Component '%s' referenced as a dependency but is not contained in the map" id)
                                    {:id id}))))]

      (debugf "Components order is %s" (apply str (interpose ", " (map :jig/id component-instances))))

      ;; Projects must have a structure, like 'last seen time', etc..

      (let [seed {:jig/components []
                  :jig/config config
                  :jig/projects projects}
            system
            (reduce (fn [system component]
                      (with-classloaders (some->> component :jig/project :classloader)
                        (try
                          (-> (.init (:jig/instance component) system)
                              (validate-system component "init")
                              (update-in [:jig/components] conj component))
                          (catch Exception e
                            (errorf e "Failed to initialize component: %s" (:jig/id component))
                            ;; Tell the repl
                            (println "Component failed to initialize (check the logs):" (:jig/id component))
                            (update-in system [:jig/components-failed-init] conj component)
                            ))))
                    seed component-instances)]
        (debugf
         "After system initialization, system keys are %s"
         (apply str (interpose ", " (keys system))))
        system))))

(defn start
  "Start the system"
  [{components :jig/components :as system}]
  (infof "Starting the system")
  (let [system
        (reduce
         (fn [system component]
           (with-classloaders (some->> component :jig/project :classloader)
             (try
               (infof "Starting component '%s'" (:jig/id component))
               (-> (.start (:jig/instance component) system)
                   (validate-system component "start")
                   (update-in [:jig/components] conj component))
               (catch Exception e
                 (errorf e "Failed to start component: %s" (:jig/id component))
                 ;; Tell the repl
                 (println "Component failed to start (check the logs):" (:jig/id component))
                 (update-in system [:jig/components-failed-start] conj component)
                 ))))
         (assoc system :jig/components []) components)]
    (debugf "After system start, system keys are %s" (apply str (interpose ", " (keys system))))
    system))

(defn stop
  "Stop the system"
  [{components :jig/components :as system}]
  (infof "Stopping the system")
  (->> components
       reverse ;; Components are stopped in reverse order
       (reduce
        (fn [system component]
          (with-classloaders (some->> component :jig/project :classloader)
            (try
              (infof "Stopping component '%s'" (:jig/id component))
              (.stop (:jig/instance component) system)
              (catch Exception e
                (errorf e "Failed to stop component (check the logs): %s"
                        (:jig/id component))
                ;; Tell the repl
                (println "Component failed to stop (check the logs):" (:jig/id component))
                ;; Return system, the :jig/components-failed-stop may be
                ;; used by other components (unlikely)
                (update-in system [:jig/components-failed-stop] conj component))
              )))
        ;; Seed the reduce with the system
        system)))
