(ns jordanlewis.data.union-find-test
  (:use clojure.test
        jordanlewis.data.union-find))

(deftest test-union-find
  (let [set (-> (union-find 1 2 3 4 5 6)
                (connect 1 2)
                (connect 3 4)
                (connect 4 5))]
    (testing "Singleton sets are their own leaders."
      (is (= 6 (second (get-canonical set 6)))))
    (testing "Singleton sets unioned with themselves are still their own leaders."
      (is (= 6 (second (get-canonical (connect set 6 6) 6)))))
    (testing "Connected singletons have the same leader."
      (let [[set a] (get-canonical set 1)
            [set b] (get-canonical set 2)
            [set c] (get-canonical set 3)
            [set d] (get-canonical set 4)
            [set e] (get-canonical set 5)]
        (is (= a b))
        (is (= c d))
        (is (= c e))
        (is (not= b c))
        (let [set (connect set 2 3)
              [set a] (get-canonical set 1)
              [set c] (get-canonical set 3)]
          (is (= a c 1)))))
    (testing "Count counts the number of connected components."
      (is (= 3 (count set))))
    (testing "Conj adds new singletons"
      (let [set (conj set 7)]
        (is (= 4 (count set)))
        (is (= 7 (set 7)))
        (is (= 6 ((connect set 6 7) 7)))))

    (testing "union-find is gettable"
      (is (= 1 (get set 2)))
      (is (= 1 (get set 1)))
      (is (= nil (get set 10)))
      (is (= :not-found (get set 10 :not-found))))

    (testing "union-find is a function"
      (is (= 1 (set 2)))
      (is (= 1 (set 1)))
      (is (= nil (set 10)))
      (is (= :not-found (set 10 :not-found))))

    (testing "supports meta"
      (is (= {:with :meta} (meta (with-meta set {:with :meta})))))

    (testing "More map equalities")))
