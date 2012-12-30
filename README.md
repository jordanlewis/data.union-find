# data.union-find

A Clojure implementation of persistent disjoint-set forests using Tarjan's
union-find algorithm.

## Usage

Make a new union-find data structure containing its arguments as singleton sets:

    user=> (def uf (union-find 1 2 3 4 5))
    user=> uf
    {5 [5], 4 [4], 3 [3], 2 [2], 1 [1]}

Add a new element as a singleton set with conj or cons:

    user=> (conj uf 8)
    {8 [8], 5 [5], 4 [4], 2 [3 2], 1 [1]}

Union two sets:

    user=> (def uf (union uf 2 3))
    user=> uf
    {5 [5], 4 [4], 2 [3 2], 1 [1]}

Look up the canonical element for an element:

    user=> (get-canonical uf 3)
    [{5 [5], 4 [4], 2 [3 2], 1 [1]} 2]

Getting the canonical element of a set can change the internals of the data structure,
due to an optimization called path compression. Therefore, get-canonical returns two
objects: the updated data structure, and the requested canonical element.

union-find also implements ILookup and IFn as canonical element lookups, so you
can use get on it or apply it to an element like you would with a vector or a
map. Using it this way doesn't perform the path compression optimization, and
just returns the canonical element.

    user=> (uf 3)
    2
    user=> (get uf 3)
    2
    user=> (uf 10)
    nil
    user=> (uf 10 :not-found)
    :not-found


## License

Copyright © 2012 Jordan Lewis

Distributed under the Eclipse Public License, the same as Clojure.
