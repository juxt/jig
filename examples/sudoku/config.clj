{
 :jig/components
 {
  ;; Sudoku - demonstrating the support for good old Ring/Compojure

  :examples.sudoku
  {:jig/component sudoku/Website
   :jig/project "examples/sudoku/project.clj"
   :name "Sudoku"
   :description "A difficult Sudoku grid solved using core.logic. Renders the grid using Hiccup and serves it via Ring."
   :jig.example/uri "http://localhost:8091/sudoku.html"
   ;; This puzzle is Copyright AI SUDOKU.
   ;; I have interpreted this usage to fall under 'research purposes'
   ;; which is allowed http://www.aisudoku.com/index_en.html
   :puzzle [[8 0 0 0 0 0 0 0 0]
            [0 0 3 6 0 0 0 0 0]
            [0 7 0 0 9 0 2 0 0]
            [0 5 0 0 0 7 0 0 0]
            [0 0 0 0 4 5 7 0 0]
            [0 0 0 1 0 0 0 3 0]
            [0 0 1 0 0 0 0 6 8]
            [0 0 8 5 0 0 0 1 0]
            [0 9 0 0 0 0 4 0 0]]}

  ;; Note the change in the dependency order - Jetty depends on
  ;; Compojure (for it's handler), while Compojure depends on handlers
  ;; provided by other components.

  :examples.sudoku/routing
  {:jig/component jig.compojure/HandlerComposer
   :jig/project "examples/sudoku/project.clj"
   :jig/dependencies [:examples.sudoku]}

  :examples.sudoku/webserver
  {:jig/component jig.jetty/Server
   :jig/project "examples/sudoku/project.clj"
   :jig/dependencies [:examples.sudoku/routing]
   :port 8091}

  }
 }
