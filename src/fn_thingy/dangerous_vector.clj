(ns fn-thingy.dangerous-vector
  "WARNING: Unless you are the author of this library, you almost certainly
  don't want to use this namespace. The items provided here diverge from
  typical Clojure semantics and are intended for a very specific case that is
  unusual. All other uses are strongly discouraged.

  This namespace provides instances of vectors with dynamically-setable function
  invocation when they appear in the first element in an S-expression. The
  `toString` method is also dynamically setable to indicate that the vectors
  are altered. All other behaviors are identical to Clojure's built-in persitent
  vectors.

  Examples
  ```clojure
  (def normal-vector (vector 1 2 3))
  (def dangerous-vector (alt-fn-vector 1 2 3))

  ;; shares standard behavior
  (count normal-vector) ;; 3
  (count dangerous-vector) ;; 3
  ```

  Change the invocaton function.
  ```clojure
  (reset!-options {:fn (fn [v1 v2] (concat v1 v2))
                 :left-delimiter \"⟨\"
                 :right-delimiter \"⟩\"})
  ```

  Now, the `invoke` and `toString` behavior is altered.
  ```clojure
  (dangerous-vector [97 98 99]) ;; (1 2 3 97 98 99)

  (.toString dangerous-vector) ;; \"⟨1 2 3⟩\"
  ```"
  (:require [fn-thingy.utilities :refer [scaffold print-n-newlines]]))


(import com.sagevisuals.AltFnInvocablePersistentVector)


(defonce default-options {:fn nth
                          :left-delimiter "⟨"
                          :right-delimiter "⟩"})


(defn options-validator
  "Given hash-map `m`, returns `true` if the `:fn` value is a function, and if
  the `:left-delimiter` and `:right-delimiter` values are strings."
  {:UUIDv4 #uuid "678a3cbc-a1c0-4ef8-9a0e-58d3a2d49410"}
  [m]
  (and (contains? m :fn)
       (contains? m :left-delimiter)
       (contains? m :right-delimiter)
       (fn? (:fn m))
       (string? (:left-delimiter m))
       (string? (:right-delimiter m))))


(def options (atom default-options :validator options-validator))


(defn reset!-options
  "Resets options to map `m`. `m` must associate the following key-vals:

  * `:fn`               a function
  * `:left-delimiter`   a string
  * `:right-delimiter` a string

  Note: `m` must be coerced into a clojure.lang.PersistentHashMap."
  {:UUIDv4 #uuid "1f51852b-56cf-4387-b9d9-d0a91703ca81"}
  [m]
  (reset! options m))


(defn defaultize!-options
  "Resets options map to default values. See [[default-options]]."
  {:UUIDv4 #uuid "88653ccc-ff14-46b2-932d-d9e2906dbeae"}
  []
  (reset! options default-options))


(defn alt-fn-vector
  "Creates a new vector containing the args. The returned vector has a
  modifiable function behavior (defaults to `nth`).

  Analogous to `clojure.core.vector`."
  {:UUIDv4 #uuid "7208362c-3d06-41c6-97cf-3bc068c21632"}
  ([]                   (. com.sagevisuals.AltFnInvocablePersistentVector (create options (list))))
  ([a]                  (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (list)))))
  ([a b]                (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (list b)))))
  ([a b c]              (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (cons b (list c))))))
  ([a b c d]            (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (cons b (cons c (list d)))))))
  ([a b c d e]          (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (cons b (cons c (cons d (list e))))))))
  ([a b c d e f]        (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (cons b (cons c (cons d (cons e (list f)))))))))
  ([a b c d e f & args] (. com.sagevisuals.AltFnInvocablePersistentVector (create options (cons a (cons b (cons c (cons d (cons e (cons f args))))))))))


(defn alt-fn-vec
  "Creates a new vector containing the contents of collection `c`.

  Analogous to `clojure.core.vec`, but does not currently handle Java arrays in
  the same manner."
  {:UUIDv4 #uuid "916f21ca-636b-4d0a-b72c-87b445e0145b"}
  [c]
  (apply alt-fn-vector c))