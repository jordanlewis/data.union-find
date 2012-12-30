(ns
  ^{:doc "Persistent disjoint set forests using Tarjan's union-find algorithm."
    :author "Jordan Lewis"}
  jordanlewis.data.union-find)

(defprotocol DisjointSetForest
  "A data structure that maintains information on a number of disjoint sets."
  (union [this x y] "Union two sets. Return a new disjoint set forest with the
sets that x and y belong to unioned.")
  (get-canonical [this x] "Get the canonical element of an element. Return a
vector of two elements: a new disjoint set forest that may have been modified
due to the path compression optimization, and the canonical element of the input, or
nil if no such element exists in the forest."))

(defrecord ^:private UFNode [value rank parent])

(declare empty-union-find)

(deftype PersistentDSF [elt-map num-sets _meta]
  Object
  ;; prints out a map from canonical element to elements unioned to that element.
  (toString [this] (str (group-by this (keys elt-map))))

  clojure.lang.IPersistentCollection
  ;; count returns the number of disjoint sets, not the number of total elements
  (count [this] (num-sets))
  ;; cons adds the input to a new singleton set
  (cons [this x]
    (if (elt-map x)
      this
      (PersistentDSF. (assoc elt-map x (->UFNode x 0 nil)) (inc num-sets) _meta)))
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
  (withMeta [this meta] (PersistentDSF. elt-map num-sets meta))

  DisjointSetForest
  (get-canonical [this x]
    (let [node (elt-map x)
          parent (:parent node)]
      (cond
        (= node nil) [this nil]
        (= parent nil) [this x]
        :else (let [[set canonical] (get-canonical this parent)
                    elt-map (.elt-map set)]
                [(PersistentDSF. (assoc-in elt-map [x :parent] canonical)
                                   num-sets _meta)
                 canonical]))))
  (union [this x y]
    (let [[newset x-root] (get-canonical this x)
          [newset y-root] (get-canonical newset y)
          ;; update elt-map to be the new one after get-canonical potentially changes it
          elt-map (.elt-map newset)
          x-rank (:rank (elt-map x-root))
          y-rank (:rank (elt-map y-root))
          new-num-sets (inc num-sets)]
      (cond (or (nil? x-root) (nil? y-root)) newset
            (= x-root y-root) newset
            (< x-rank y-rank) (PersistentDSF.
                                (assoc-in elt-map [x-root :parent] y-root)
                                new-num-sets _meta)
            (< y-rank x-rank) (PersistentDSF.
                                (assoc-in elt-map [y-root :parent] x-root)
                                new-num-sets _meta)
            :else (PersistentDSF.
                    (-> elt-map
                      (transient)
                      (assoc! y-root (assoc (elt-map y-root) :parent x-root))
                      (assoc! x-root (assoc (elt-map x-root) :rank (inc x-rank)))
                      (persistent!))
                    new-num-sets _meta)))))

(def ^:private empty-union-find (->PersistentDSF {} 0 {}))

(defn union-find
  "Returns a new union-find data structure with provided elements as singletons."
  [& xs]
  (reduce conj empty-union-find xs))
