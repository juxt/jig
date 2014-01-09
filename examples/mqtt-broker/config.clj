{:jig/components

 {:examples.mqtt-broker/decoder
  {:jig/component jig.netty.mqtt/MqttDecoder
   :jig/project "examples/mqtt-broker/project.clj"}

  :examples.mqtt-broker/encoder
  {:jig/component jig.netty.mqtt/MqttEncoder
   :jig/project "examples/mqtt-broker/project.clj"}

  :examples.mqtt-broker/handler
  {:jig/component mqtt-broker/MqttHandler
   :jig/project "examples/mqtt-broker/project.clj"}

  :examples.mqtt-broker/server
  {:jig/component jig.netty/Server
   :jig/dependencies [:examples.mqtt-broker/decoder :examples.mqtt-broker/encoder :examples.mqtt-broker/handler]
   :jig/project "examples/mqtt-broker/project.clj"
   :port 1883}}}
