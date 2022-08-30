# data.union-find

A Clojure implementation of persistent disjoint-set forests using Tarjan's
union-find algorithm.

    com.github.jordanlewis/data.union-find {:git/tag "1.0.0" :sha "0e8a06f"}
    
This is not currently released on Clojars. If someone needs that, please put in
a ticket and it can be done.

This library is stable and I do not currently anticipate any changes.

## Usage

Make a new union-find data structure containing its arguments as singleton sets:

    (use 'jordanlewis.data.union-find)
    (def uf (union-find 1 2 3 4 5))
    uf
    ;; => {5 [5], 4 [4], 3 [3], 2 [2], 1 [1]}

Add a new element as a singleton set with conj or cons:

    (conj uf 8)
    ;; => {8 [8], 5 [5], 4 [4], 2 [3 2], 1 [1]}

Union two sets:

    (def uf (union uf 2 3))
    uf
    ;; => {5 [5], 4 [4], 2 [3 2], 1 [1]}

Look up the canonical element for an element:

    (get-canonical uf 3)
    ;; => [{5 [5], 4 [4], 2 [3 2], 1 [1]} 2]
    
Union find also supports being used as a transient editable collection, which
can improve performance in some scenarios:

    (-> (transient uf)
        (union! 2 5)
        persistent!)

Getting the canonical element of a set can change the internals of the data structure,
due to an optimization called path compression. Therefore, get-canonical returns two
objects: the updated data structure, and the requested canonical element.

Getting the count of a union-find data structure returns the number of connected
components, not the number of elements. count is a constant-time operation.

    (count uf) ;; => 4
    ;; 4 connected components, but 5 elements

Treating a union-find data structure as a seq similiarly returns only the
canonical elements of the data structure, not all of the elements:

    (seq uf) ;; => (5 4 2 1) 
    ;; doesn't include 3, which is a non-canonical element

union-find also implements ILookup and IFn as canonical element lookups, so you
can use get on it or apply it to an element like you would with a vector or a
map. Using it this way doesn't perform the path compression optimization, and
just returns the canonical element.

    (uf 3) ;; => 2
    (get uf 3) ;; => 2
    (uf 10) ;; => nil
    (uf 10 :not-found) ;; => :not-found


## License

Copyright © 2012-2022 Jordan Lewis and Darrick Wiebe

Distributed under the Eclipse Public License, the same as Clojure.
