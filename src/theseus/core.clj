(ns theseus.core
  (:require [lacij.edit.graph :refer :all]
            [lacij.layouts.layout :refer :all]
            [lacij.view.graphview :refer [export]])
  (:gen-class))


(defn not-in?
  "Return true if x is not in coll."
  [coll x]
  (not (.contains coll x)))


(defn actions-from
  "Find all actions from some place in a collection of facts."
  [place facts]
  (for [fact facts
        :when (cond
               (coll? (:from fact)) (.contains (:from fact) place)
               (fn? (:from fact))   ((:from fact) place)
               :else                (= place (:from fact)))]
    fact))


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
        valid-next-steps (filter #(or (:repeat %)
                                      (not-in? path %)) compatable-steps)]
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
        before (fn [x] (filter #(and (:before %) (cond
                                                  (coll? (:before %)) (or (.contains (:before %) (:id x))
                                                                          (.contains (:before %) (:to x)))
                                                  (fn? (:before %)) (or ((:before %) (:id x))
                                                                        ((:before %) (:to x)))
                                                  :else (or (= (:id x) (:before %))
                                                            (= (:to x) (:before %))))) facts))
        after (fn [x] (filter #(and (:after %) (cond
                                                (coll? (:after %)) (or (.contains (:after %) (:id x))
                                                                       (.contains (:after %) (:to x)))
                                                (fn? (:after %)) (or ((:after %) (:id x))
                                                                     ((:after %) (:to x)))
                                                :else (or (= (:id x) (:after %))
                                                          (= (:to x) (:after %))))) facts))]
    (->> path
         (mapcat (fn [x] (concat before-each (before x) [x] (after x) after-each)))
         (#(concat before-all % after-all)))))

(defn paths
  "Find all paths in the facts of defined actions from one place to another."
  ([facts from to]
   (let [starting-paths (map vector (actions-from from facts))
         all-paths (paths-from starting-paths facts)
         full-paths (mapcat #(sub-paths-to to %) all-paths)]
      (map #(add-ancillary facts %) full-paths))))


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
  "Draw an svg image of a collection of facts."
  [facts filepath]
  (let [valid-nodes (filter :name facts)
        add-nodes (fn [g nodes]
          (reduce (fn [g n] (add-node g n (.replace (name n) "-" " ")))
                  g
                  nodes))
        add-edges (fn [g edges]
          (reduce (fn [g e]
                    (cond
                     (coll? (:from e)) g
                     (fn? (:from e)) g
                     :else (add-label (add-edge g (:id e) (:from e) (:to e)) (:id e) (:name e))))
                  g
                  edges))
        g (-> (graph :width 1024 :height 768)
              (add-default-edge-style :stroke "grey")
              (add-nodes (distinct (filter keyword? (concat (map :from valid-nodes) (map :to valid-nodes)))))
              (add-edges valid-nodes)
              (layout :hierarchical :flow :out)
              (build))]
    (export g filepath :indent "yes")))
