;; Rename to supervisor
(ns jig.git
  (:require
   jig
   [clojure.tools.logging :refer :all]
   [clojure.java.shell :as sh])
  (:import (jig Lifecycle)))

(deftype GitPull [config]
  Lifecycle
  (init [_ system]
    system)
  (start [_ system]
    system)
  (stop [_ system]
    ;; We pull the latest system on stop.
    (infof "Pulling latest git version")
    (let [{:keys [exit out err]} (sh/sh "git" "pull")]
      (cond
       (pos? exit) (errorf "Git failed with the following (error %d): %s\n%s" exit out err)
       (not (empty? err)) (errorf "Git error: %s" err)))
    system))
