(ns thingy.core
  "Instantiate _thingy_ objects and assign their invocation function.

  Example:

  1. Create _thingy_ instance.
      ```clojure
      (def a-thingy (make-thingy :a :b))

      a-thingy ;; => [:a :b]
      ```

  2. Define an invocation function, e.g., with `defn`.
      ```clojure
      (defn foo
        \"A 3-arity example, concatenating three vector-like objects.\"
        [x y z]
        (concat x y z))
      ```

  3. Assign the _thingy_ invocation function.
      ```clojure
      (assign-thingy-fn! foo)
      ```

  4. Test invocation behavior of the _thingy_.
      ```clojure
      (a-thingy [:c :d] [:e :f]) ;; => (:a :b :c :d :e :f)
      ```
      Note: This evaluation concatenates a _thingy_ instance, `a-thingy`, with
      two standard Clojure vectors."
  (:require [thingy.dangerous-vector :refer [alt-fn-vec
                                             reset!-options]]))



(defn assign-thingy-fn!
  "Synchronously mutates the invocation function of all _thingy_ instances to
  function `f`.

  `f` is a function with an arity of 0 to 9, inclusive. If `f` accepts an
  argument, the thingy instance is that first argument, followed by up to eight
  additional arguments.

  See also [[make-thingy]] and [[thingy.core]]."
  {:UUIDv4 #uuid "1764069c-76af-487e-ad7a-c43c03566804"}
  [f]
  (reset!-options {:fn f
                   :left-delimiter "["
                   :right-delimiter "]"}))


(defn make-thingy
  "Given elements `xs`, returns an instance of a _thingy_. Analogous to
  [`clojure.core/vector`](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/vector).

  Example:
  ```clojure
  (make-thingy 1 2 3) ;; => [1 2 3]
  ```

  See also [[assign-thingy-fn!]] and [[thingy.core]]."
  {:UUIDv4 #uuid "68f93654-8c38-4051-9dba-27826235cf97"}
  [& xs]
  (alt-fn-vec xs))