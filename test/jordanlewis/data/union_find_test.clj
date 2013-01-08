(ns jordanlewis.data.union-find-test
  (:use clojure.test
        jordanlewis.data.union-find))

(deftest test-union-find
  (let [set (-> (union-find 1 2 3 4 5 6)
                (union 1 2)
                (union 3 4)
                (union 4 5))]
    (testing "Missing elements have nil leaders."
      (is (= [set nil] (get-canonical set 10))))
    (testing "Singleton sets are their own leaders."
      (is (= 6 (second (get-canonical set 6)))))
    (testing "Singleton sets unioned with themselves are still their own leaders."
      (is (= 6 (second (get-canonical (union set 6 6) 6)))))
    (testing "Unioning from both sides of size works as expected"
      (let [set (union set 1 3)
            set-left  (union set 1 4)
            set-right (union set 4 1)]
        (is (= 1 (set-left  1)))
        (is (= 1 (set-right 1)))))
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
        (let [set (union set 2 3)
              [set a] (get-canonical set 1)
              [set c] (get-canonical set 3)]
          (is (= a c 1)))))
    (testing "Seq returns only leader elements"
      (is (= 3 (count (seq set)))))
    (testing "Count counts the number of connected components."
      (is (= 3 (count set)))
      (is (= 2 (count (union set 1 3)))))
    (testing "Conj adds new singletons"
      (let [set (conj set 7)]
        (is (= 4 (count set)))
        (is (= 3 (count (union set 6 7))))
        (is (= 7 (set 7)))
        (is (= 6 ((union set 6 7) 7)))))

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

    (testing "equality works right"
      (is (= set set))
      (is (not= set (conj set 8)))
      (is (= (union set 5 6) (union set 6 5))))

    (testing "unioning a missing element is a no-op."
      (is (= set (union set 5 10))))))
