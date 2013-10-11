;; Here's the code I used to determine that the context class loader can
;; lead across task invocations and should therefore be nilled if the
;; thread is interrupted (code is in org.clojure/tools.nrepl).

;; It's therefore questionable whether nrepl should be make use of a
;; ThreadPoolExecutor, and perhaps should start each nrepl task with a
;; clean thread.

(ns jig.nrepl.play
  (:require [clojure.java.io :refer (as-url file)])
  (:import
   java.util.concurrent.atomic.AtomicLong
   (java.util.concurrent ThreadPoolExecutor
                         TimeUnit BlockingQueue
                         ThreadFactory
                         SynchronousQueue)))

(defn- configure-thread-factory
  "Returns a new ThreadFactory for the given session.  This implementation
   generates daemon threads, with names that include the session id."
  []
  (let [session-thread-counter (AtomicLong. 0)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (doto (Thread. runnable
                (format "task-%s" (.getAndIncrement session-thread-counter)))
          (.setDaemon true))))))

(def t (atom nil))

(defn taskA [out message]
  (fn []
    (binding [*out* out]
      (reset! t (Thread/currentThread))
      (println)
      (println (format "origin classloader is %s" (.getContextClassLoader (Thread/currentThread))))
      (.setContextClassLoader (Thread/currentThread) (java.net.URLClassLoader. (into-array (map as-url [(file "/home/malcolm/tmp/cl-testing/a.jar")]))))
      (println (format "%s, my thread is %s" message (.getName (Thread/currentThread))))
      (println (format "current classloader is %s" (.getContextClassLoader (Thread/currentThread))))
      (Thread/sleep 2000)
      (println "taskA done")
)))

(defn taskB [out message]
  (fn []
    (binding [*out* out]
      (println)
      (println (format "%s, my thread is %s" message (.getName (Thread/currentThread))))
      (println (format "current classloader is %s" (.getContextClassLoader (Thread/currentThread)))))))

(let [executor
      (ThreadPoolExecutor. 1 1 (long 30000) TimeUnit/MILLISECONDS
                           ^BlockingQueue (SynchronousQueue.)
                           (configure-thread-factory))]
  (.execute executor (taskA *out* "Hello"))
  (Thread/sleep 1000)
  (println "t is " @t)
  (.stop @t)
  (println "stopped thread")

  ;; Kill it!!
  (Thread/sleep 2000)
  (.execute executor (taskB *out* "Goodbye"))
  )
