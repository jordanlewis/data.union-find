(ns dev-utils)

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

