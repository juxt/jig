(ns snake.cljs.app
    (:require [clojure.string :as s]
              [snake.cljs.home :as home]
              clojure.browser.repl)
    (:require-macros [dommy.macros :refer [sel sel1]]))

(def default-hash "#/")

(defn- bind-hash [!hash]
  (letfn [(on-hash-change []
            (reset! !hash (.-hash js/location)))]
    
    (set! (.-onhashchange js/window) on-hash-change)
    
    (when (s/blank? (.-hash js/location))
      (set! (.-hash js/location) default-hash))
    
    (on-hash-change)))

(set! (.-onload js/window)
      (fn []
        (let [!hash (atom nil)]
          (home/watch-hash! !hash)
          (bind-hash !hash))))


