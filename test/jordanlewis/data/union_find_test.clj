(ns jordanlewis.data.union-find-test
  (:use clojure.test
        jordanlewis.data.union-find))

(deftest singletons
  (testing "Singleton sets are their own leaders."
    (is (= 1 (second (get-canonical (union-find 1) 1))))))

