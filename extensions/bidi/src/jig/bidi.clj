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

(ns jig.bidi
  (:require
   [bidi.bidi :as bidi]
   [jig.util :refer (satisfying-dependency)]
   [clojure.java.io :as io]
   [ring.util.response :refer (file-response)]
   [ring.middleware.content-type :refer (wrap-content-type)]
   [ring.middleware.file-info :refer (wrap-file-info)]
   jig)
  (:import (jig Lifecycle)))

(defn add-bidi-routes
  "Add a bidi route structure to the component's entry in the system
  map, under a special ::routes key. These can then be found Router
  component, and can form part of a composite route
  structure. If :jig.web/context is defined in the config, this is used
  as a mount point for these routes in the composite route structure."
  [system config routes]
  (update-in system [(:jig/id config) ::routes]
             conj
             (if-let [webctx (:jig.web/context config)]
               [webctx [routes]]
               routes)))

(defn wrap-system
  "Add the system map to the request so it's available to Ring handlers."
  [h system]
  (fn [req]
    (h (assoc req :jig/system system))))

(defn wrap-routes
  "Add the final set of routes from which the Ring handler is built."
  [h routes]
  (fn [req]
    (h (assoc req ::routes routes))))

(defn wrap-web-context
  "Add the web context that this router is mounted to."
  [h webctx]
  (fn [req]
    (h (assoc req :jig.web/context webctx))))

(defn wrap-jig-component
  "Associate the contributing component as a :jig/component entry in the request"
  [routes component]
  ["" (bidi/->WrapMiddleware routes (fn [h] (fn [req] (h (assoc req :jig/component component)))))])

(deftype Router [config]
  Lifecycle
  (init [_ system]
    system)
  (start [_ system]
    (let [routes [(or (:jig.web/context config) "")
                  (vec
                   (remove nil? ;; Remove contributions from components that don't contribute routes.
                           (for [dep (:jig/dependencies config)
                                 :let [component (-> system :jig/config :jig/components dep (assoc :jig/id dep))]]
                             ;; Iff the component contributes routes, include them.
                             (when-let [routes (-> system dep ::routes)]
                               (wrap-jig-component routes component)))))]

          handler (-> routes
                      bidi/make-handler
                      (wrap-system system)
                      (wrap-routes routes)
                      (wrap-web-context (or (:jig.web/context config) "")))]
      (-> system
          ;; The primary purpose of this component is to set this entry
          (assoc-in [(:jig/id config) :jig.ring/handler] handler)
          ;; The final set of routes is useful for debugging in the REPL
          (assoc-in [(:jig/id config) ::routes] routes)
          )))
  (stop [_ system] system))

(defrecord Files [options]
  bidi.bidi.Matched
  (resolve-handler [this m]
    (assoc (dissoc m :remainder)
      :handler (-> (fn [req] (file-response (:remainder m) {:root (:dir options)}))
                   (wrap-file-info (:mime-types options))
                   (wrap-content-type options))))
  (unresolve-handler [this m] nil))

(deftype ClojureScriptRouter [config]
  Lifecycle
  (init [_ system]
    (let [builder (satisfying-dependency system config 'jig.cljs-builder/Builder)]
      (add-bidi-routes system config [""
                                      (->Files {:dir (:output-dir builder)
                                                :mime-types {"map" "application/javascript"}})])))

  (start [_ system] system)
  (stop [_ system] system))
