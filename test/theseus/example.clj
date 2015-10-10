(ns theseus.example
  (:require [theseus.core :refer :all]))

(defn increment-state-counter [state]
  (assoc state :counter (inc (:counter state 0))))

(defn has-content [something]
  true)

(def facts [
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

  {:id :go-to-help
   :name "help"
   :from :home-screen
   :to :help-screen
   :fn increment-state-counter}

  {:id :logout
   :name "logout"
   :from [:fancy-screen :help-screen]
   :to :logout-screen
   :fn increment-state-counter}

  {:before :all
   :verify (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:after :each
   :verify (fn [state]
                (has-content (str "Hello " (:user-name state))))}

  {:before :go-to-help
   :verify (fn [state]
                (has-content (str "Hello " (:user-name state))))}])

(draw facts "/tmp/example.svg")

(->> (paths facts :start-screen :logout-screen)
     (map #(map :id (filter :id %))))

(->> (paths facts :start-screen :logout-screen)
     (first)
     (run))
