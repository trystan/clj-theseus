# theseus

**Path-based testing decomplected.**

The ideal of theseus is that with a good description of your application, you could query for and run the smallest set of paths that verify that your app does what you expect it to. Or whatever you want, it's just data.

**Path-based** because theseus creates paths through your app for you.

**Decomplected** because instead of having a large set of automated integration tests where each test navigates through your system to a specific place, does some stuff, and verifies some state along the way; you keep your assertions, data, states, how to navigate, how to do domain actions, etc all seperate and theseus combines them for you.


## Installation

In Leiningen:

    [theseus "0.1.0"]


## To use

First you create a collection of facts about your system that describe preconditions, postconditions, actions, location, and descriptions about your system. Each fact is a map with several keys. Most facts will be either a description of an **action** that has a `:from` location, `:to` location, and `:fn` function or a description of an **expectation** that has either a `:before`, or an `:after`, and a `:verify` function.

The supported keys are:

<table>
  <tr><td>:id</td>
      <td>A unique identifier. Optional, unless you want this fact to be the target of a :before or :after.</td></tr>
  <tr><td>:name</td>
      <td>A natural language description that is displayed when drawing a graph of your system. A good idea but only required if you want it to show up when drawing a graph.</td></tr>
  <tr><td>:from</td>
      <td>The location this action is available from. Optional and generally used for actions. :from is usually a symbol, but can also be a collection or a predicate that takes the current location.</td></tr>
  <tr><td>:to</td>
      <td>The location your system is in after this action is run. Optional and generally used for actions. :to is usually a symbol.</td></tr>
  <tr><td>:fn</td>
      <td>A function that takes the current state and returns a new one. Optional and generally used for actions. It should do everything needed to transition to the new state and only fail in extreme circumstances (that is, it should be robust instead of fail-fast). Don't put assertions in :fn functions - assertions should go in :verify functions. If a :verify and an :fn are both present on the same fact, the :fn will be run first.</td></tr>
  <tr><td>:before</td>
      <td>If specified, this action will appear before the value. Optional and generally used for expectations and setup. Can be :all, :each, the :id of another fact, a location, a list of :ids and locations, or a predicate that takes an :id or location.</td></tr>
  <tr><td>:after</td>
      <td>If specified, this action will appear after the value. Optional and generally used for expectations and cleanup. Can be :all, :each, the :id of another fact, a location, a list of :ids and locations, or a predicate that takes an :id or location.</td></tr>
  <tr><td>:verify</td>
      <td>A function that takes the current state - its return value is ignored. Optional and generally used for expectations. If a :verify and an :fn are both present on the same fact, the verify will be run last.</td></tr>
  <tr><td>:requires</td>
      <td>A map of required initial state. Optional and generally used for actions. Two actions with incompatable :requires will not appear in the same path. {:requires {:a 1}} and {:requires {:a 1}} are compatable; as is {:requires {:a 1}} and {:requires {:b 2}}. However; {:requires {:a 1}} and {:requires {:a 2}} are not compatable. All :requires in the path are merged to form the initial state when running a path.</td>
  <tr><td>:repeat</td>
      <td>If true, this fact can be repeated. Useful for when :from is a collection or predicate.</td></tr></tr>
</table>


## Example

```clj
(ns theseus.example
  (:require [theseus.core :refer :all]))

(defn increment-state-counter [state] ; just an example action
  (assoc state :counter (inc (:counter state 0))))

(defn has-content [something] ; just an example assertion
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

  {:after :home-screen
   :verify (fn [state]
                  (has-content (str "Hello " (:user-name state))))}])

(draw facts "/tmp/example.svg")
;; returns nil

(->> (paths facts :start-screen :logout-screen)
     (map #(map :id (filter :id %))))
;; returns ((:login :become-fancy :logout-from-fancy-screen) (:login :go-to-help :logout))

(->> (paths facts :start-screen :logout-screen)
     (first)
     (run))
;; returns {:counter 3}
```

## To do

**Allow facts to express after-state.** Like they do with required state. This would be used when creating paths.
```clj
;; just a possible thought
{:id :move-from-nevada-to-california
 :from :move-screen
 :to :moved-screen
 :fn some-other-function
 :requires { :user-home-state "NV" }
 :ensures { :user-home-state "CA" }}
```

**Allow paths to fork from each other.** This would be usefull if an action had a lot of side effects that could be verified in parallel by running in new browser tabs (for example). I think it would be safe to fork paths as long as they didn't affect what has been done before them so maybe it would be better to express that.
```clj
;; just a possible thought
{:id :share-everywhere
 :from :share-screen
 :to :share-confirmation-screen
 :fn (fn [state]
       (->> state
         (share-on "facebook")
         (share-on "reddit")
         (share-on "linked in")
         (share-on "twitter")))}
{:after :share-everywhere
 :fork true
 :verify #(verify-shared-on "facebook")}
{:after :share-everywhere
 :fork true
 :verify #(verify-shared-on "reddit")}
{:after :share-everywhere
 :fork true
 :verify #(verify-shared-on "linked in")}
{:after :share-everywhere
 :fork true
 :verify #(verify-shared-on "twitter")}
```
