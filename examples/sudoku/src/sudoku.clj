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

(ns sudoku
  (:refer-clojure :exclude [==])

  (:require
   [clojure.core.logic :refer :all]
   [clojure.core.logic.fd :as fd]
   [hiccup.core :refer (html)]
   [clojure.pprint :refer (pprint)]
   jig
   [jig.console.example :refer (add-example)]
   [jig.ring :refer (add-ring-handler)]

   [compojure.core :refer (routes defroutes GET)])

  (:import (jig Lifecycle)))

(defn hinto
  "Unifies a logical variable with a hint (a hint is a non blank square
  in the starting grid). Blank squares are represented by 0 so fail the
  pos? check and therefore succeed."
  [var hint]
  (if (pos? hint)
    (== var hint)
    succeed))

(defn solve
  "Solve a Sudoku puzzle. The given puzzle must be represented as a
  sequence of 9 sequences, each representing a single row (left-to-right
  across the grid). Each row contains 9 numbers denoting the hints,
  blanks are represented by zero. The algorithm uses core.logic,
  starting with 81 dynamically created lvars (logic variables), upon
  which logic relations are applied, constraining the possible
  solutions. Since we know that Sudoku can only have one solution, we
  only ask for one solution in the results (and take the first one).

  Acknowledgment: This algorithm has been adapted from the example in
  the core.logic test suite."
  [puzzle]
  (first ; return the first (and only) solution
   (let [vars (repeatedly 81 lvar)]
     (run 1 [q]
          (== q vars)
          ;; Every lvar must be in the range 1 to 9 (inclusive)
          (everyg #(fd/in % (apply fd/domain (range 1 10))) vars)
          ;; lvars match their corresponding non-zero hints in the starting grid
          (everyg (partial apply hinto) (map vector vars (apply concat puzzle)))
          ;; Every lvar in a row must be distinct
          (everyg fd/distinct (partition 9 vars))
          ;; Every lvar in a column must be distinct
          (everyg fd/distinct (apply map vector (partition 9 vars)))
          ;; Every lvar in a square must be distinct
          (everyg fd/distinct (->> vars (partition 3) (partition 3)
                                   (apply interleave) (partition 3)
                                   (map (partial apply concat))))))))

(defroutes handler
  (GET "/sudoku.html" [:as req]
    {:status 200
     :body
     (let [puzzle (-> req :jig/config :puzzle)]
       (html
        [:html
         [:head [:title "Sudoku"]]
         [:body
          [:style " table { border: 1px solid black;
                border-collapse: collapse } td { width: 20px; height:
                20px; text-align: center } td { border: 1px solid grey;
                } td:nth-child(1) { border-left: 2px solid black; }
                td:nth-child(4) { border-left: 2px solid black; }
                td:nth-child(7) { border-left: 2px solid black; }
                td:nth-child(9) { border-right: 2px solid black; }
                tr:nth-child(1) { border-top: 2px solid black; }
                tr:nth-child(4) { border-top: 2px solid black; }
                tr:nth-child(7) { border-top: 2px solid black; }
                tr:nth-child(9) { border-bottom: 2px solid black; }"]

          [:h2 "Sudoku"]

          [:h3 "Puzzle"]

          [:table
           (for [row puzzle]
             [:tr
              (for [el row]
                [:td
                 (if (and (number? el) (pos? el)) el)])])]

          [:h3 "Solution"]

          [:table
           (for [row (partition 9 (solve puzzle))]
             [:tr
              (for [el row]
                [:td
                 (if (and (number? el) (pos? el)) el)])])]

          ]]))})
  (GET "/test.html" [:as req]
    {:status 200
     :body (html
            [:html
             [:body
              [:p "System keys are " ]
              [:pre (str (keys (:jig/system req)))]
              [:p "Config is" ]
              [:pre (with-out-str (pprint (:jig/config req)))]
              ]])}))


(deftype Website [config]
  Lifecycle
  (init [_ system]
    (-> system
        (add-example config)
        (add-ring-handler config handler)
        ))
  (start [_ system] system)
  (stop [_ system] system))
