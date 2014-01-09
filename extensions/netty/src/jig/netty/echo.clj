;; Copyright © 2013 - 2014, JUXT LTD. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns jig.netty.echo
  (:require
   jig)
  (:import
   (jig Lifecycle)
   (io.netty.channel ChannelHandlerAdapter)))

(deftype EchoHandler [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]
    (assoc-in system [(:jig/id config) ::handler-factory]
              #(proxy [ChannelHandlerAdapter] []
                 (channelRead [ctx msg]
                   (.write ctx msg)
                   (.flush ctx))
                 (exceptionCaught [ctx cause]
                   (.printStackTrace cause)
                   (.close ctx)))))
  (stop [_ system] system))
