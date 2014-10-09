(ns theseus.example
  (:require [theseus.core :refer :all]))

(defn increment-state-counter [state]
  (assoc state :counter (inc (:counter state 0))))

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
   :fn increment-state-counter}

  {:id :go-to-help
   :name "help"
   :from :home-screen
   :to :help-screen
   :fn increment-state-counter}

  {:id :logout-from-help-screen
   :name "logout"
   :from :help-screen
   :to :logout-screen
   :fn increment-state-counter}

  {:before :all
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:after :each
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:before :go-to-help
   :invariant (fn [state]
                (has-content (str "Hello " (:user-name state))))}])

(draw catalog "/tmp/example.svg")

(map #(map :id (filter :id %)) (paths catalog :start-screen :logout-screen))
((comp run first) (paths catalog :start-screen :logout-screen))
