(ns fn-thingy.core-test
  "Note: `$ lein test :all` at the command line evaluates tests in parallel.
  During those evaluations, there may be contention for setting the `options`
  atom. Therefore, each unit test that calls an altered method --- .toString()
  and .invoke() --- needs to be run in a locking context.

  When each test namespace is evaluated individually, all tests run in sequence,
  isolated from one another."
  (:require [clojure.test :refer [deftest is are testing run-tests]]
            [fn-thingy.core :refer :all]))


;; Note: Testing macroexpansion from lein requires precise namespacing. When
;; using reqular quote, not all the symbols are namespaced. That's okay when
;; running tests from within this namespace, but command line lein runs the
;; tests from some other namespace. Syntax quote inserts the namespace so that
;; the tests work properly from any namespace.

;; See https://github.com/technomancy/leiningen/issues/912


(deftest defn-thingy-macroexpansion
  (are [x y] (= x y)
    (macroexpand-1 `(defn-thingy foo
                      "doc-string"
                      {:metadata "val"}
                      [arg1 arg2 arg3]
                      (concat arg1 arg2 arg2)))

    `(do (clojure.core/defn fn-thingy.core-test/foo
           "doc-string" {:metadata "val"}
           [fn-thingy.core-test/arg1 fn-thingy.core-test/arg2 fn-thingy.core-test/arg3]
           (clojure.core/concat fn-thingy.core-test/arg1 fn-thingy.core-test/arg2 fn-thingy.core-test/arg2))
         (fn-thingy.dangerous-vector/reset!-options
          {:fn fn-thingy.core-test/foo,
           :right-delimiter "]",
           :left-delimiter "["}))))


(defn test-invoke-while-locked
  "Evaluates the `.invoke()` instance method of AlternateFunctionInvokePersistentVector
 `v` in a locking context."
  {:UUIDv4 #uuid "26e9ae56-6ef6-4d70-b4fc-2f979220e0da"}
  [v1 v2 v3]
  (let [lock (Object.)]
    (locking lock
      (defn-thingy foo-test
        "Foo docstring"
        {:added 1.2}
        [a b c]
        (concat a b c))
      (v1 v2 v3))))


(deftest make-fn-thingy-tests
  (testing "basic creation"
    (are [x y] (= x y)
      [] (make-fn-thingy)
      [:a :b :c] (make-fn-thingy :a :b :c)))
  (testing "fn-thingy invocation"
    (let [X (make-fn-thingy :a :b)
          Y (make-fn-thingy :c :d)
          Z (make-fn-thingy :e :f)]
      (are [x y] (= x y)
        (test-invoke-while-locked X Y Z) [:a :b :c :d :e :f]
        (test-invoke-while-locked Y Z X) [:c :d :e :f :a :b]
        (test-invoke-while-locked Z X Y) [:e :f :a :b :c :d]))))


#_(run-tests)