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

(ns jig.nrepl
  (:require
   jig
   [clojure.tools.logging :refer :all]
   [clojure.tools.nrepl.middleware :refer (set-descriptor!)]
   [clojure.tools.nrepl.server :refer (start-server stop-server)])
  (:import (jig Lifecycle)))

(deftype Server [conf]
  Lifecycle
  ;; TODO This obviously should be in the start phase, but the pedestal
  ;; middleware that adds the system to the context will not see this
  ;; info at the time it is constructed. One of the following needs to
  ;; happen: 1. The pedestal interceptor is added to the routes only
  ;; upon start.  2. We consider making system an atom, and passing the
  ;; atom around, so that components can communicate with those in the
  ;; 'past'. This is already happening 'internally' for the mutually
  ;; dependent routes and url-for to be added.
  (init [_ system]
    ;; Note the nREPL port is in the system map, under [::server :port]
    ;; We assume the nREPL server is a singleton, it makes it easier for clients to locate.
    (let [server (start-server :port (:port conf 0))]
      (infof "Starting nREPL server on port %s" (:port server))
      (assoc system ::server server)
      ))
  (start [_ system] system)
  (stop [_ system]
    (stop-server (::server system))
    system))

;; nREPL middleware

;; This middleware will only work against a version of
;; interruptible-eval in org.clojure/tools.nrepl that accepts and uses
;; the :context-classloader value in the message. This includes
;; [malcolmsparks/tools.nrepl "0.2.3"]. Note that Leiningen builds in
;; tools.nrepl, so may have to be custom built to depend on a
;; tools.nrepl for this feature. See my pull request to tools.nrepl. (If
;; this comment is out of date, please remove it)

(defn wrap-jig-loader
  "Injects the proxy classloader, proxies over all the project
  classloaders, into the nREPL message. This is necessary so that
  functions such as nrepl-jump will work, even when run against symbols
  which are defined by code outside of the system classpath."
  [h]
  (fn [{:keys [op transport] :as msg}]
    (h (assoc msg :context-classloader (:jig/proxy-classloader user/system)))))

(set-descriptor!
 #'wrap-jig-loader
 {:requires #{}
  :expects #{"eval"}
  :handles {}})
