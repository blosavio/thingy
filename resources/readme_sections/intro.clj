[:section#intro
 [:h2 "Introduction"]

 [:p "This Clojure library provides tools to create function-like objects which implement the vector interface. It is intended for a very specific, niche use case and it is exceedingly unlikely to be useful or safe outside of that niche."]

 [:p "A " [:em "fn-thingy"] " instance, when appearing at the head of an S-expression serves as a function, consuming the arguments contained in the tail and yielding a value. Unlike a typical function, the internal structure of a " [:em "fn-thingy"] " is manipulatable with all the functions that make up the vector interface."]

 [:p "Such objects may be used in " [:a {:href "#examples"} "academic programming languages"] " that explore using sequential collections of values to define functions. It is certainly possible to write "]

 [:pre [:code "(application-function vector arg1 arg2...)"]]

 [:p "where " [:code "application-function"] " provides some kind of logic about how to interpret " [:code "vector"] ". But having " [:code "application-function"] " scattered around is visually distracting. Instead of plainly representing the concepts, the machinery is leaking onto the page. It is much nicer to write"]

 [:pre [:code "(fn-vec arg1 arg2...)"]]

 [:p "as well to read and to understand. The " [:em "fn-thingy"] " library enables such streamlining."]]