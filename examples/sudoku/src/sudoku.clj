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
   [jig.web
    [app :refer (add-routes)]]
   [compojure.core :refer (routes defroutes GET)]
   [clojure.core.logic :refer :all]
   [clojure.core.logic.fd :as fd]
   [hiccup.core :refer (html)]
   [clojure.pprint :refer (pprint)]
   jig)
  (:import (jig Lifecycle)))

(defn hinto [var hint]
  (if (pos? hint)
    (== var hint)
    succeed))

(defn solve [puzzle]
  (first
   (let [vars (repeatedly 81 lvar)]
     (run 1 [q]
          (== q vars)
          (everyg #(fd/in % (apply fd/domain (range 1 10))) vars)
          (everyg (partial apply hinto) (map vector vars puzzle))
          (everyg fd/distinct (partition 9 vars))
          (everyg fd/distinct (apply map vector (partition 9 vars)))
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
           (for [row (partition 9 puzzle)]
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
