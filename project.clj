(defproject org.jordanlewis/data.union-find "0.1.1"
            :description "Persistent disjoint-set forests using Tarjan's union-find algorithm."
            :url "http://github.com/jordanlewis/data.union-find"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.11.1"]]

            :profiles
            {:dev {:source-paths      ["src" "dev"]
                   :dependencies [[criterium "0.4.6"]]}})
