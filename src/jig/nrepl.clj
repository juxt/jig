(ns jig.nrepl
  (:require
   jig
   [clojure.tools.logging :refer :all]
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
