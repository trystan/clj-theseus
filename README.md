# theseus

**Path-based testing decomplected.**

The ideal of theseus is that with a good description of your application, you could query for and run the smallest set of paths that verify that your app does what you expect it to. Or whatever you want, it's just data.

**Path-based** because theseus creates paths through your app for you.

**Decomplected** because instead of having a large set of automated integration tests where each test navigates through your system to a specific place, does some stuff, and verifies some state along the way; you keep your assertions, data, states, how to navigate, how to do domain actions, etc all seperate and theseus combines them for you.

First you create a catalog of facts about your system that describe preconditions, postconditions, actions, states, and descriptions about your system. Each fact is a map with several keys:

<table>
  <tr><td>:id</td>
      <td>A unique identifier. Optional, unless you want this fact to be the target of a :before or :after.</td></tr>
  <tr><td>:name</td>
      <td>A natural language description that is displayed when drawing a graph of your system. Required if you want it to show up when drawing a graph.</td></tr>
  <tr><td>:from</td>
      <td>The state this action is available from. Optional but expected of actions.</td></tr>
  <tr><td>:to</td>
      <td>The state your system is in after this action is run. Optional but expected of actions.</td></tr>
  <tr><td>:fn</td>
      <td>A function that takes the current state and returns a new one. Don't put assertions in :fn functions - assertions should go in :invariant functions. If an :invariant and an :fn are both present on the same fact, the :invariant will be run first. Optional but expected of actions.</td></tr>
  <tr><td>:before</td>
      <td>If specified, this action will appear before the value. Optional but expected of invariants and setup. Can be :all, :each, or the :id of another fact.</td></tr>
  <tr><td>:after</td>
      <td>If specified, this action will appear after the value. Optional but expected of invariants and cleanup. Can be :all, :each, or the :id of another fact.</td></tr>
  <tr><td>:invariant</td>
      <td>A function that takes the current state. It's return value is ignored. If an :invariant and an :fn are both present on the same fact, the invariant will be run first.</td></tr>
</table>

## Installation

In Leiningen:

    [theseus "0.1.0"]

## Example

```clj
(ns theseus.example
  (:require [theseus.core :refer :all]))

(defn increment-state-counter [state] ; just an example action
  (assoc state :counter (inc (:counter state 0))))

(defn has-content [something] ; just an example assertion
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
;; returns => nil

(map #(map :id (filter :id %)) (paths catalog :start-screen :logout-screen))
;; returns => ((:login :become-fancy :logout-from-fancy-screen) (:login :go-to-help :logout-from-help-screen))

((comp run first) (paths catalog :start-screen :logout-screen))
;; returns => {:counter 3}
```

## To do

Allow `:before` and `:after` to apply to states as well as `:all`, `:each`, and a specific id. I'm not sure how that would work out best, but it would be nice to verify invariants of a specific screen (eg, `if the user is logged in, then the user's name appears on the home screen`).


Have actions express data preconditions that other actions can use. So a `share-on-facebook` action can express that it only works with users who have facebook accounts and an earlier `login` action would know to login as a facebook user.

```clj
;; just a possible thought
{:id :share-on-facebook
 :from :share-screen
 :to :after-share-screen
 :fn (partial share-on "facebook")
 :requires { :is-facebook-user true })}
```
