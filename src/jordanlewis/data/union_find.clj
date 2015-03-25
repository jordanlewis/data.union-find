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

(defprotocol IUFNode
  (value [n])
  (rank [n])
  (parent [n]))

(defprotocol MutateUFNode
  (set-rank! [n r])
  (set-parent! [n p]))

(deftype ^:private UFNode [v r p]
  IUFNode
  (value [_] v)
  (rank [_] r)
  (parent [_] p)

  Object
  (equals [_ o]
    (boolean (and o
                  (= v (value o))
                  (= r (rank o))
                  (= p (parent o))))))

(deftype ^:private MutableUFNode [v ^:unsynchronized-mutable r ^:unsynchronized-mutable p]
  IUFNode
  (value [_] v)
  (rank [_] r)
  (parent [_] p)

  MutateUFNode
  (set-rank! [n rank]
    (set! r rank))
  (set-parent! [n parent]
    (set! p parent))

  Object
  (equals [_ o]
    (boolean (and o
                  (= v (value o))
                  (= r (rank o))
                  (= p (parent o))))))

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
    (map first (filter (comp nil? parent val) elt-map)))

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
        (if-let [parent (parent node)]
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
          parent (when node (parent node))]
      (cond
        (= node nil) [this nil]
        (= parent nil) [this x]
        ;; path compression. set the parent of each node on the path we take
        ;; to the root that we find.
        :else (let [[set canonical] (get-canonical this parent)
                    elt-map (.elt-map set)]
                [(PersistentDSF. (update-in elt-map [x] #(->UFNode (value %) (rank %) canonical))
                                 num-sets _meta)
                 canonical]))))
  (union [this x y]
    (let [[newset x-root] (get-canonical this x)
          [newset y-root] (get-canonical newset y)
          ;; update elt-map to be the new one after get-canonical potentially
          ;; changes it, and decrement num-sets since 2 sets are joining
          elt-map (.elt-map newset)
          num-sets (dec num-sets)
          x-node (elt-map x-root)
          y-node (elt-map y-root)
          x-rank (when x-node (rank x-node))
          y-rank (when y-node (rank y-node))]
      (cond (or (nil? x-root) ;; no-op - either the input doesn't exist in the
                (nil? y-root) ;; universe, or the two inputs are already unioned
                (= x-root y-root)) newset
            (< x-rank y-rank) (PersistentDSF.
                                (update-in elt-map [x-root] #(->UFNode (value %) (rank %) y-root))
                                num-sets _meta)
            (< y-rank x-rank) (PersistentDSF.
                                (update-in elt-map [y-root] #(->UFNode (value %) (rank %) x-root))
                                num-sets _meta)
            :else (let []
                    (PersistentDSF.
                      (-> elt-map
                          (transient)
                          (assoc! y-root (->UFNode (value y-node) (rank y-node) x-root))
                          (assoc! x-root (->UFNode (value x-node) (inc x-rank) (parent x-node)))
                          (persistent!))
                      num-sets _meta)))))


  )

(deftype TransientDSF [^:unsynchronized-mutable elt-map
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
        (if-let [parent (parent node)]
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
      (do (set! elt-map (assoc! elt-map x (->MutableUFNode x 0 nil)))
          (set! num-sets (inc num-sets))
          this)
      ))

  (persistent [this]
    (PersistentDSF. (persistent! elt-map) num-sets meta))

  DisjointSetForest
  (get-canonical [this x]
    (let [node (elt-map x)
          parent (when node (parent node))]
      (cond
       (= node nil) [this nil]
       (= parent nil) [this x]
       ;; path compression. set the parent of each node on the path we take
       ;; to the root that we find.
       :else (let [[_ canonical] (get-canonical this parent)]
               (do (if (satisfies? MutateUFNode node)
                     (set-parent! node canonical)
                     (set! elt-map (assoc! elt-map x (->MutableUFNode (value node) (rank node) canonical))))
                   [this canonical])))))
  (union [this x y]
    (throw (java.lang.UnsupportedOperationException "Use union! on transients")))

  TransientDisjointSetForest
  (union! [this x y]
    (let [[_ x-root] (get-canonical this x)
          [_ y-root] (get-canonical this y)
          x-node (elt-map x-root)
          y-node (elt-map y-root)
          x-rank (when x-node (rank x-node))
          y-rank (when y-node (rank y-node))]
      (cond (or (nil? x-root) ;; no-op - either the input doesn't exist in the
                (nil? y-root) ;; universe, or the two inputs are already unioned
                (= x-root y-root)) this
            (< x-rank y-rank)
            (do
              (if (satisfies? MutateUFNode x-node)
                (set-parent! x-node y-root)
                (set! elt-map (assoc! elt-map x-root (->MutableUFNode (value x-node) (rank x-node) y-root))))
              (set! num-sets (dec num-sets))
              this)
            (< y-rank x-rank)
            (do
              (if (satisfies? MutateUFNode y-node)
                (set-parent! y-node x-root)
                (set! elt-map (assoc! elt-map y-root (->MutableUFNode (value y-node) (rank y-node) x-root))))
              (set! num-sets (dec num-sets))
              this)
            :else
            (do
              (if (satisfies? MutateUFNode y-node)
                (set-parent! y-node x-root)
                (set! elt-map (assoc! elt-map y-root (->MutableUFNode (value y-node) (rank y-node) x-root))))
              (if (satisfies? MutateUFNode x-node)
                (set-rank! x-node (inc x-rank))
                (set! elt-map (assoc! elt-map x-root (->MutableUFNode (value x-node) (inc x-rank) (parent x-node)))))
              (set! num-sets (dec num-sets))
              this)))))

(def ^:private empty-union-find (->PersistentDSF {} 0 {}))

(defn union-find
  "Returns a new union-find data structure with provided elements as singletons."
  [& xs]
  (reduce conj empty-union-find xs))
