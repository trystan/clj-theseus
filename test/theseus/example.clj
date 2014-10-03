(ns theseus.core-example
  (:require [theseus.core :refer :all]))

(defn increment-state-counter [state]
  (if (:counter state)
    (assoc state :counter (inc (:counter state)))
    (assoc state :counter 1)))


(def catalog [
  {:id :login
   :from :start-screen
   :to :home-screen
   :fn increment-state-counter}

  {:id :become-fancy
   :from :home-screen
   :to :fancy-screen
   :fn increment-state-counter}

  {:id :logout-from-fancy-screen
   :from :fancy-screen
   :to :logout-screen
   :fn (fn [state]
         { :pre [(< 1 (:counter state))] } ;; you can use clojure pre and post conditions
         (increment-state-counter state))}

  {:id :go-to-help
   :from :home-screen
   :to :help-screen
   :fn increment-state-counter}

  {:id :logout-from-help-screen
   :from :help-screen
   :to :logout-screen
   :fn (fn [state]
         { :pre [(< 1 (:counter state))] } ;; you can use clojure pre and post conditions
         (increment-state-counter state))}])

(map #(map :id %) (paths catalog :start-screen :logout-screen))

((comp run first) (paths catalog :start-screen :logout-screen))
