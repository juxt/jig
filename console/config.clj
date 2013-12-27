{
 :jig/components
 {
  ;; Console - demonstrating the support for Pedestal services

  :console/stencil-loader
  {:jig/component jig.stencil/StencilLoader
   :jig/project "console/project.clj"}

  :console/main
  {:jig/component jig.console/Console
   :jig/project "console/project.clj"
   :jig/dependencies [:console/stencil-loader]
   ;;:jig.stencil/loader :console/stencil-loader
   }

  :console/routing
  {:jig/component jig.bidi/Router
   :jig/project "console/project.clj"
   :jig/dependencies [:console/main]
   ;; Optionally, route systems can be mounted on a sub-context
   ;;:jig.web/context "/console"
   }

  :console/server
  {:jig/component jig.http-kit/Server
   :jig/project "console/project.clj"
   :jig/dependencies [:console/routing]
   :port 8001}

  ;; Now for some default console extensions
  :console.extension/system-browser
  {
   :jig/component jig.console.system-browser/SystemBrowser
   :jig/project "console/extensions/system-browser/project.clj"
   :jig/dependencies [:console/main]
   :jig.web/context "/system"
   }

  :console.extension/codox
  {
   :jig/component jig.console.codox/JigComponent
   :jig/project "console/extensions/codox/project.clj"
   :jig/dependencies [:console/main]
   :jig.web/context "/codox"
   }

  :console.extension/todos
  {
   :jig/component jig.console.todo/JigComponent
   :jig/project "console/extensions/todo-browser/project.clj"
   :jig/dependencies [:console/main]
   :jig.web/context "/todo"
   }

  :console.extension/example-browser
  {
   :jig/component jig.console.example-browser/ExampleBrowser
   :jig/project "console/extensions/example-browser/project.clj"
   :jig/dependencies [:console/main]
   :jig.web/context "/examples"
   }

;; Other ideas
;;;            ["Codox" ::codox-page]
;;;           [(str \T \O \D \O \s) ::todos-page]
            ;;           ["Config" nil]





            ;;           ["Components" nil]
            ;;           ["Logs" nil]
            ;;           ["Testing" nil]
            ;;           ["Structure" nil]
            ;;           ["Stats" nil]
            ;;           ["Tracing" nil]
            ;;           ["Profiling" nil]
            ;;           ["Visualisations" nil]
            ;;           ["Help" nil]
  }
 }
