(ns jordanlewis.data.union-find-test
  (:use clojure.test
        jordanlewis.data.union-find))

(deftest test-union-find
  (testing "Singleton sets are their own leaders."
    (is (= 1 (second (get-canonical (union-find 1) 1)))))
  (testing "Singleton sets unioned with themselves are still their own leaders."
    (is (= 1 (second (get-canonical (connect (union-find 1) 1 1) 1)))))
  (testing "Connected singletons have the same leader."
    (let [set (-> (union-find 1 2)
                  (connect 1 2))
          [set a] (get-canonical set 1)
          [set b] (get-canonical set 2)]
      (is (= a b)))))
