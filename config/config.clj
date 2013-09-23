;; This config tells Jig which components to run
{
 :jig/components
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
                    :jig/project "../juxtweb/project.clj"
                    :static-path  "../website-static"
                    }

  ;; ----------------------------------------------------------------
  ;; Congreve accounts

  :congreve/accounts/db
  {:jig/component pro.juxt.accounting.jig/Database
   :jig/project "../accounting/project.clj"
   :db {:uri "datomic:mem://pro.juxt/accounts"}
   :accounts-file "/home/malcolm/Dropbox.private/Congreve/accounts.edn"
   }

  :congreve/accounts/service
  {:jig/component pro.juxt.accounting.jig/PedestalService
   :jig/dependencies [:juxtweb/web :congreve/accounts/db]
   :jig/project "../accounting/project.clj"
   :pro.juxt.accounting/data :congreve/accounts/db
   :jig.web/app-name :juxtweb/web
   :jig.web/context "/congreve"
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
                     :jig/dependencies [:examples/docsite :congreve/accounts/service :juxtweb/service]
                     :jig.web.firefox-reload/host "localhost"
                     :jig.web.firefox-reload/port 32000
                     }

  }

 :jig/projects
 [{
   :jig/project "../juxtweb/project.clj"
   :jig/classloader-pinned? false
   }
  {
   :jig/project "../accounting/project.clj"
   :jig/classloader-pinned? true
   ;; eg. extra classpath, source dirs, etc. here
   }
  ]

 }
