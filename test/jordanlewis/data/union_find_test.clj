(ns jordanlewis.data.union-find-test
  (:use clojure.test
        jordanlewis.data.union-find))

(deftest test-union-find
  (testing "Singleton sets are their own leaders."
    (is (= 1 (second (get-canonical (union-find 1) 1)))))
  (testing "Singleton sets unioned with themselves are still their own leaders."
    (is (= 1 (second (get-canonical (connect (union-find 1) 1 1) 1))))))

