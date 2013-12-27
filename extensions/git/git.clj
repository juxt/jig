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
    (let [{:keys [exit out err]}
          (if-let [dir (:directory config)]
            (sh/with-sh-dir dir
              (sh/sh "git" "pull"))
            (sh/sh "git" "pull"))]
      (cond
       (pos? exit) (errorf "Git failed with the following (error %d): %s\n%s" exit out err)
       (not (empty? err)) (errorf "Git error: %s" err)))
    system))
