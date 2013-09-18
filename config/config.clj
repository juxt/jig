;; This declarative config tells Jig which components to run
{:components
 ;; Keys are usually keywords or strings, but can be anything (uuids, urls, ...)
 {
  :server
  {:jig/component jig.web.server/Component
   :io.pedestal.service.http/port 8000
   :io.pedestal.service.http/type :jetty
   }

  :opensensors/web
  {:jig/component jig.web.app/Component
   :jig/dependencies [:server]
   :jig/scheme :http
   :jig/hostname "localhost"
   :jig.web/server :server
   }

  :opensensors/mosquitto-bridge
  {:jig/component opensensors.mqtt/MqttBridge
   }

  :opensensors/service
  {:jig/component opensensors.core/WebServices
   :jig/dependencies [:opensensors/web :opensensors/mosquitto-bridge]
   :jig.web/app-name :opensensors/web
   :channel [:opensensors/mosquitto-bridge :channel]
   :static-path "../adl/mqtt.opensensors.io/public"
   }

  :opensensors/scheduled-thread-pool
  {:jig/component opensensors.core/ScheduledThreadPool
   }

  :opensensors/dummy-event-generator
  {:jig/component opensensors.core/DummyEventGenerator
   :jig/dependencies [:opensensors/service :opensensors/scheduled-thread-pool]
   }

  }}
