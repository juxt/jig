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

(ns jig.pedestal.service.cljs
  (:require
   jig
   [jig.pedestal.service.app :refer (add-routes)]
   [ring.util.response :as ring-resp]
   [ring.util.codec :as codec]
   [io.pedestal.service.interceptor :as interceptor :refer (definterceptorfn)])
  (:import (jig Lifecycle)))

(definterceptorfn static
  [root-path & [opts]]
  (interceptor/handler
   ::cljs-static
   (fn [req]
     (ring-resp/file-response
      (codec/url-decode (get-in req [:path-params :path]))
      {:root root-path, :index-files? true, :allow-symlinks? false}))))

(deftype FileServer [config]
  Lifecycle
  (init [_ system]
    (let [deps (select-keys (-> system :jig/config :jig/components) (:jig/dependencies config))
          ;; This should be a common pattern, find a dependency that matches
          app (first (filter (fn [[k v]] (= 'jig.pedestal.service.app/Component (:jig/component v))) deps))
          builder (first (filter (fn [[k v]] (= 'jig.cljs/Builder (:jig/component v))) deps))]
      (add-routes
       system
       (assoc config :jig.web/app-name (first app))
       [["/*path" {:get (static (:output-dir (second builder)))}]])))

  (start [_ system]
    system)
  (stop [_ system]
    system))
