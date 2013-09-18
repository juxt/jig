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

(ns jig.web.stencil
  (:require
   jig
   [clojure.core.cache :refer (lru-cache-factory)]
   [stencil.loader :refer (set-cache)])
  (:import (jig Lifecycle)))

;; Stencil is a little painful in development because it caches
;; templates for performance (which is great in production). But we can
;; hook a cache expiry into our reload mechanism, yay!
(deftype StencilCache [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    ;; Clear stencil cache!
    (let [cache (lru-cache-factory {})]
      (set-cache cache))
    system)
  (stop [_ system] system))
