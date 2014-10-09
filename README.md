# theseus

Path-based testing decomplected.

Describe your application as a state machine with states and actions.
Query that description for the shortest path from A to B, all paths from A,

## Installation

In Leiningen:

    [theseus "0.1.0"]

## Example

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

    ;; returns ((:login :become-fancy :logout-from-fancy-screen) (:login :go-to-help :logout-from-help-screen))

    ((comp run first) (paths catalog :start-screen :logout-screen))

    ;; returns {:counter 3}


## To do

Generate graphs from a list of actions. Maybe using `graphvis` or something.

Have actions express data preconditions that other actions can use. So a `share-on-facebook` action can express that it only works with users who have facebook accounts and an earlier `login` action would know to login as a facebook user.

    ;; just a possible thought
    {:id :share-on-facebook
     :from :share-screen
     :to :after-share-screen
     :fn (partial share-on "facebook")
     :requires { :is-facebook-user true })}



Define seperate assertions that can be run at the right time. So a `verify-shared-on-facebook` assertion would be run in another tab or background worker whenever something is shared on facebook.

    ;; just a possible thought
    {:id :verify-shared-on-facebook
     :after :share-on-facebook
     :fn (fn [state]
           (visit facebook-url)
           (login-to-facebook-if-necessary (:facebook-user state))
           (assert-post-is-on-wall (:post-text state)))}
