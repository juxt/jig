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

(ns jig.web.server
  (:require
   jig
   [clojure.pprint :refer (pprint)]
   [io.pedestal.service.http.route :as route]
   [io.pedestal.service.interceptor :refer (defbefore defhandler on-request defon-request definterceptorfn before)]
   [clojure.tools.logging :refer :all]
   [io.pedestal.service.http :as bootstrap]
   [io.pedestal.service.http.route.definition :as routedef])
  (:import (jig Lifecycle)))

;; TODO Replace with defbefore?
(definterceptorfn
  inject-system
  "A function that creates an interceptor that injects the System (and url-for) into
  the Pedestal context so that handlers can access it."
  [server-id system-atom app-name]
  (before
   (fn [{:keys [request] :as context}]
     (let [url-for (get-in @system-atom [server-id :url-for])]
       (debugf "Injecting System keys: %s" (apply str (interpose ", " (keys @system-atom))))
       (assoc context
         :system @system-atom
         :app (get @system-atom app-name)
         :url-for (fn [route-name & options]
                    (let [m (merge {:app-name app-name :request request} (apply hash-map options))]
                      (apply url-for route-name (apply concat (seq m))))))))))

(defn extract-app-terse-routes
  "Routes are extracted from the System, from
<app-name> :jig.web/routes, and placed under a web-context. If none is
specified, '/' is used. See 'Hierarchical route definitions' in
http://pedestal.io/documentation/service-routing/"
  [system app-name]
  (debugf "Extracting terse routes from '%s'" app-name)
  (let [terse-routes
        [(vec (apply concat (get-in system [app-name :jig.web/routes])))]]
    ;;(debugf "Terse routes are :-\n%s" (with-out-str (pprint terse-routes)))
    terse-routes))

(defn push-interceptor
  "Push an interceptor to the front of the interceptors for a given expanded route."
  [route interceptor]
  (update-in route [:interceptors]
             #(vec (cons interceptor %))))

(defn sort-routes
  "It's important that any path that is going to match the whole path is
  tried last, otherwise it will eclipse other routes. Not usually a
  concern for a single app, but it is a concern when routes are blended
  together."
  [routes]
  (let [matcher #(re-matches #".*/\*.*" %)]
    (->> routes
         (sort-by (comp count matcher :path))
         (split-with (comp not matcher :path)) ; separate out splat routes
         (map (fn [a b] (a b)) [identity reverse]) ; smallest splat route last
         (apply concat))))

(defn make-routes
  "Expand the Pedestal routes in the terse route definitions contained
in the System. The routes are created with Pedestal's expand-routes
mechanism."
  [server-id system system-atom]
  (debugf "Making routes from %d apps" (count (get-in system [server-id :app-names])))
  (let [result
        (apply
         concat              ; because we're handling multiple app-names
         (for [app-name (get-in system [server-id :app-names])]
           (let [terse-routes (extract-app-terse-routes system app-name)]
             ;; We now expand the routes. We are still in the context of
             ;; an app-name.  We add to each (expanded) route map the
             ;; common entries that constrain the route to this
             ;; application (hostname, scheme, app-name). See
             ;; jig.web.app
             (for [route (sort-routes (routedef/expand-routes terse-routes))]
               (-> route
                   (merge (get-in system [app-name :jig.web/route-common]))
                   ;; Now we push on an interceptor that injects the
                   ;; System into the Pedestal context passed into the
                   ;; handlers. Note, this isn't the System as it is
                   ;; now, but the System that contains these routes
                   ;; because we want to derive url-for . We can't know
                   ;; that now, because the routes are currently being
                   ;; constructed, so instead we reference the atom that
                   ;; will contain the System.
                   (push-interceptor (inject-system server-id system-atom app-name)))))))]
    result))

;; A Jig component that creates a web server
(deftype Component [config]
  Lifecycle

  (init [_ system]
    (debugf "Init webserver config is %s" config)
    (assoc-in system [(:jig/id config) :server-map]
              (merge config
                     {:env :dev
                      ;; do not block thread that starts web server
                      ::bootstrap/join? false})))

  (start [_ system]
    (debugf "Starting web server %s" (:jig/id config))
    (debugf "System keys are %s" (apply str (interpose ", " (keys system))))
    (let [ ;; an atom is used to break the dependency cycle: routes ->
          ;; interceptors -> url-for -> routes
          system-atom (atom nil)
          routes (make-routes (:jig/id config) system system-atom)
          url-for (route/url-for-routes routes)
          server-map (merge (get-in system [(:jig/id config) :server-map])
                            {::bootstrap/routes routes})
          ;;_ (debugf "Server map is :-\n%s" (with-out-str (pprint server-map)))
          service-map (bootstrap/create-server server-map)]

      ;;(debugf "Starting server, service-map is :-\n%s" (with-out-str (pprint service-map)))
      (bootstrap/start service-map)
      (let [system (-> system
                       (assoc-in [(:jig/id config) :service-map] service-map)
                       (assoc-in [(:jig/id config) :url-for] url-for))]
        (reset! system-atom system)
        system)))

  (stop [_ system]
    (debugf "Stopping server: %s" (get-in system [(:jig/id config) :service-map]))
    (bootstrap/stop (get-in system [(:jig/id config) :service-map]))
    system))
