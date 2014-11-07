(ns theseus.core
  (:require [lacij.edit.graph :refer :all]
            [lacij.layouts.layout :refer :all]
            [lacij.view.graphview :refer [export]])
  (:gen-class))


(defn not-in?
  "Return true if x is not in coll."
  [coll x]
  (not-any? (hash-set x) coll))


(defn actions-from
  "Find all actions from some place in a collection of facts."
  [place facts]
  (filter #(= place (:from %)) facts))


(defn has-value-or-missing [m k v]
  (= (get m k v) v))

(defn compatable-requirements [requires m]
  (if (nil? requires)
    true
    (reduce #(and %1 %2) (map #(has-value-or-missing m % (get requires %)) (keys requires)))))

(defn step
  "Find all valid next steps of one path in a collection of facts."
  [path facts]
  (let [requires (apply merge (map :requires path))
        next-steps (actions-from (:to (last path)) facts)
        compatable-steps (filter #(compatable-requirements requires (:requires %)) next-steps)
        valid-next-steps (filter (partial not-in? path) compatable-steps)]
    (if (seq valid-next-steps)
      (map #(conj path %) valid-next-steps)
      [path])))


(defn steps
  "Find all valid next steps of many paths in a collection of facts."
  [paths facts]
  (mapcat #(step % facts) paths))


(defn paths-from
  "Find all valid continuations of some paths in a collection of facts."
  [paths facts]
  (loop [current-paths paths
         next-paths (steps paths facts)]
    (if (= next-paths current-paths)
      next-paths
      (recur next-paths (steps next-paths facts)))))


(defn sub-paths-to
  "Find all subpaths of a path that end at a specific place."
  [to path]
  (if (== 1 (count path))
    [path]
    (loop [so-far []
           [here & remaining] path
           paths []]
      (cond
       (nil? here)
         paths
       (= to (:to here))
         (recur (conj so-far here) remaining (conj paths (conj so-far here)))
       :else
         (recur (conj so-far here) remaining paths)))))

(defn add-ancillary
  "Add all before and after facts to a path"
  [facts path]
  (let [before-all (filter #(= :all (:before %)) facts)
        before-each (filter #(= :each (:before %)) facts)
        after-all (filter #(= :all (:after %)) facts)
        after-each (filter #(= :each (:after %)) facts)
        before (fn [x] (filter #(and (:before %) (= (:id x) (:before %))) facts))
        after (fn [x] (filter #(and (:after %) (= (:id x) (:after %))) facts))]
    (->> path
         (mapcat (fn [x] (concat before-each (before x) [x] (after x) after-each)))
         (#(concat before-all % after-all)))))

(defn paths
  "Find all paths in the facts of defined actions from one place to another."
  ([facts from to]
   (let [starting-paths (map vector (actions-from from facts))
          all-paths (paths-from starting-paths facts)]
      (mapcat #(sub-paths-to to %) all-paths))))


(defn run-action
  "Run the state through the :fn of an action (if it exists). Pass the state to any :verify function after as well."
  [state action]
  (let [new-state ((get action :fn identity) state)]
    (when (:verify action)
      ((:verify action) state))
    new-state))

(defn run
  "Run the actions in a given path. Takes an optional state to pass through; otherwise, all :requires will be merged for the initial state."
  ([path]
    (run path (reduce merge {} (map :requires path))))
  ([path state]
    (reduce run-action state path)))


(defn draw
  "Draw an svg facts of a collection of facts."
  [facts path]
  (let [valid-nodes (filter :name facts)
        add-nodes (fn [g nodes]
          (reduce (fn [g n] (add-node g n (.replace (name n) "-" " ")))
                  g
                  nodes))
        add-edges (fn [g edges]
          (reduce (fn [g e]
                    (add-label (add-edge g (:id e) (:from e) (:to e)) (:id e) (:name e)))
                  g
                  edges))
        g (-> (graph :width 1024 :height 768)
              (add-default-edge-style :stroke "grey")
              (add-nodes (distinct (concat (map :from valid-nodes) (map :to valid-nodes))))
              (add-edges valid-nodes)
              (layout :hierarchical :flow :out)
              (build))]
    (export g path :indent "yes")))
