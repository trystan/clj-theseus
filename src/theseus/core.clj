(ns theseus.core
  (:use lacij.edit.graph
        lacij.view.graphview
        (lacij.layouts core layout))
  (:gen-class))


(defn not-in? [coll x]
  "Return true if x is not in coll"
  (not (some #{x} coll)))


(defn run-action [state action]
  "Run the state through an action."
  ((:fn action) state))


(defn actions-from [place graph]
  "Find all actions from some place in a graph."
  (filter #(= place (:from %)) graph))


(defn step [path graph]
  "Find all valid next steps of some path in a graph."
  (let [next-steps (actions-from (:to (last path)) graph)
        valid-next-steps (filter (partial not-in? path) next-steps)]
    (if (seq valid-next-steps)
      (map #(conj path %) valid-next-steps)
      [path])))


(defn steps [paths graph]
  "Find all valid next steps of some paths in a graph."
  (mapcat #(step % graph) paths))


(defn paths-from [paths graph]
  "Find all valid continuations of some paths in a graph."
  (loop [current-paths paths
         next-paths (steps paths graph)]
    (if (= next-paths current-paths)
      next-paths
      (recur next-paths (steps next-paths graph)))))


(defn sub-paths-to [to path]
  "Find all subpaths of a path that end at a specific place."
  (loop [so-far []
         [here & remaining] path
         paths []]
    (cond
     (nil? here)
       paths
     (= to (:to here))
       (recur (conj so-far here) remaining (conj paths (conj so-far here)))
     :else
       (recur (conj so-far here) remaining paths))))


(defn paths
  "Find all paths in the graph of defined actions from one place to another."
  ([graph from to]
    (let [starting-paths (map (partial vector) (actions-from from graph))
          all-paths (paths-from starting-paths graph)]
      (mapcat (partial sub-paths-to to) all-paths))))


(defn run
  "Run the actions in a given path. Takes an optional state to pass through."
  ([path]
    (run path {}))
  ([path state]
    (reduce run-action state path)))


(defn draw [actions path]
  "Draw an svg graph to the specificed path."
  (let [add-nodes (fn [g nodes]
          (reduce (fn [g n] (add-node g n (.replace (name n) "-" " ")))
                  g
                  nodes))
        add-edges (fn [g edges]
          (reduce (fn [g e]
                    (add-label (add-edge g (:id e) (:from e) (:to e)) (:id e) (:name e)))
                  g
                  edges))
        g (-> (graph :width 800 :height 800)
              (add-default-edge-style :stroke "grey")
              (add-nodes (distinct (concat (map :from actions) (map :to actions))))
              (add-edges actions)
              (layout :hierarchical :flow :out)
              (build))]
    (export g path :indent "yes")))
