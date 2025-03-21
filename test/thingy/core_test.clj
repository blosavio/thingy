(ns thingy.core-test
  "Note: `$ lein test :all` at the command line evaluates tests in parallel.
  During those evaluations, there may be contention for setting the `options`
  atom. Therefore, each unit test that calls an altered method --- .toString()
  and .invoke() --- needs to be run in a locking context.

  When each test namespace is evaluated individually, all tests run in sequence,
  isolated from one another."
  (:require [clojure.test :refer [deftest is are testing run-tests]]
            [thingy.core :refer :all]))


(defn foo-test
  {:UUIDv4 #uuid "618a3284-abac-4771-90ed-8aa273351120"}
  [a b c]
  (concat a b c))


(defn test-invoke-while-locked
  "Evaluates the `.invoke()` instance method of AlternateFunctionInvokePersistentVector
  `v` in a locking context."
  {:UUIDv4 #uuid "26e9ae56-6ef6-4d70-b4fc-2f979220e0da"}
  [v1 v2 v3]
  (let [lock (Object.)]
    (locking lock
      (assign-thingy-fn! foo-test)
      (v1 v2 v3))))


(deftest make-thingy-tests
  (testing "basic creation"
    (are [x y] (= x y)
      [] (make-thingy)
      [:a :b :c] (make-thingy :a :b :c)))
  (testing "thingy invocation"
    (let [X (make-thingy :a :b)
          Y (make-thingy :c :d)
          Z (make-thingy :e :f)]
      (are [x y] (= x y)
        (test-invoke-while-locked X Y Z) [:a :b :c :d :e :f]
        (test-invoke-while-locked Y Z X) [:c :d :e :f :a :b]
        (test-invoke-while-locked Z X Y) [:e :f :a :b :c :d]))))


#_(run-tests)