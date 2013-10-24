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

(ns sudoku.jig
  (:require
   jig
   [sudoku.core :refer (handler)]
   [jig.web.ring :refer (add-handler)])
  (:import (jig Lifecycle)))

(deftype Website [config]
  Lifecycle
  (init [_ system]
    (add-handler handler system config))
  (start [_ system] system)
  (stop [_ system] system))
