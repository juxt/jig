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

(ns jig.web.firefox-reload
  (:require
   [clojure.tools.logging :refer :all]
   jig)
  (:import
   (jig Lifecycle)
   (java.net Socket)
   (java.io IOException)))

(deftype Component [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (try
      (infof "Reloading firefox")
      (with-open [sock (Socket. (::host config) (::port config))
                  out (.getOutputStream sock)]
        (.write out (.getBytes "reload"))
        (.flush out))
      (catch IOException e
        (errorf "Failed to reload firefox")))
    system)
  (stop [_ system] system))
