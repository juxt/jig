(ns examples.docsite.markdown
  (:require
   [endophile.core :refer (mp to-clj)]
   [clojure.java.io :refer (resource)]))

(defn emit-element
  ;; An alternative emit-element that doesn't cause newlines to be
  ;; inserted around punctuation.
  [e]
  (if (instance? String e)
    (print e)
    (do
      (print (str "<" (name (:tag e))))
      (when (:attrs e)
	(doseq [attr (:attrs e)]
	  (print (str " " (name (key attr)) "='" (val attr)"'"))))
      (if (:content e)
	(do
	  (print ">")
          (if (instance? String (:content e))
            (print (:content e))
            (doseq [c (:content e)]
              (emit-element c)))
	  (print (str "</" (name (:tag e)) ">")))
	(print "/>")))))

(defn markdown [content]
  (->> content mp to-clj (map emit-element) dorun with-out-str))
