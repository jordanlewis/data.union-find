(ns jordanlewis.data.union-find)

(defprotocol DisjointSet
  "A data structure that maintains informations on a number of disjoint sets."
  (connect [this x y] "Union the sets that x and y are in")
  (get-canonical [this x] "Return the canonical element of the set x is in"))

(defrecord UFNode [value rank parent])

(declare empty-union-find)

(deftype PersistentUFSet [elt-map num-sets]
  Object
  (toString [this] (str (group-by this (keys elt-map))))

  clojure.lang.IPersistentCollection
  (count [this] (num-sets))
  (cons [this x]
    (if (elt-map x)
      this
      (PersistentUFSet. (assoc elt-map x (->UFNode x 0 nil)) (inc num-sets))))
  (empty [this] empty-union-find)
  (equiv [this that] (.equals this that))
  (hashCode [this] (.hashCode elt-map))
  (equals [this that] (or (identical? this that) (.equals elt-map that)))
  (seq [this]
    (seq (filter #(nil? (:parent (second %))) elt-map)))

  clojure.lang.IFn
  (invoke [this k] (second (get-canonical this k)))
  (invoke [this k not-found]
    (let [ret (get-canonical this k)]
      (if (nil? ret) not-found
        (second ret))))

  DisjointSet
  (get-canonical [this x]
    (let [node (elt-map x)
          parent (:parent node)]
      (cond
        (= node nil) nil
        (= parent nil) [this x]
        :else (let [[set canonical] (get-canonical this parent)
                    elt-map (.elt-map set)]
                [(PersistentUFSet. (assoc-in elt-map [x :parent] canonical) num-sets)
                 canonical]))))
  (connect [this x y]
    (let [[this x-root] (get-canonical this x)
          [this y-root] (get-canonical this y)
          ;; update elt-map to be the new one after get-canonical potentially changes it
          elt-map (.elt-map this)
          x-rank (:rank (elt-map x-root))
          y-rank (:rank (elt-map y-root))
          new-num-sets (inc num-sets)]
      (cond (= x-root y-root) this
            (< x-rank y-rank) (PersistentUFSet.
                                (assoc-in elt-map [x-root :parent] y-root) new-num-sets)
            (< y-rank x-rank) (PersistentUFSet.
                                (assoc-in elt-map [y-root :parent] x-root) new-num-sets)
            :else (PersistentUFSet.
                    (-> elt-map
                      (transient)
                      (assoc! y-root (assoc (elt-map y-root) :parent x-root))
                      (assoc! x-root (assoc (elt-map x-root) :rank (inc x-rank)))
                      (persistent!))
                    new-num-sets)))))

(def ^:private empty-union-find (->PersistentUFSet {} 0))

(defn union-find
  "Returns a new union-find data structure with provided elements as singletons"
  [& xs]
  (reduce conj empty-union-find xs))
