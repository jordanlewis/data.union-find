(ns jordanlewis.data.union-find-test
  (:use clojure.test
        jordanlewis.data.union-find))

(deftest test-union-find
  (testing "Singleton sets are their own leaders."
    (is (= 1 (second (get-canonical (union-find 1) 1)))))
  (testing "Singleton sets unioned with themselves are still their own leaders."
    (is (= 1 (second (get-canonical (connect (union-find 1) 1 1) 1)))))
  (testing "Connected singletons have the same leader."
    (let [set (-> (union-find 1 2 3 4 5 6)
                  (connect 1 2)
                  (connect 3 4))
          [set a] (get-canonical set 1)
          [set b] (get-canonical set 2)
          [set c] (get-canonical set 3)
          [set d] (get-canonical set 4)]
      (is (= a b))
      (is (= c d))
      (is (not= b c))
      (let [set (connect set 2 3)
            [set a] (get-canonical set 1)
            [set c] (get-canonical set 3)]
        (is (= a c 1)))))
  (testing "Count counts the number of elts"
    (is (= (count (union-find 1 2 3 4)) 4)))

  (testing "union-find as function"
    (let [set (-> (union-find 1 2)
                  (connect 1 2))]
      (is (= 1 (set 2)))
      (is (= 1 (set 1)))
      (is (= nil (set 3)))
      (is (= :not-found (set 3 :not-found)))))

  (testing "More map equalities"))
