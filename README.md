# data.union-find

A Clojure implementation of the Union-Find data structure.

## Usage

Make a new union-find data structure containing its arguments as singleton sets:

    user=> (def uf (union-find 1 2 3 4 5))

Union two sets:

    user=> (connect uf 2 3)
    {2 [3 2]}

Getting the canonical element of a set can change the internals of the data structure,
due to an optimization called path compression. Therefore, get-canonical returns two
objects: the updated data structure, and the requested canonical element. Get
the canonical element of an element:

    user=> (get-canonical uf 3)
    [{2 [3 2]} 2]


## License

Copyright © 2012 Jordan Lewis

Distributed under the Eclipse Public License, the same as Clojure.
