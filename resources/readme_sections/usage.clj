[:section#usage

 [:h2 "Usage"]

 [:h3 "Basics"]

 [:p "Let's pretend we want to create a trio of single-arity functions. "]

 [:ul
  [:li [:code "Alice"] " returns the string " [:code "Hello, World!"] " regardless of the argument."]
  [:li [:code "Bob"] " increments its argument."]
  [:li [:code "Charlie"] " reverses the sequence passed as an argument."]]

 [:p "Since we're pretending, we want to define " [:code "Alice"] ", " [:code "Bob"] ", and " [:code "Charlie"] " each as a sequential collection, such as a Clojure vector. For the sake of discussion, we'll state the following \"function\" definitions, using vector literals."]

 [:pre
  (print-form-then-eval "(def Alice [:a])") [:br]
  (print-form-then-eval "(def Bob [:b])") [:br]
  (print-form-then-eval "(def Charlie [:c])")]

 [:p "Let's write an application function, whose name we'll intentionally mis-spell as " [:code "appli"] " so that we don't shadow " [:code "clojure.core/apply"] ". " [:code "appli"] " dispatches on argument " [:code "f"] "'s first element."]

 [:pre (-> (print-form-then-eval "(defn appli [f x]
                              (condp = (first f)
                             :a \"Hello, World!\"
                             :b (inc x)
                             :c (reverse x)))" 45 45)
           (newline-and-indent-after-chars 3 ["f)" "rld!\"" "inc x)"]))]

 [:p "For this demonstration, that " [:code "condp"] " serves as a kind of crude lookup table, but " [:code "appli"] " could be anything, such as a higher-order function, or a recursive function, etc."]

 [:p "Now we test out what we've done."]

 [:pre
  (print-form-then-eval "(appli Alice :an-ignored-arg)") [:br] [:br]
  (print-form-then-eval "(appli Bob 99)") [:br] [:br]
  (print-form-then-eval "(appli Charlie [:chocolate :strawberry :vanilla])" 85 75)]

 [:p "That looks promising. " [:code "Alice"] " returns string " [:code "Hello, World!"] " regardless of the argument, " [:code "Bob"] " increments the numeric argument, and " [:code "Charlie"] " indeed reverses the sequence passed as the next argument."]

 [:p "There's something we'd like to improve: We wanted to think of " [:code "Alice"] ", " [:code "Bob"] ", and " [:code "Charlie"] " as functions, so having " [:code "appli"] " sprinkled throughout kinda destroys that illusion."]

 [:p "Regular Clojure vectors have the capability to act as functions when at the head of an S-expression."]

 [:pre (print-form-then-eval "([97 98 99] 1)")]

 [:p "We see that when a vector is at the head of the S-expression, there's an implied " [:code "nth"] ". It's as if we had written this."]

 [:pre (print-form-then-eval "(nth [97 98 99] 1)")]

 [:p "For our Alice/Bob/Charlie trio, we'd like for there to be instead an implied " [:code "appli"] "."]

 [:p "To do that, let's introduce a new utility, " [:code "make-thingy"] "."]

 [:pre (print-form-then-eval "(make-thingy 97 98 99)")]

 [:p "Does it do vector tasks?"]

 [:pre
  (print-form-then-eval "(count (make-thingy 97 98 99))") [:br]
  (print-form-then-eval "(nth (make-thingy 97 98 99) 1)") [:br]
  (print-form-then-eval "(conj (make-thingy 97 98 99) 100)")]

 [:p "That certainly " [:em "looks"] " like a vector. Each instance of a " [:em "thingy"] " implements the vector interface, so we have all the familiar functions like " [:code "count"] ", " [:code "conj"] ", etc."]

 [:p "Now, let's re-define the our trio using " [:code "make-thingy"] "."]

 [:pre
  (print-form-then-eval "(def Alice (make-thingy :a))") [:br]
  (print-form-then-eval "(def Bob (make-thingy :b))") [:br]
  (print-form-then-eval "(def Charlie (make-thingy :c))")]

 [:p "Let's see what happens when we try to use a " [:em "thingy"] " as a function."]

 (do ;; evaluate without displaying results
   (thingy.dangerous-vector/defaultize!-options)
   nil)

 [:pre (print-form-then-eval "(Alice 0)")]

 [:p "Hmm. It appears to behave like a regular vector, with an implicit " [:code "nth"] ". This is the default."]

 [:p "Next, we'll re-set the invocation behavior of all instances of " [:em "thingy"] "s to " [:code "appli"] " using another utility, " [:code "defn-thingy"] ". It looks pretty much like " [:code "defn"] "."]

 [:pre (-> (print-form-then-eval "(defn-thingy my-appli \"doc string\" {:metadata \"foo\"} [f x] (appli f x))")
           trim-evaluation
           (newline-and-indent-after-chars 1 ["my-appli" "ing\"" "}" "x]"]))]

 [:p [:code "defn-thingy"] " mutates the invocation behavior for all " [:em "thingy"] " instances."]

 [:p "Let's see how our trio behaves."]

 [:pre
  (print-form-then-eval "(Alice :an-ignored-arg)") [:br] [:br]
  (print-form-then-eval "(Bob 99)") [:br] [:br]
  (print-form-then-eval "(Charlie [:chocolate :strawberry :vanilla])")]

 [:p "Now our " [:code "Alice"] ", " [:code "Bob"] ", and " [:code "Charlie"] " thingys behave like functions."]

 [:h3 "Details"]

 [:p "There are two steps to using the " [:em "thingy"] " library: creating an instance, and assigning the invocation function."]

 [:ol
  [:li [:p [:strong "Creating"] " and manipulating a " [:em "thingy"] " instance is analogous to that of vectors. To create, use " [:code "make-thingy"]]

   [:pre (print-form-then-eval "(make-thingy 97 98 99)")]

   [:p "similar to using " [:code "clojure.core/vector"] ". (There is no analogous facility to making a vector literal.) Create a new instance from some other existing collection like this."]

   [:pre (print-form-then-eval "(into (make-thingy) #{:foo :bar :baz})")]

   [:p "Note that the type is distinct from a standard Clojure persistent vector."]

   [:pre (print-form-then-eval "(type (into (make-thingy) #{:foo :bar :baz}))")]

   [:p "Manipulate an instance with your favorite tools."]

   [:pre
    (print-form-then-eval "(update (make-thingy 97 98 99) 2 inc)") [:br] [:br]
    (print-form-then-eval "(map inc (make-thingy 97 98 99))")]

   [:p "Note that, just like Clojure vectors, sequence functions consuming a " [:em "thingy"] " instance will return a sequence."]

   [:pre (print-form-then-eval "(type (map inc (make-thingy 97 98 99)))")]]

  [:li [:p [:strong "Assigning"] " a " [:em "thingy"] " invocation function is analogous to using " [:code "defn"] ". One difference is that supplying a doc-string and metadata are required. (Feel free to leave them empty, though.)"]

   [:pre (-> (print-form-then-eval "(defn-thingy yippee
                                 \"My docstring.\"
                                 {:added 1.2}
                                 [_ _]
                                 \"Hooray!\")")
             (trim-evaluation)
             (newline-and-indent-after-chars 1 ["yippee"
                                                "string.\""
                                                "1.2}"
                                                "_]"]))]

   [:p "This example assigns a 2-arity function that returns a string, ignoring its arguments. Observe."]

   [:pre
    (print-form-then-eval "((make-thingy) 99)") [:br]
    (print-form-then-eval "((make-thingy :a :b :c) :foo)") [:br]
    (print-form-then-eval "((make-thingy 1.23 4.56) 22/7)")]

   [:p "The function is accessible in the same manner as any other var in the namespace."]

   [:p "The name has no affect on the operation of " [:em "thingy"] " instances, but is provided so that the function may be invoked manually, like this."]

   [:pre (print-form-then-eval "(yippee :a :b)")]

   [:p "Evaluating " [:code "defn-thingy"] " synchronously mutates the invocation function for all " [:em "thingy"] " instances."]

   [:p "The invocation function may have an arity of zero to eight, inclusive. When there is at least one argument, the " [:em "thingy"] " instance is passed as the first argument."]]]]