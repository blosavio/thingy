(ns thingy.dangerous-vector-tests
  "Tests in SECTION ONE sourced from Clojure's vector test suite [clojure.test-clojure.vectors](https://github.com/clojure/clojure/blob/clojure-1.12.0/test/clojure/test_clojure/vectors.clj),
  version 1.12.0, retrieved on 2025 March 03.

  Tests in SECTION TWO written by Brad Losavio.

  Note: `$ lein test :all` at the command line evaluates tests in parallel.
  During those evaluations, there may be contention for setting the `options`
  atom. Therefore, each unit test that calls an altered method --- .toString()
  and .invoke() --- needs to be run in a locking context.

  When each test namespace is evaluated individually, all tests run in sequence,
  isolated from one another."
  (:require
   [clojure.test :refer [are is deftest run-tests testing]]
   [thingy.dangerous-vector :refer :all])
  (:import
   [java.util Collection Spliterator]
   [java.util.function Consumer]
   [java.util.stream Collectors]))


(defaultize!-options)


(defmacro test-while-locked-to-default-options
  "Evaluate `body` while the `options` atom is locked to default values."
  {:UUIDv4 #uuid "1edc6450-73d2-420e-b45a-9cb0c5d0391a"}
  [body]
  `(locking (Object.)
     (defaultize!-options)
     ~body))


;;;; SECTION ONE

;;  Copyright (c) Rich Hickey. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

;; Author: Stuart Halloway, Daniel Solano Gómez


(deftest test-reversed-vec
  (let [r (range 6)
        v (into (alt-fn-vector) r)
        reversed (.rseq v)]
    (testing "returns the right impl"
      (is (= clojure.lang.APersistentVector$RSeq (class reversed))))
    (testing "RSeq methods"
      (is (= [5 4 3 2 1 0] reversed))
      (is (= 5 (.index reversed)))
      (is (= 5 (.first reversed)))
      (is (= [4 3 2 1 0] (.next reversed)))
      (is (= [3 2 1 0] (.. reversed next next)))
      (is (= 6 (.count reversed))))
    (testing "clojure calling through"
      (is (= 5 (first reversed)))
      (is (= 5 (nth reversed 0))))
    (testing "empty reverses to nil"
      (is (nil? (.. v empty rseq))))))


(deftest test-vecseq
  (let [r (range 100)
        vs (into (alt-fn-vector) r)
        vs-1 (next vs)
        vs-32 (.chunkedNext (seq vs))]
    (testing "="
      (are [a b] (= a b)
        vs vs
        vs-1 vs-1
        vs-32 vs-32)
      (are [a b] (not= a b)
        vs vs-1
        vs-1 vs
        vs vs-32
        vs-32 vs))
    (testing "IPersistentCollection.empty"
      (are [a] (identical? clojure.lang.PersistentList/EMPTY (.empty (seq a)))
        vs vs-1 vs-32))
    (testing "IPersistentCollection.cons"
      (are [result input] (= result (.cons input :foo))
        [:foo 1] (seq (into (vector-of :int) [1]))))
    (testing "IPersistentCollection.count"
      (are [ct s] (= ct (.count (seq s)))
        100 vs
        99 vs-1
        68 vs-32)
      ;; can't manufacture this scenario: ASeq defers to Counted, but
      ;; LazySeq doesn't, so Counted never gets checked on reified seq below
      #_(testing "hops to counted when available"
          (is (= 200
                 (.count (concat
                          (seq vs)
                          (reify clojure.lang.ISeq
                            (seq [this] this)
                            clojure.lang.Counted
                            (count [_] 100))))))))
    (testing "IPersistentCollection.equiv"
      (are [a b] (true? (.equiv a b))
        vs vs
        vs-1 vs-1
        vs-32 vs-32
        vs r)
      (are [a b] (false? (.equiv a b))
        vs vs-1
        vs-1 vs
        vs vs-32
        vs-32 vs
        vs nil))
    (testing "internal-reduce"
      (is (= [99] (into [] (drop 99 vs)))))))


(deftest test-primitive-subvector-reduce
  ;; regression test for CLJ-1082
  (is (== 60 (let [prim-vec (into (alt-fn-vector) (range 1000))]
               (reduce + (subvec prim-vec 10 15))))))


(deftest test-vec-compare
  (let [nums      (range 1 100)
        ;; randomly replaces a single item with the given value
        rand-replace  (fn[val]
                        (let [r (rand-int 99)]
                          (concat (take r nums) [val] (drop (inc r) nums))))
        ;; all num sequences in map
        num-seqs      {:standard       nums
                       :empty          '()
                       ;; different lengths
                       :longer         (concat nums [100])
                       :shorter        (drop-last nums)
                       ;; greater by value
                       :first-greater  (concat [100] (next nums))
                       :last-greater   (concat (drop-last nums) [100])
                       :rand-greater-1 (rand-replace 100)
                       :rand-greater-2 (rand-replace 100)
                       :rand-greater-3 (rand-replace 100)
                       ;; lesser by value
                       :first-lesser   (concat [0] (next nums))
                       :last-lesser    (concat (drop-last nums) [0])
                       :rand-lesser-1  (rand-replace 0)
                       :rand-lesser-2  (rand-replace 0)
                       :rand-lesser-3  (rand-replace 0)}
        ;; a way to create compare values based on num-seqs
        create-vals   (fn[base-val]
                        (zipmap (keys num-seqs)
                                (map #(into base-val %1) (vals num-seqs)))) ; Vecs made of int primitives
        int-vecs      (create-vals (alt-fn-vector))
        ;; Vecs made of long primitives
        long-vecs     (create-vals (alt-fn-vector))
        ;; standard boxing vectors
        regular-vecs  (create-vals [])
        ;; the standard int Vec for comparisons
        int-vec       (:standard int-vecs)]
    (testing "compare"
      (testing "identical"
        (is (= 0 (compare int-vec int-vec))))
      (testing "equivalent"
        (are [x y] (= 0 (compare x y))
          ;; standard
          int-vec (:standard long-vecs)
          (:standard long-vecs) int-vec
          int-vec (:standard regular-vecs)
          (:standard regular-vecs) int-vec
          ;; empty
          (:empty int-vecs) (:empty long-vecs)
          (:empty long-vecs) (:empty int-vecs)))
      (testing "lesser"
        (are [x] (= -1 (compare int-vec x))
          (:longer int-vecs)
          (:longer long-vecs)
          (:longer regular-vecs)
          (:first-greater int-vecs)
          (:first-greater long-vecs)
          (:first-greater regular-vecs)
          (:last-greater int-vecs)
          (:last-greater long-vecs)
          (:last-greater regular-vecs)
          (:rand-greater-1 int-vecs)
          (:rand-greater-1 long-vecs)
          (:rand-greater-1 regular-vecs)
          (:rand-greater-2 int-vecs)
          (:rand-greater-2 long-vecs)
          (:rand-greater-2 regular-vecs)
          (:rand-greater-3 int-vecs)
          (:rand-greater-3 long-vecs)
          (:rand-greater-3 regular-vecs))
        (are [x] (= -1 (compare x int-vec))
          nil
          (:empty int-vecs)
          (:empty long-vecs)
          (:empty regular-vecs)
          (:shorter int-vecs)
          (:shorter long-vecs)
          (:shorter regular-vecs)
          (:first-lesser int-vecs)
          (:first-lesser long-vecs)
          (:first-lesser regular-vecs)
          (:last-lesser int-vecs)
          (:last-lesser long-vecs)
          (:last-lesser regular-vecs)
          (:rand-lesser-1 int-vecs)
          (:rand-lesser-1 long-vecs)
          (:rand-lesser-1 regular-vecs)
          (:rand-lesser-2 int-vecs)
          (:rand-lesser-2 long-vecs)
          (:rand-lesser-2 regular-vecs)
          (:rand-lesser-3 int-vecs)
          (:rand-lesser-3 long-vecs)
          (:rand-lesser-3 regular-vecs)))
      (testing "greater"
        (are [x] (= 1 (compare int-vec x))
          nil
          (:empty int-vecs)
          (:empty long-vecs)
          (:empty regular-vecs)
          (:shorter int-vecs)
          (:shorter long-vecs)
          (:shorter regular-vecs)
          (:first-lesser int-vecs)
          (:first-lesser long-vecs)
          (:first-lesser regular-vecs)
          (:last-lesser int-vecs)
          (:last-lesser long-vecs)
          (:last-lesser regular-vecs)
          (:rand-lesser-1 int-vecs)
          (:rand-lesser-1 long-vecs)
          (:rand-lesser-1 regular-vecs)
          (:rand-lesser-2 int-vecs)
          (:rand-lesser-2 long-vecs)
          (:rand-lesser-2 regular-vecs)
          (:rand-lesser-3 int-vecs)
          (:rand-lesser-3 long-vecs)
          (:rand-lesser-3 regular-vecs))
        (are [x] (= 1 (compare x int-vec))
          (:longer int-vecs)
          (:longer long-vecs)
          (:longer regular-vecs)
          (:first-greater int-vecs)
          (:first-greater long-vecs)
          (:first-greater regular-vecs)
          (:last-greater int-vecs)
          (:last-greater long-vecs)
          (:last-greater regular-vecs)
          (:rand-greater-1 int-vecs)
          (:rand-greater-1 long-vecs)
          (:rand-greater-1 regular-vecs)
          (:rand-greater-2 int-vecs)
          (:rand-greater-2 long-vecs)
          (:rand-greater-2 regular-vecs)
          (:rand-greater-3 int-vecs)
          (:rand-greater-3 long-vecs)
          (:rand-greater-3 regular-vecs))))
    (testing "Comparable.compareTo"
      (testing "incompatible"
        (is (thrown? NullPointerException (.compareTo int-vec nil)))
        (are [x] (thrown? ClassCastException (.compareTo int-vec x))
          '()
          {}
          #{}
          (sorted-set)
          (sorted-map)
          nums
          1))
      (testing "identical"
        (is (= 0 (.compareTo int-vec int-vec))))
      (testing "equivalent"
        (are [x] (= 0 (.compareTo int-vec x))
          (:standard long-vecs)
          (:standard regular-vecs)))
      (testing "lesser"
        (are [x] (= -1 (.compareTo int-vec x))
          (:longer int-vecs)
          (:longer long-vecs)
          (:longer regular-vecs)
          (:first-greater int-vecs)
          (:first-greater long-vecs)
          (:first-greater regular-vecs)
          (:last-greater int-vecs)
          (:last-greater long-vecs)
          (:last-greater regular-vecs)
          (:rand-greater-1 int-vecs)
          (:rand-greater-1 long-vecs)
          (:rand-greater-1 regular-vecs)
          (:rand-greater-2 int-vecs)
          (:rand-greater-2 long-vecs)
          (:rand-greater-2 regular-vecs)
          (:rand-greater-3 int-vecs)
          (:rand-greater-3 long-vecs)
          (:rand-greater-3 regular-vecs)))
      (testing "greater"
        (are [x] (= 1 (.compareTo int-vec x))
          (:empty int-vecs)
          (:empty long-vecs)
          (:empty regular-vecs)
          (:shorter int-vecs)
          (:shorter long-vecs)
          (:shorter regular-vecs)
          (:first-lesser int-vecs)
          (:first-lesser long-vecs)
          (:first-lesser regular-vecs)
          (:last-lesser int-vecs)
          (:last-lesser long-vecs)
          (:last-lesser regular-vecs)
          (:rand-lesser-1 int-vecs)
          (:rand-lesser-1 long-vecs)
          (:rand-lesser-1 regular-vecs)
          (:rand-lesser-2 int-vecs)
          (:rand-lesser-2 long-vecs)
          (:rand-lesser-2 regular-vecs)
          (:rand-lesser-3 int-vecs)
          (:rand-lesser-3 long-vecs)
          (:rand-lesser-3 regular-vecs))))))


(deftest test-vec-associative
  (let [empty-v (alt-fn-vector)
        v       (into empty-v (range 1 6))]
    (testing "Associative.containsKey"
      (are [x] (.containsKey v x)
        0 1 2 3 4)
      (are [x] (not (.containsKey v x))
        -1 -100 nil [] "" #"" #{} 5 100)
      (are [x] (not (.containsKey empty-v x))
        0 1))
    (testing "contains?"
      (are [x] (contains? v x)
        0 2 4)
      (are [x] (not (contains? v x))
        -1 -100 nil "" 5 100)
      (are [x] (not (contains? empty-v x))
        0 1))
    (testing "Associative.entryAt"
      (are [idx val] (= (clojure.lang.MapEntry. idx val)
                        (.entryAt v idx))
        0 1
        2 3
        4 5)
      (are [idx] (nil? (.entryAt v idx))
        -5 -1 5 10 nil "")
      (are [idx] (nil? (.entryAt empty-v idx))
        0 1))))


(deftest empty-vector-equality
  (let [colls [[] (alt-fn-vector) '()]]
    (doseq [c1 colls, c2 colls]
      (is (= c1 c2))
      (is (.equals c1 c2)))))


(defn =vec
  [expected v] (and (vector? v) (= expected v)))


(deftest test-mapv
  (are [r c1] (=vec r (mapv + c1))
    (alt-fn-vector 1 2 3) (alt-fn-vector 1 2 3))
  (are [r c1 c2] (=vec r (mapv + c1 c2))
    (alt-fn-vector 2 3 4) (alt-fn-vector 1 2 3) (repeat 1))
  (are [r c1 c2 c3] (=vec r (mapv + c1 c2 c3))
    (alt-fn-vector 3 4 5) (alt-fn-vector 1 2 3) (repeat 1) (repeat 1))
  (are [r c1 c2 c3 c4] (=vec r (mapv + c1 c2 c3 c4))
    (alt-fn-vector 4 5 6) (alt-fn-vector 1 2 3) (alt-fn-vector 1 1 1) [1 1 1] [1 1 1]))


(deftest test-filterv
  (are [r c1] (=vec r (filterv even? c1))
    [] (alt-fn-vector 1 3 5)
    [2 4] (alt-fn-vector 1 2 3 4 5)))


(deftest test-subvec
  (let [v1 (apply alt-fn-vector (range 100))
        v2 (subvec v1 50 57)]
    (is (thrown? IndexOutOfBoundsException (v2 -1)))
    (is (thrown? IndexOutOfBoundsException (v2 7)))
    (is (test-while-locked-to-default-options (= (v1 50) (v2 0))))
    (is (test-while-locked-to-default-options (= (v1 56) (v2 6))))))


(deftest test-vec
  (is (= [1 2] (alt-fn-vec (first {1 2}))))
  (is (= [0 1 2 3] (alt-fn-vec [0 1 2 3])))
  (is (= [0 1 2 3] (alt-fn-vec (list 0 1 2 3))))
  (is (= [0 1 2 3] (alt-fn-vec (sorted-set 0 1 2 3))))
  (is (= [[1 2] [3 4]] (alt-fn-vec (sorted-map 1 2 3 4))))
  (is (= [0 1 2 3] (alt-fn-vec (range 4))))
  (is (= [\a \b \c \d] (alt-fn-vec "abcd")))
  (is (= [0 1 2 3] (alt-fn-vec (object-array (range 4)))))
  (is (= [1 2 3 4] (alt-fn-vec (eduction (map inc) (range 4)))))
  ;; TODO: Need to investigate the following test.
  #_(is (= [0 1 2 3] (alt-fn-vec (reify clojure.lang.IReduceInit
                                   (reduce [_ f start]
                                     (reduce f start (range 4))))))))


(deftest test-reduce-kv-vectors
  (is (= 25 (reduce-kv + 10 (alt-fn-vector 2 4 6))))
  (is (= 25 (reduce-kv + 10 (subvec (alt-fn-vec [0 2 4 6]) 1)))))


(deftest test-vector-eqv-to-non-counted-types
  (is (not= (range) (alt-fn-vector 0 1 2)))
  (is (not= (alt-fn-vector 0 1 2) (range)))
  (is (= (alt-fn-vector 0 1 2) (take 3 (range))))
  (is (= (alt-fn-vector 0 1 2) (new java.util.ArrayList [0 1 2])))
  (is (not= (alt-fn-vector 1 2) (take 1 (cycle [1 2]))))
  (is (= (alt-fn-vector 1 2 3 nil 4 5 6 nil) (eduction cat [[1 2 3 nil] [4 5 6 nil]]))))


(set! *warn-on-reflection* true)


;; remember returns a consumer that adds to an-atom of coll
(defn remember
  ^Consumer [an-atom]
  (reify Consumer (accept [_ v] (swap! an-atom conj v))))


(deftest test-empty-vector-spliterator
  (let [v (alt-fn-vector)
        s (.spliterator ^Collection v)
        seen (atom [])]
    (is (= 0 (.estimateSize s) (.getExactSizeIfKnown s)))
    (is (nil? (.trySplit s)))
    (is (false? (.tryAdvance s (remember seen))))
    (is (= @seen []))))


;; tryAdvance then forEachRemaining walks vector spliterator
(deftest test-spliterator-tryadvance-then-forEach
  (let [n 66
        source-vec (alt-fn-vec (range n))]
    (for [v (into [(alt-fn-vec (range n))] (map #(subvec source-vec % (+ % 33)) (range 33)))]
      (dotimes [up-to n]
        (let [s (.spliterator ^Collection v)
              seen (atom [])
              consumer (remember seen)]
          (loop [i 0]
            (if (< i up-to)
              (do (is (true? (.tryAdvance s consumer))) (recur (inc i)))
              (.forEachRemaining s consumer)))
          (is (= v @seen))
          (is (false? (.tryAdvance s consumer))))))))


;; recursively split vector spliterators, walk all of the splits
(deftest test-spliterator-trySplit
  (dotimes [n 257]
    (let [v (alt-fn-vec (range n))
          seen (atom #{})
          consumer (remember seen)
          splits (loop [ss [(.spliterator ^Collection v)]]
                   (let [ss' (map #(.trySplit ^Spliterator %) ss)]
                     (if (every? nil? ss')
                       ss
                       (recur (into ss (remove nil? ss'))))))]
      (loop [[spl & sr] splits]
        (when spl
          (.forEachRemaining ^Spliterator spl consumer)
          (recur sr)))
      (is (= v (sort @seen))))))


(deftest test-vector-parallel-stream
  (dotimes [n 1024]
    (let [v (alt-fn-vec (range n))]
      (is (= n
             (-> ^Collection v .stream (.collect (Collectors/counting)))
             (-> ^Collection v .parallelStream (.collect (Collectors/counting)))
             (-> v ^Collection (subvec 0 n) .stream (.collect (Collectors/counting)))
             (-> v ^Collection (subvec 0 n) .parallelStream (.collect (Collectors/counting))))))))


(set! *warn-on-reflection* false)



;;;; SECTION TWO

;; Tests written by Brad Losavio

;; Use `testfoo.utilities/scaffold` to enumerate methods of
;; com.sagevisuals.AltFnInvocablePersistentVector
;; Note: Some methods immediately throw an UnsupportedOperationException





(deftest enumerated-methods-alt-fn-vector-tests
  (testing "com.sagevisuals.AltFnInvocablePersistentVector methods"
    (are [x y] [= x y]
      [1 2 99 4 5] (.assocN (alt-fn-vector 1 2 3 4 5) 2 99)
      [1 2 3 99] (.cons (alt-fn-vector 1 2 3) 99)
      [1 2 3] (.chunkedSeq (alt-fn-vector 1 2 3))
      3 (.count (alt-fn-vector 1 2 3))
      [2 3] (.drop (alt-fn-vector 1 2 3) 1)
      [] (.empty (alt-fn-vector 1 2 3))
      9 (.kvreduce (alt-fn-vector 1 2 3) + 0)
      nil (.meta (alt-fn-vector 1 2 3))
      {:foo "bar"} (.meta (with-meta (alt-fn-vector 1 2 3) {:foo "bar"}))
      3 (.nth (alt-fn-vector 1 2 3) 2)
      [1 2] (.pop (alt-fn-vector 1 2 3))
      6 (.reduce (alt-fn-vector 1 2 3) +)
      nil (.seq (alt-fn-vector))
      (seq [1 2 3]) (.seq (alt-fn-vector 1 2 3))))
  (testing "clojure.lang.APersistentVector methods"
    (let [v (alt-fn-vector 1 2 3)]
      (are [x y] (= x y)
        [1 2 3 99] (.assoc v 3 99)
        0 (.compareTo v [1 2 3])
        true (.contains v 3)
        true (.containsAll (alt-fn-vector 1 1 1) [1 1 1])
        true (.containsKey v 0)
        [2 3] (.entryAt v 2)
        true (.equals v [1 2 3])
        true (.equiv v [1 2 3])
        true (.equiv v v)
        3 (.get v 2)
        2 (.indexOf v 3)
        3 (test-while-locked-to-default-options (.invoke v 2))
        true (.isEmpty (alt-fn-vector))
        false (.isEmpty v)
        3 (.lastIndexOf (alt-fn-vector 1 1 2 2 3 3) 2)
        3 (.length v)
        3 (.peek v)
        [3 2 1] (.rseq v)
        3 (.size v)
        [2 3 4] (.subList (alt-fn-vector 1 2 3 4 5) 1 4)
        "⟨1 2 3⟩" (test-while-locked-to-default-options (.toString v))
        3 (.valAt v 2)
        nil (.valAt v 99))))
  (testing "clojure.lang.AFn and java.lang.Object methods"
    (are [x y] (= x y)
      3 (test-while-locked-to-default-options (.invoke (alt-fn-vector 1 2 3) 2))
      com.sagevisuals.AltFnInvocablePersistentVector (.getClass (alt-fn-vector 1 2 3)))))


(deftest options-validator-tests
  (testing "complete contents"
    (are [x y] (= x y)
      false (options-validator {})
      false (options-validator {:fn inc})
      false (options-validator {:fn inc :right-delimiter "foo"})
      false (options-validator {:fn inc                        :left-delimiter "bar"})
      true  (options-validator {:fn inc :right-delimiter "foo" :left-delimiter "bar"})))
  (testing "content properties"
    (are [x y] (= x y)
      false (options-validator {:fn "x" :right-delimiter "foo" :left-delimiter "bar"})
      false (options-validator {:fn inc :right-delimiter \f    :left-delimiter "bar"})
      false (options-validator {:fn inc :right-delimiter "foo" :left-delimiter :bar })
      true  (options-validator {:fn inc :right-delimiter "foo" :left-delimiter "bar"}))))


(deftest atom-validator
  (is (thrown? IllegalStateException (atom {} :validator options-validator)))
  (is (thrown? IllegalStateException (atom {:fn inc
                                            :right-delimiter "foo"
                                            :left-delimiter :bar}
                                           :validator options-validator))))


(deftest test-vec-creation
  (testing "empty and instance"
    (are [x] (and (empty? x) (instance? com.sagevisuals.AltFnInvocablePersistentVector x))
      (alt-fn-vector))
    #_(testing "with invalid type argument"
        (are [x] (thrown? IllegalArgumentException x))
        ;; alt-fn-vector may contain any legal value
        ))
  (testing "vector-like (vector-of x1 x2 x3 … xn)"
    (are [vec gvec] (and (instance? com.sagevisuals.AltFnInvocablePersistentVector gvec)
                         (= (into (alt-fn-vector) vec) gvec)
                         (= vec gvec)
                         (= (hash vec) (hash gvec)))
      [1] (alt-fn-vector 1)
      [1 2] (alt-fn-vector 1 2)
      [1 2 3] (alt-fn-vector 1 2 3)
      [1 2 3 4] (alt-fn-vector 1 2 3 4)
      [1 2 3 4 5] (alt-fn-vector 1 2 3 4 5)
      [1 2 3 4 5 6] (alt-fn-vector 1 2 3 4 5 6)
      (apply vector (range 1000)) (apply alt-fn-vector (range 1000))
      [1 2 3] (apply alt-fn-vector (map int [1M 2.0 3.1]))
      [97 98 99] (apply alt-fn-vector (map int [\a \b \c])))
    (testing "with null values"
      #_(are [x] (thrown? NullPointerException x)
          ;; alt-fn-vector may contain any legal value
          ))
    (testing "with unsupported values"
      #_(are [x] (thrown? ClassCastException x)
          ;; alt-fn-vector may contain any legal value
          ))
    (testing "instances of IPersistentVector"
      (are [gvec] (instance? clojure.lang.IPersistentVector gvec)
        (apply alt-fn-vector (vector-of :int 1 2 3))
        (apply alt-fn-vector (vector-of :double 1 2 3))))
    (testing "fully implements IPersistentVector"
      (are [gvec] (= 3 (.length gvec))
        (apply alt-fn-vector (vector-of :int 1 2 3))
        (apply alt-fn-vector (vector-of :double 1 2 3))))))


(deftest alt-fn-vector-creation
  (testing "integer elements"
    (are [x] (let [v (vec (range x))
                   alt-v (into (alt-fn-vector) (range x))]
               (and (= v alt-v)
                    (instance? com.sagevisuals.AltFnInvocablePersistentVector alt-v)))
      0 1 2 3 4 5 6 7 8 33 65))
  (testing "non-integer elements"
    (are [x y] (= x y)
      [{:a :foo} {:b :bar}] (alt-fn-vector {:a :foo} {:b :bar})
      [nil] (alt-fn-vector nil)))
  (testing "implements"
    (let [alt-v (alt-fn-vector 1 2 3)]
      (are [x] (true? x)
        (coll? alt-v)
        (vector? alt-v)
        (seq? (seq alt-v))
        (sequential? alt-v)
        (associative? alt-v)
        (counted? alt-v)
        (reversible? alt-v)))))


(defn test-toString-while-locked
  "Given an options map `m`, evaluates the `.toString()` instance method of
  AlternateFunctionInvokePersistentVector `v` in a locking context."
  {:UUIDv4 #uuid "2df99edb-f1e0-4273-bc88-e9f37ceff1f6"}
  [v m]
  (let [lock (Object.)]
    (locking lock
      (reset! options m)
      (.toString v))))


(deftest alt-fn-vector-toString
  (testing "default"
    (are [x y] (= x (test-toString-while-locked y default-options))
      "⟨⟩" (alt-fn-vector)
      "⟨1 2 3⟩" (alt-fn-vector 1 2 3)))
  (testing "non-defaults"
    (let [non-default-options {:fn nth
                               :left-delimiter "<<<"
                               :right-delimiter ">>>"}]
      (are [x y] (= x (test-toString-while-locked y non-default-options))
        "<<<>>>" (alt-fn-vector)
        "<<<1 2 3>>>" (alt-fn-vector 1 2 3)))
    (reset! options default-options)))


(deftest alt-fn-vec-tests
  (testing "sequentials"
    (are [x] (= [1 2 3] x)
      (alt-fn-vec [1 2 3])
      (alt-fn-vec '(1 2 3 ))
      (alt-fn-vec (sorted-set 1 2 3))))
  (testing "maps"
    (are [x y] (= x y)
      [[:a 1] [:b 2] [:c 3]] (alt-fn-vec {:a 1 :b 2 :c 3})))
  (testing "self"
    (are [x y] (= x y)
      (alt-fn-vec [1 2 3]) (alt-fn-vector 1 2 3))))


(defn test-invoke-while-locked
  "Given an options map `m` and zero or more `args`, evaluates the `.invoke()`
  instance method of AlternateFunctionInvokePersistentVector `v` in a locking
  context."
  {:UUIDv4 #uuid "2df99edb-f1e0-4273-bc88-e9f37ceff1f6"}
  [v m & args]
  (let [lock (Object.)]
    (locking lock
      (reset! options m)
      (apply v args))))


(deftest alt-fn-vector-invoke
  (testing "basic functions"
    (are [x y] (= x y)
      99 (test-invoke-while-locked (alt-fn-vector 97 98 99) {:fn nth
                                                             :left-delimiter "~~~"
                                                             :right-delimiter "~~~"} 2)

      5 (test-invoke-while-locked (alt-fn-vector 1 2 3 4 5) {:fn #(count %)
                                                             :left-delimiter "~~~"
                                                             :right-delimiter "~~~"})

      [1 2 3 4 5 6] (test-invoke-while-locked (alt-fn-vector 1 2 3) {:fn #(concat %1 %2)
                                                                     :left-delimiter "==="
                                                                     :right-delimiter "==="} [4 5 6])))
  (testing "arities & argument order"
    (let [v (alt-fn-vector)
          f (fn [_ & args] args)
          m {:fn f
             :left-delimiter ""
             :right-delimiter ""}]
      (are [x y] (= x y)
        [1]               (test-invoke-while-locked v m 1)
        [1 2]             (test-invoke-while-locked v m 1 2)
        [1 2 3]           (test-invoke-while-locked v m 1 2 3)
        [1 2 3 4]         (test-invoke-while-locked v m 1 2 3 4)
        [1 2 3 4 5]       (test-invoke-while-locked v m 1 2 3 4 5)
        [1 2 3 4 5 6]     (test-invoke-while-locked v m 1 2 3 4 5 6)
        [1 2 3 4 5 6 7]   (test-invoke-while-locked v m 1 2 3 4 5 6 7)
        [1 2 3 4 5 6 7 8] (test-invoke-while-locked v m 1 2 3 4 5 6 7 8))
      (are [x] (= x (apply test-invoke-while-locked v m x))
        (range 1)
        (range 2)
        (range 3)
        (range 4)
        (range 5)
        (range 6)
        (range 7)
        (range 8)))))


(defaultize!-options)
#_(run-tests)