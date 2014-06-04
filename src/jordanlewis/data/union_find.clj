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

(defprotocol TransientDisjointSetForest
  "A data structure that maintains information on a number of disjoint sets."
  (union! [this x y] "Union two sets. Return a mutated disjoint set forest with the
sets that x and y belong to unioned."))

(defrecord ^:private UFNode [value rank parent])

(declare empty-union-find)

(declare ->TransientDSF)

(deftype PersistentDSF [elt-map num-sets _meta]
  Object
  ;; prints out a map from canonical element to elements unioned to that element.
  (toString [this] (str (group-by this (keys elt-map))))
  (hashCode [this] (.hashCode elt-map))
  (equals [this that] (or (identical? this that) (.equals elt-map (.elt-map that))))

  clojure.lang.Seqable
  ;; seq returns each of the canonical elements, not all of the elements
  (seq [this]
    (map first (filter (comp nil? :parent val) elt-map)))

  clojure.lang.IPersistentCollection
  ;; cons adds the input to a new singleton set
  (cons [this x]
    (if (elt-map x)
      this
      (PersistentDSF. (assoc elt-map x (->UFNode x 0 nil)) (inc num-sets) _meta)))
  (empty [this] empty-union-find)
  (equiv [this that] (.equals this that))

  clojure.lang.IEditableCollection
  (asTransient [this]
    (->TransientDSF
     (transient elt-map) num-sets _meta))

  ;; count returns the number of disjoint sets, not the number of total elements
  clojure.lang.Counted
  (count [this] num-sets)

  clojure.lang.ILookup
  ;; valAt gets the canonical element of the key without path compression
  (valAt [this k] (.valAt this k nil))
  (valAt [this k not-found]
    (loop [x k]
      (if-let [node (elt-map x)]
        (if-let [parent (:parent node)]
          (recur parent)
          x)
        not-found)))

  clojure.lang.IFn
  ;; invoking as function behaves like valAt.
  (invoke [this k] (.valAt this k))
  (invoke [this k not-found] (.valAt this k not-found))

  ;; implementing IMeta and IObj gives us meta
  clojure.lang.IMeta
  (meta [this] _meta)
  clojure.lang.IObj
  (withMeta [this meta] (PersistentDSF. elt-map num-sets meta))

  DisjointSetForest
  (get-canonical [this x]
    (let [node (elt-map x)
          parent (:parent node)]
      (cond
        (= node nil) [this nil]
        (= parent nil) [this x]
        ;; path compression. set the parent of each node on the path we take
        ;; to the root that we find.
        :else (let [[set canonical] (get-canonical this parent)
                    elt-map (.elt-map set)]
                [(PersistentDSF. (assoc-in elt-map [x :parent] canonical)
                                 num-sets _meta)
                 canonical]))))
  (union [this x y]
    (let [[newset x-root] (get-canonical this x)
          [newset y-root] (get-canonical newset y)
          ;; update elt-map to be the new one after get-canonical potentially
          ;; changes it, and decrement num-sets since 2 sets are joining
          elt-map (.elt-map newset)
          num-sets (dec num-sets)
          x-rank (:rank (elt-map x-root))
          y-rank (:rank (elt-map y-root))]
      (cond (or (nil? x-root) ;; no-op - either the input doesn't exist in the
                (nil? y-root) ;; universe, or the two inputs are already unioned
                (= x-root y-root)) newset
            (< x-rank y-rank) (PersistentDSF.
                                (assoc-in elt-map [x-root :parent] y-root)
                                num-sets _meta)
            (< y-rank x-rank) (PersistentDSF.
                                (assoc-in elt-map [y-root :parent] x-root)
                                num-sets _meta)
            :else (PersistentDSF.
                    (-> elt-map
                      (transient)
                      (assoc! y-root (assoc (elt-map y-root) :parent x-root))
                      (assoc! x-root (assoc (elt-map x-root) :rank (inc x-rank)))
                      (persistent!))
                    num-sets _meta))))


  )

(deftype TransientDSF [elt-map
                       ^:unsynchronized-mutable num-sets
                       meta]
  Object
  ;; prints out a map from canonical element to elements unioned to that element.
  (toString [this] (str (group-by this (keys elt-map))))

  clojure.lang.Counted
  (count [this] num-sets)

  clojure.lang.ILookup
  ;; valAt gets the canonical element of the key without path compression
  (valAt [this k] (.valAt this k nil))
  (valAt [this k not-found]
    (loop [x k]
      (if-let [node (elt-map x)]
        (if-let [parent (:parent node)]
          (recur parent)
          x)
        not-found)))

  clojure.lang.IFn
  ;; invoking as function behaves like valAt.
  (invoke [this k] (.valAt this k))
  (invoke [this k not-found] (.valAt this k not-found))

  clojure.lang.ITransientCollection
  (conj [this x]
    (if (elt-map x)
      this
      (do (assoc! elt-map x (->UFNode x 0 nil))
          (set! num-sets (inc num-sets))
          this)
      ))

  (persistent [this]
    (PersistentDSF. (persistent! elt-map) num-sets meta))

  DisjointSetForest
  (get-canonical [this x]
    (let [node (elt-map x)
          parent (:parent node)]
      (cond
       (= node nil) [this nil]
       (= parent nil) [this x]
       ;; path compression. set the parent of each node on the path we take
       ;; to the root that we find.
       :else (let [[_ canonical] (get-canonical this parent)
                   ]
               (do (assoc! elt-map x (assoc (get elt-map x) :parent canonical))
                   [this canonical])))))
  (union [this x y]
    (throw (java.lang.UnsupportedOperationException "Use union! on transients")))

  TransientDisjointSetForest
  (union! [this x y]
    (let [[_ x-root] (get-canonical this x)
          [_ y-root] (get-canonical this y)
          x-rank (:rank (elt-map x-root))
          y-rank (:rank (elt-map y-root))]
      (cond (or (nil? x-root) ;; no-op - either the input doesn't exist in the
                (nil? y-root) ;; universe, or the two inputs are already unioned
                (= x-root y-root)) this
            (< x-rank y-rank)
            (do
              (assoc! elt-map x-root (assoc (get elt-map x-root) :parent y-root))
              (set! num-sets (- num-sets 1))
              this)
            (< y-rank x-rank)
            (do
              (assoc! elt-map y-root (assoc (get elt-map y-root) :parent x-root))
              (set! num-sets (- num-sets 1))
              this)
            :else
            (do
              (assoc! elt-map y-root (assoc (elt-map y-root) :parent x-root))
              (assoc! elt-map x-root (assoc (elt-map x-root) :rank (inc x-rank)))
              (set! num-sets (- num-sets 1))
              this)))))

(def ^:private empty-union-find (->PersistentDSF {} 0 {}))

(defn union-find
  "Returns a new union-find data structure with provided elements as singletons."
  [& xs]
  (reduce conj empty-union-find xs))
