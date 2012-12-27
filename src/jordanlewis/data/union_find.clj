(ns jordanlewis.data.union-find)

(defprotocol DisjointSet
  "A data structure that maintains informations on a number of disjoint sets."
  (add-singleton [this x] "Add the element x to a new singleton set")
  (connect [this x y] "Union the sets that x and y are in")
  (get-canonical [this x] "Return the canonical element of the set x is in"))

(defrecord UFNode [value rank parent])

(defrecord PersistentUFSet [elt-map]
  DisjointSet
  (add-singleton [this x]
    (assoc-in this [:elt-map x] (->UFNode x 0 nil)))
  (get-canonical [this x]
    (let [parent (:parent (elt-map x))]
      (if (= parent nil) [this x]
        (let [set (get-canonical this parent)]
          (assoc-in set [0 :elt-map x :parent] (second set))))))
  (connect [this x y]
    (let [[x-set x-root] (get-canonical this x)
          [y-set y-root] (get-canonical x-set y)
          ;; update elt-map to be the new one after get-canonical potentially changes it
          elt-map (:elt-map y-set)
          x-rank (:rank (elt-map x-root))
          y-rank (:rank (elt-map y-root))]
      (if (= x-root y-root) y-set
        (cond (< x-rank y-rank) (assoc-in y-set [:elt-map x-root :parent] y-root)
              (< y-rank x-rank) (assoc-in y-set [:elt-map y-root :parent] x-root)
              :else (-> y-set
                      (assoc-in [:elt-map y-root :parent] x-root)
                      (assoc-in [:elt-map x-root :rank] (inc x-rank)))))))
  ;;clojure.lang.IPersistentCollection
  ;;(count [this] (count elt-map))
  ;;(cons [this e] (add-singleton this e))
  ;;(empty [this] (make-persistent-set))
  ;;(equiv [this o] (.equiv elt-map o))
  ;;(hashCode [this] (.hashCode elt-map))
  ;;(equals [this o] (or (identical? this o) (.equals elt-map o)))
  )

(defn make-persistent-set []
  (->PersistentUFSet {}))
