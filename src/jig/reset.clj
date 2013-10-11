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

(ns ^{:doc "Some utility functions to assist a live reset"}
  jig.reset
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.tools.nrepl :as repl])
  )

(defn reset-via-nrepl [system delay-in-ms]
  (let [port (get-in system [:jig.nrepl/server :port])]
    (infof "Reset by calling reset on nREPL client port %s" port)
    (with-open [conn (repl/connect :port port)]
      (-> (repl/client conn 1000)   ; message receive timeout required
          (repl/message {:op "eval" :code "(reset)"}))))
  ;; We get this, because obviously the reset is pretty catastrophic
;; Exception in thread "nREPL-worker-0" java.lang.Error:
;; java.net.SocketException: Socket closed at
;; java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1116)
;; at
;; java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:603)
;; at java.lang.Thread.run(Thread.java:722) Caused by:
;; java.net.SocketException: Socket closed at
;; java.net.SocketOutputStream.socketWrite(SocketOutputStream.java:116)
;; at java.net.SocketOutputStream.write(SocketOutputStream.java:153)

  ;; TODO we should try doing this in a future with a sleep in it

)
