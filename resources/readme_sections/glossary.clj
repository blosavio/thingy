[:section#glossary

 [:h2 "Glossary"]

 [:dl

  [:dt#instance "thingy instance"]

  [:dd [:p "An instance of " [:code "com.sagevisuals.AltFnInvocableVector"] ". In nearly every regard, identical to an instance of " [:code "clojure.lang.PersistentVector"] ". Whereas a Clojure vector at the head of an S-expression behaves as if there is an implicit " [:code "nth"] ", a " [:em "thingy"] " instance invokes a dynamically-mutable " [:a {:href "#inv-fn"} "invocation function"] ". (Defaults to " [:code "nth"] ".)"]
   [:p "Instantiated with " [:code "make-thingy"] "."]]

  [:dt#inv-fn "invocation function"]

  [:dd [:p "The function that is invoked when evaluating an S-expression with a " [:a {:href "#instance"} [:em "thingy"] " instance"] " at the head. In all respects, a standard Clojure function, that may be called from any site."]

   [:p "Defined with " [:code "defn-thingy"] "."]]]]