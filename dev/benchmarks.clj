(ns benchmarks
  (:use jordanlewis.data.union-find
        dev-utils)
  (:require [criterium.core :refer [bench quick-bench]]))

(comment

  (quick-bench (partition-graph (union-find) 10 100 conj union))
  (quick-bench (persistent! (partition-graph (transient (union-find)) 10 100 conj! union!)))

  (quick-bench (dorun (make-stars 10 1000)))
  (quick-bench (partition-graph (union-find) 10 1000 conj union))
  (quick-bench (persistent! (partition-graph (transient (union-find)) 10 1000 conj! union!)))
  )
