(ns benchmarks
  (:use jordanlewis.data.union-find)
  (:require [criterium.core :refer [bench quick-bench]]))

(defn make-stars [stars points]
  (for [a (range stars)
        b (range points)
        :let [b (+ stars (* a points) b)]]
    [a b (str b)]))

(defn partition-graph [uf stars points -conj -union]
  (reduce
    (fn [uf [a b c]]
      (-> uf
          (-conj a)
          (-conj b)
          (-conj c)
          (-union a b)
          (-union b c)))
    uf
    (make-stars stars points)))

(comment

  (quick-bench (partition-graph (union-find) 10 100 conj union))
  (quick-bench (persistent! (partition-graph (transient (union-find)) 10 100 conj! union!)))

  (quick-bench (dorun (make-stars 10 1000)))
  (quick-bench (partition-graph (union-find) 10 1000 conj union))
  (quick-bench (persistent! (partition-graph (transient (union-find)) 10 1000 conj! union!)))
  )
