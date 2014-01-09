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

(ns jig.netty
  (:require
   jig
   [jig.util :refer (get-dependencies)]
   [clojure.tools.logging :refer :all])
  (:import
   (io.netty.channel ChannelInitializer ChannelOption ChannelHandler)
   (io.netty.channel.nio NioEventLoopGroup)
   (io.netty.bootstrap ServerBootstrap)
   (io.netty.channel.socket.nio NioServerSocketChannel)
   (jig Lifecycle)))

;; From https://github.com/netty/netty/wiki/User-guide-for-5.x
(deftype Server [config]
  Lifecycle
  (init [_ system] system)
  (start [_ system]

    (debugf "Dependencies are %s" (vec (get-dependencies system config)))

    (let [handlers (keep #(get-in system [(:jig/id %) ::handler-factory]) (get-dependencies system config))]

      (when (empty? handlers)
        (throw (ex-info (format "No dependencies of %s register entries of %s"
                                (:jig/id config) ::handler-factory) {:jig/id (:jig/id config)})))

      (let [boss-group (NioEventLoopGroup.)
            worker-group (NioEventLoopGroup.)]
        (let [b (ServerBootstrap.)]
          (-> b
              (.group boss-group worker-group)
              (.channel NioServerSocketChannel)
              (.childHandler
               (proxy [ChannelInitializer] []
                 (initChannel [ch]
                   (debugf "Initializing channel with handlers: %s" (vec handlers))
                   (-> ch (.pipeline) (.addLast (into-array ChannelHandler (map (fn [f] (if (fn? f) (f) f)) handlers)))))))
              (.option ChannelOption/SO_BACKLOG (int (or (:so-backlog config) 128)))
              (.childOption ChannelOption/SO_KEEPALIVE (or (:so-keepalive config) true)))

          (when-not (:port config)
            (throw (ex-info "No :port specified in component configuration" {:jig/id (:jig/id config)})))

          (-> system
              (assoc-in [(:jig/id config) :channel] (-> b (.bind (:port config))))
              (assoc-in [(:jig/id config) :event-loop-groups :boss-group] (NioEventLoopGroup.))
              (assoc-in [(:jig/id config) :event-loop-groups :worker-group] (NioEventLoopGroup.)))))))
  (stop [_ system]
    (let [fut (get-in system [(:jig/id config) :channel])]
      (.awaitUninterruptibly fut)       ; await for it to be bound
      (-> fut (.channel) (.close) (.sync)))
    (.shutdownGracefully (get-in system [(:jig/id config) :event-loop-groups :worker-group]))
    (.shutdownGracefully (get-in system [(:jig/id config) :event-loop-groups :boss-group]))
    system))
