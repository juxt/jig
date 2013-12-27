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
   [compojure.core :refer (routes)]
   [compojure.route :refer (files)]
   [jig.ring :refer (add-ring-handler)]
   [clojure.tools.logging :refer :all])
  (:import (jig Lifecycle)))

#_(deftype Compojure [config]
  Lifecycle
  (init [_ system]
    (if-let [handlers (::handlers system)]
      (assoc system ::handler (apply routes handlers))
      (throw (ex-info "Compojure won't initialise without handlers available. The Compojure component must depend on other components that supply handlers by conj'd (or concat'd) to ::handlers" config))))
  (start [_ system] system)
  (stop [_ system] system))

;; This Jig component iterates across its dependencies. For every
;; dependency D, it checks for [D :jig.ring/handlers], and forms a
;; handler, which is places in its :jig.ring/handler entry. This can be
;; picked up by any dependant Ring-compatible servers, such as Jetty,
;; http-kit or application servers.

(defn log-routes [routes]
  (infof "Number of routes composed in handler is %d" (count routes))
  routes)

(deftype HandlerComposer [config]
  Lifecycle
  (init [_ system]
    (->> system
         ((apply juxt (:jig/dependencies config))) ; all dependencies
         (mapcat :jig.ring/handlers)  ; get the compojure routes of each
         log-routes
         (apply routes)               ; combine into a single handler
         ;; and associate under the component's :jig.ring/handler key
         (assoc-in system [(:jig/id config) :jig.ring/handler])))
  (start [_ system] system)
  (stop [_ system] system))

;; This Jig component serves ClojureScript

(deftype ClojureScriptRouter [config]
  Lifecycle
  (init [_ system]
    (let [builders (->> system :jig/config :jig/components
                        ;; get dependencies
                        ((apply juxt (:jig/dependencies config)))
                        ;; that are cljs builders
                        (filter (comp (partial = 'jig.cljs/Builder) :jig/component)))]

      ;; Assert at least one builder
      (when (empty? builders)
        (throw (ex-info (format "%s must depend on a component of type %s"
                                (:jig/id config) 'jig.cljs/Builder) {})))

      ;; Assert not multiple builders
      (when (> (count builders) 1)
        (throw (ex-info (format "%s cannot depend on (or serve) more than one builder"
                                (:jig/id config)) {})))

      (add-ring-handler system
                        (files "/js" {:root (:output-dir (first builders))
                                      :mime-types {"cljs" "text/plain"
                                                   "map" "application/javascript"}})
                        config)))

  (start [_ system] system)
  (stop [_ system] system))
