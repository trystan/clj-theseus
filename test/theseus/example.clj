(ns theseus.core-example
  (:require [theseus.core :refer :all]))

(defn increment-state-counter [state]
  (if (:counter state)
    (assoc state :counter (inc (:counter state)))
    (assoc state :counter 1)))

(defn has-content [something]
  true)

(def catalog [
  {:id :login
   :name "login"
   :from :start-screen
   :to :home-screen
   :fn increment-state-counter}

  {:id :become-fancy
   :name "fancify"
   :from :home-screen
   :to :fancy-screen
   :fn increment-state-counter}

  {:id :logout-from-fancy-screen
   :name "logout"
   :from :fancy-screen
   :to :logout-screen
   :fn (fn [state]
         { :pre [(< 1 (:counter state))] } ;; you can use clojure pre and post conditions
         (increment-state-counter state))}

  {:id :go-to-help
   :name "help"
   :from :home-screen
   :to :help-screen
   :fn increment-state-counter}

  {:id :logout-from-help-screen
   :name "logout"
   :from :help-screen
   :to :logout-screen
   :fn (fn [state]
         { :pre [(< 1 (:counter state))] } ;; you can use clojure pre and post conditions
         (increment-state-counter state))}

  {:before :all
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:after :each
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:before :go-to-help
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:before :help-screen
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}])

(map #(map :id (filter :id %)) (paths catalog :start-screen :logout-screen))

((comp run first) (paths catalog :start-screen :logout-screen))

(draw catalog "/tmp/example.svg")
