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

(ns jig.ring
  (:require
   jig
   [clojure.tools.logging :refer :all])
  (:import (jig Lifecycle)))

(defn wrap-config
  "Add the handler config to the request so it's available to Ring handlers."
  [h config]
  (fn [req]
    (h (assoc req :jig/config config))))

(defn add-ring-handler
  "Register a Ring handler against the given component. The benefit of
  using this function is that the config is added to the incoming
  request (via Ring middleware)"
  [system config handler]
  (update-in system [(:jig/id config) :jig.ring/handlers]
             conj (wrap-config handler config)))
