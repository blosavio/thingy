(ns thingy.core
  "Define and instantiate thingys."
  (:require [thingy.dangerous-vector :refer [alt-fn-vec
                                             alt-fn-vector
                                             defaultize!-options
                                             options
                                             reset!-options]]))


(defmacro defn-thingy
  "Defines a _thingy_ function by binding `name` to `body` and assigns it to
  all instances. Analagous to `defn`, but requires a `doc-string` and a metadata
  `attr-map`.

  `name` is a function with an arity of 0 to 8, inclusive. If the function
  accepts an argument, the thingy instance is the first argument, followed by
  up to eight additonal arguments.

  Example:
  ```clojure
  (defn-thingy foo-test
    \"Foo doc-string\"
    {:added 1.2}
    [a b c]
    (concat a b c))

  (def X (make-thingy :a :b))
  (def Y (make-thingy :c :d))
  (def Z (make-thingy :e :f))

  (X Y Z) ;; => (:a :b :c :d :e :f)
  ```

  See also [[make-thingy]]."
  {:UUIDv4 #uuid "561b059d-c6c2-468f-9ff1-b710a8275fb9"}
  [name doc-string attr-map args body]
  `(do
     (defn ~name ~doc-string ~attr-map [~@args] ~body)
     (reset!-options {:fn ~name
                      :left-delimiter "["
                      :right-delimiter "]"})))


(defn make-thingy
  "Given elements `xs`, returns an instance of a _thingy_.

  See also [[defn-thingy]]."
  {:UUIDv4 #uuid "68f93654-8c38-4051-9dba-27826235cf97"}
  [& xs]
  (alt-fn-vec xs))