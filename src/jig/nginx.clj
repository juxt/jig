;; Rename to supervisor
(ns jig.nginx
  (:require
   jig
   [clojure.tools.logging :refer :all]
   [clojure.java.shell :as sh])
  (:import (jig Lifecycle)))

(deftype Purge [config]
  Lifecycle
  (init [_ system]
    system)
  (start [_ system]
    system)
  (stop [_ system]
    ;; We pull the latest system on stop.
    (try
      (let [{:keys [exit out err]} (sh/sh "purge" (:domain config))]
        (cond
         (pos? exit) (errorf "Purge failed with the following (error %d): %s\n%s" exit out err)
         (not (empty? err)) (errorf "Purge error: %s" err)))
      (catch java.io.IOException e))
    system))
