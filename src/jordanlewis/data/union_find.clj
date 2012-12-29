(ns jordanlewis.data.union-find)

(defprotocol DisjointSet
  "A data structure that maintains informations on a number of disjoint sets."
  (connect [this x y] "Union the sets that x and y are in")
  (get-canonical [this x] "Return the canonical element of the set x is in"))

(defrecord UFNode [value rank parent])

(declare empty-union-find)

(deftype PersistentUFSet [elt-map num-sets _meta]
  Object
  ;; prints out a map from canonical element to elements connected to that element.
  (toString [this] (str (group-by this (keys elt-map))))

  clojure.lang.IPersistentCollection
  ;; count returns the number of disjoint sets, not the number of total elements
  (count [this] (num-sets))
  ;; cons adds the input to a new singleton set
  (cons [this x]
    (if (elt-map x)
      this
      (PersistentUFSet. (assoc elt-map x (->UFNode x 0 nil)) (inc num-sets) _meta)))
  (empty [this] empty-union-find)
  (equiv [this that] (.equals this that))
  (hashCode [this] (.hashCode elt-map))
  (equals [this that] (or (identical? this that) (.equals elt-map (.elt-map that))))
  ;; seq returns each of the canonical elements, not all of the elements
  (seq [this]
    (seq (filter #(nil? (:parent (second %))) elt-map)))

  clojure.lang.ILookup
  ;; valAt gets the canonical element of the key without path compression
  ;; TODO rewrite to be tail recursive, don't need to waste stack space remembering
  ;; the path compressions since we're going to throw them away anyway.
  (valAt [this k] (second (get-canonical this k)))
  (valAt [this k not-found]
    (let [[newset ret] (get-canonical this k)]
      (if (nil? ret) not-found ret)))

  clojure.lang.IFn
  ;; invoking as function behaves like valAt.
  (invoke [this k] (.valAt this k))
  (invoke [this k not-found] (.valAt this k not-found))

  clojure.lang.IObj
  ;; implementing IObj gives us meta
  (meta [this] _meta)
  (withMeta [this meta] (PersistentUFSet. elt-map num-sets meta))

  DisjointSet
  (get-canonical [this x]
    (let [node (elt-map x)
          parent (:parent node)]
      (cond
        (= node nil) [this nil]
        (= parent nil) [this x]
        :else (let [[set canonical] (get-canonical this parent)
                    elt-map (.elt-map set)]
                [(PersistentUFSet. (assoc-in elt-map [x :parent] canonical)
                                   num-sets _meta)
                 canonical]))))
  (connect [this x y]
    (let [[newset x-root] (get-canonical this x)
          [newset y-root] (get-canonical newset y)
          ;; update elt-map to be the new one after get-canonical potentially changes it
          elt-map (.elt-map newset)
          x-rank (:rank (elt-map x-root))
          y-rank (:rank (elt-map y-root))
          new-num-sets (inc num-sets)]
      (cond (or (nil? x-root) (nil? y-root)) newset
            (= x-root y-root) newset
            (< x-rank y-rank) (PersistentUFSet.
                                (assoc-in elt-map [x-root :parent] y-root)
                                new-num-sets _meta)
            (< y-rank x-rank) (PersistentUFSet.
                                (assoc-in elt-map [y-root :parent] x-root)
                                new-num-sets _meta)
            :else (PersistentUFSet.
                    (-> elt-map
                      (transient)
                      (assoc! y-root (assoc (elt-map y-root) :parent x-root))
                      (assoc! x-root (assoc (elt-map x-root) :rank (inc x-rank)))
                      (persistent!))
                    new-num-sets _meta)))))

(def ^:private empty-union-find (->PersistentUFSet {} 0 {}))

(defn union-find
  "Returns a new union-find data structure with provided elements as singletons"
  [& xs]
  (reduce conj empty-union-find xs))
