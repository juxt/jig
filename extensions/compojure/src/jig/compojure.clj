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

(ns jig.compojure
  (:require
   jig
   [jig.util :refer (satisfying-dependency)]
   [compojure.core :refer (routes)]
   [compojure.route :refer (files)]
   [jig.ring :refer (add-ring-handler)]
   [clojure.tools.logging :refer :all])
  (:import (jig Lifecycle)))

;; This Jig component iterates across its dependencies. For every
;; dependency D, it checks for [D :jig.ring/handlers], and forms a
;; handler, which is places in its :jig.ring/handler entry. This can be
;; picked up by any dependant Ring-compatible servers, such as Jetty,
;; http-kit or application servers.

(deftype HandlerComposer [config]
  Lifecycle
  (init [_ system]
    (->> system
         ((apply juxt (:jig/dependencies config))) ; all dependencies
         (mapcat :jig.ring/handlers)  ; get the compojure routes of each
         (apply routes)               ; combine into a single handler
         ;; and associate under the component's :jig.ring/handler key
         (assoc-in system [(:jig/id config) :jig.ring/handler])))
  (start [_ system] system)
  (stop [_ system] system))

;; This Jig component serves ClojureScript

(deftype ClojureScriptRouter [config]
  Lifecycle
  (init [_ system]
    (let [builder (satisfying-dependency system config 'jig.cljs-builder/Builder)]
      (add-ring-handler system
                        (files "/js" {:root (:output-dir builder)
                                      :mime-types {"cljs" "text/plain"
                                                   "map" "application/javascript"}})
                        config)))

  (start [_ system] system)
  (stop [_ system] system))
