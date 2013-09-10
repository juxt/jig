;; This config tells Jig which components to run
{:components
 ;; Keys are usually keywords or strings, but can be anything (uuids, urls, ...)
 {
  :server {:jig/component jig.web.server/Component
           :io.pedestal.service.http/port 8000
           :io.pedestal.service.http/type :jetty
           }

  ;; ----------------------------------------------------------------
  ;; juxt.pro

  :juxtweb/web {:jig/component jig.web.app/Component
                :jig/dependencies [:server]
                :jig/scheme :http
                :jig/hostname "localhost"
                :jig.web/server :server
                }

  :juxtweb/service {:jig/component pro.juxt.website.core/Component
                    :jig/dependencies [:juxtweb/web]
                    :jig.web/app-name :juxtweb/web
                    :static-path  "../website-static"
                    }

  ;; ----------------------------------------------------------------
  ;; Examples

  :examples/server {:jig/component jig.web.server/Component
                    :io.pedestal.service.http/port 8001
                    :io.pedestal.service.http/type :jetty
                    :io.pedestal.service.http/resource-path "/assets"
                    }

  :examples/web {:jig/component jig.web.app/Component
                 :jig/dependencies [:examples/server]
                 :jig/scheme :http
                 :jig/hostname "localhost"
                 :jig.web/server :examples/server}

  :examples/docsite {:jig/component examples.docsite.core/ReadmeComponent
                     :jig/dependencies [:examples/web]
                     :jig.web/app-name :examples/web
                     }

  ;; Reloading
  :firefox-reloader {:jig/component jig.web.firefox-reload/Component
                     :jig/dependencies [:juxtweb/service]
                     :jig.web.firefox-reload/host "localhost"
                     :jig.web.firefox-reload/port 32000
                     }

  }}
