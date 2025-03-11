
  <body>
    <a href="https://clojars.org/com.sagevlisuals/fn-thingy"><img src="https://img.shields.io/clojars/v/com.sagevlisuals/fn-thingy.svg"></a><br>
    <a href="#setup">Setup</a><br>
    <a href="https://blosavio.github.io/fn-thingy/index.html">API</a><br>
    <a href="https://github.com/blosavio/fn-thingy/blob/main/changelog.md">Changelog</a><br>
    <a href="#intro">Introduction</a><br>
    <a href="#usage">Usage</a><br>
    <a href="#examples">Examples</a><br>
    <a href="#glossary">Glossary</a><br>
    <a href="https://github.com/blosavio">Contact</a><br>
    <h1>
      fn-thingy
    </h1><em>A Clojure library that provides a bizarre function definition mechanism</em><br>
    <section id="setup">
      <h2>
        Setup
      </h2>
      <h3>
        Leiningen/Boot
      </h3>
      <pre><code>[com.sagevlisuals/fn-thingy &quot;0-SNAPSHOT0&quot;]</code></pre>
      <h3>
        Clojure CLI/deps.edn
      </h3>
      <pre><code>com.sagevlisuals/fn-thingy {:mvn/version &quot;0-SNAPSHOT0&quot;}</code></pre>
      <h3>
        Require
      </h3>
      <pre><code>(require &apos;[fn-thingy.core :refer [defn-thingy make-fn-thingy]])</code></pre>
    </section>
    <section id="intro">
      <h2>
        Introduction
      </h2>
      <p>
        This Clojure library provides tools to create function-like objects which implement the vector interface. It is intended for a very specific, niche use
        case and it is exceedingly unlikely to be useful or safe outside of that niche.
      </p>
      <p>
        A <em>fn-thingy</em> instance, when appearing at the head of an S-expression serves as a function, consuming the arguments contained in the tail and
        yielding a value. Unlike a typical function, the internal structure of a <em>fn-thingy</em> is manipulatable with all the functions that make up the
        vector interface.
      </p>
      <p>
        Such objects may be used in <a href="#examples">academic programming languages</a> that explore using sequential collections of values to define
        functions. It is certainly possible to write
      </p>
      <pre><code>(application-function vector arg1 arg2...)</code></pre>
      <p>
        where <code>application-function</code> provides some kind of logic about how to interpret <code>vector</code>. But having
        <code>application-function</code> scattered around is visually distracting. Instead of plainly representing the concepts, the machinery is leaking onto
        the page. It is much nicer to write
      </p>
      <pre><code>(fn-vec arg1 arg2...)</code></pre>
      <p>
        as well to read and to understand. The <em>fn-thingy</em> library enables such streamlining.
      </p>
    </section>
    <section id="usage">
      <h2>
        Usage
      </h2>
      <h3>
        Basics
      </h3>
      <p>
        Let&apos;s pretend we want to create a trio of single-arity functions.
      </p>
      <ul>
        <li>
          <code>Alice</code> returns the string <code>Hello, World!</code> regardless of the argument.
        </li>
        <li>
          <code>Bob</code> increments its argument.
        </li>
        <li>
          <code>Charlie</code> reverses the sequence passed as an argument.
        </li>
      </ul>
      <p>
        Since we&apos;re pretending, we want to define <code>Alice</code>, <code>Bob</code>, and <code>Charlie</code> each as a sequential collection, such as
        a Clojure vector. For the sake of discussion, we&apos;ll state the following &quot;function&quot; definitions, using vector literals.
      </p>
      <pre><code>(def Alice [:a])</code><br><code>(def Bob [:b])</code><br><code>(def Charlie [:c])</code></pre>
      <p>
        Let&apos;s write an application function, whose name we&apos;ll intentionally mis-spell as <code>appli</code> so that we don&apos;t shadow
        <code>clojure.core/apply</code>. <code>appli</code> dispatches on argument <code>f</code>&apos;s first element.
      </p>
      <pre><code>(defn appli
&nbsp; [f x]
&nbsp; (condp = (first f)
&nbsp;   :a &quot;Hello, World!&quot;
&nbsp;   :b (inc x)
&nbsp;   :c (reverse x)))</code></pre>
      <p>
        For this demonstration, that <code>condp</code> serves as a kind of crude lookup table, but <code>appli</code> could be anything, such as a
        higher-order function, or a recursive function, etc.
      </p>
      <p>
        Now we test out what we&apos;ve done.
      </p>
      <pre><code>(appli Alice :an-ignored-arg) ;; =&gt; &quot;Hello, World!&quot;</code><br><br><code>(appli Bob 99) ;; =&gt; 100</code><br><br><code>(appli Charlie [:chocolate :strawberry :vanilla])
;; =&gt; (:vanilla :strawberry :chocolate)</code></pre>
      <p>
        That looks promising. <code>Alice</code> returns string <code>Hello, World!</code> regardless of the argument, <code>Bob</code> increments the numeric
        argument, and <code>Charlie</code> indeed reverses the sequence passed as the next argument.
      </p>
      <p>
        There&apos;s something we&apos;d like to improve: We wanted to think of <code>Alice</code>, <code>Bob</code>, and <code>Charlie</code> as functions, so
        having <code>appli</code> sprinkled throughout kinda destroys that illusion.
      </p>
      <p>
        Regular Clojure vectors <em>can</em> act as functions when at the head of an S-expression.
      </p>
      <pre><code>([97 98 99] 1) ;; =&gt; 98</code></pre>
      <p>
        We see that when a vector is at the head of the S-expression, there&apos;s an implied <code>nth</code>. It&apos;s as if we had written this.
      </p>
      <pre><code>(nth [97 98 99] 1) ;; =&gt; 98</code></pre>
      <p>
        For our Alice/Bob/Charlie trio, we&apos;d like for there to be instead an implied <code>appli</code>.
      </p>
      <p>
        To do that, let&apos;s introduce a new utility, <code>make-fn-thingy</code>.
      </p>
      <pre><code>(make-fn-thingy 97 98 99) ;; =&gt; [97 98 99]</code></pre>
      <p>
        Does it do vector tasks?
      </p>
      <pre><code>(count (make-fn-thingy 97 98 99)) ;; =&gt; 3</code><br><code>(nth (make-fn-thingy 97 98 99) 1) ;; =&gt; 98</code><br><code>(conj (make-fn-thingy 97 98 99) 100) ;; =&gt; [97 98 99 100]</code></pre>
      <p>
        That certainly <code>looks</code> like a vector. Each instance of a <em>fn-thingy</em> implements the vector interface, so we have all the familiar
        functions like <code>count</code>, <code>conj</code>, etc.
      </p>
      <p>
        Now, let&apos;s re-define the our trio using <code>make-fn-thingy</code>.
      </p>
      <pre><code>(def Alice (make-fn-thingy :a))</code><br><code>(def Bob (make-fn-thingy :b))</code><br><code>(def Charlie (make-fn-thingy :c))</code></pre>
      <p>
        Let&apos;s see what happens when we try to use a <em>fn-thingy</em> as a function.
      </p>
      <pre><code>(Alice 0) ;; =&gt; :a</code></pre>
      <p>
        Hmm. It appears to behave like a regular vector, with an implicit <code>nth</code>. This is the default.
      </p>
      <p>
        Next, we&apos;ll re-set the invocation behavior of all instances of <em>fn-thingy</em>s to <code>appli</code> using another utility,
        <code>defn-thingy</code>. It looks pretty much like <code>defn</code>.
      </p>
      <pre><code>(defn-thingy my-appli
&nbsp; &quot;doc string&quot;
&nbsp; {:metadata &quot;foo&quot;}
&nbsp; [f x]
&nbsp; (appli f x))</code></pre>
      <p>
        <code>defn-thingy</code> mutates the invocation behavior for all <code>fn-thingy</code> instances.
      </p>
      <p>
        Let&apos;s see how our trio behaves.
      </p>
      <pre><code>(Alice :an-ignored-arg) ;; =&gt; &quot;Hello, World!&quot;</code><br><br><code>(Bob 99) ;; =&gt; 100</code><br><br><code>(Charlie [:chocolate :strawberry :vanilla])
;; =&gt; (:vanilla :strawberry :chocolate)</code></pre>
      <p>
        Now our <code>Alice</code>, <code>Bob</code>, and <code>Charlie</code> thingys behave like functions.
      </p>
      <h3>
        Details
      </h3>
      <p>
        There are two steps to using <em>fn-thingy</em>: creating a <em>fn-thingy</em> instance, and assigning the invocation function.
      </p>
      <ol>
        <li>
          <p>
            <strong>Creating</strong> and manipulating a <em>fn-thingy</em> instance is analogous to that of vectors. To create, use
            <code>make-fn-thingy</code>
          </p>
          <pre><code>(make-fn-thingy 97 98 99) ;; =&gt; [97 98 99]</code></pre>
          <p>
            similar to using <code>vector</code>. (There is no analogous facility to making a vector literal.) Create a new instance from some other existing
            collection like this.
          </p>
          <pre><code>(into (make-fn-thingy) #{:foo :bar :baz}) ;; =&gt; [:baz :bar :foo]</code></pre>
          <p>
            Note that the type is distinct from a standard Clojure persistent vector.
          </p>
          <pre><code>(type (into (make-fn-thingy) #{:foo :bar :baz}))
;; =&gt; com.sagevisuals.AltFnInvocablePersistentVector</code></pre>
          <p>
            Manipulate an instance with your favorite tools.
          </p>
          <pre><code>(update (make-fn-thingy 97 98 99) 2 inc) ;; =&gt; [97 98 100]</code><br><br><code>(map inc (make-fn-thingy 97 98 99)) ;; =&gt; (98 99 100)</code></pre>
          <p>
            Note that, just like Clojure vectors, sequence functions consuming a <em>fn-thingy</em> instance will return a sequence.
          </p>
          <pre><code>(type (map inc (make-fn-thingy 97 98 99))) ;; =&gt; clojure.lang.LazySeq</code></pre>
        </li>
        <li>
          <p>
            <strong>Assigning</strong> a <em>fn-thingy</em> invocation function is analogous to using <code>defn</code>. One difference is that supplying a
            doc-string and metadata are required. (Feel free to leave them empty, though.)
          </p>
          <pre><code>(defn-thingy yippee
&nbsp; &quot;My docstring.&quot;
&nbsp; {:added 1.2}
&nbsp; [v x]
&nbsp; &quot;Hooray!&quot;)</code></pre>
          <p>
            This example assigns a 2-arity function that returns a string, ignoring its arguments. Observe.
          </p>
          <pre><code>((make-fn-thingy) 99) ;; =&gt; &quot;Hooray!&quot;</code><br><code>((make-fn-thingy :a :b :c) :foo) ;; =&gt; &quot;Hooray!&quot;</code><br><code>((make-fn-thingy 1.23 4.56) 22/7) ;; =&gt; &quot;Hooray!&quot;</code></pre>
          <p>
            The function is accessible in the same manner as any other var in the namespace.
          </p>
          <p>
            The name has no affect on the operation of <em>fn-thingy</em> instances, but is provided so that the function may be invoked manually, like this.
          </p>
          <pre><code>(yippee :a :b) ;; =&gt; &quot;Hooray!&quot;</code></pre>
          <p>
            Evaluating <code>defn-thingy</code> synchronously mutates the invocation function for all <em>fn-thingy</em> instances.
          </p>
          <p>
            The invocation function may have an arity of zero to eight, inclusive. When there is at least one argument, the <em>fn-thingy</em> instance is
            passed as the first argument.
          </p>
        </li>
      </ol>
    </section>
    <section id="examples">
      <h2>
        Examples
      </h2>
      <p>
        <a href="https://example.com">Eso-lang</a>: A fictitious, esoteric language.
      </p>
    </section>
    <section id="glossary">
      <h2>
        Glossary
      </h2>
      <dl>
        <dt id="fn-instance">
          fn-thingy instance
        </dt>
        <dd>
          <p>
            An instance of <code>com.sagevisuals.AltFnInvocableVector</code>. In nearly every regard, identical to an instance of
            <code>clojure.lang.PersistentVector</code>. Whereas a Clojure vector at the head of an S-expression behaves as if there is an implicit
            <code>nth</code>, a <em>fn-thingy</em> instance invokes a dynamically-mutable <a href="#inv-fn">invocation function</a>. (Defaults to
            <code>nth</code>.)
          </p>
          <p>
            Instantiated with <code>make-fn-thingy</code>.
          </p>
        </dd>
        <dt id="inv-fn">
          invocation function
        </dt>
        <dd>
          <p>
            The function that is invoked when evaluating an S-expression with a <a href="#fn-instance"><em>fn-thingy</em> instance</a> at the head. In all
            respects, a standard Clojure function, that may be called from any site.
          </p>
          <p>
            Defined with <code>defn-thingy</code>.
          </p>
        </dd>
      </dl>
    </section><br>
    <h2>
      License
    </h2>
    <p></p>
    <p>
      This program and the accompanying materials are made available under the terms of the <a href="https://opensource.org/license/MIT">MIT License</a>.
    </p>
    <p></p>
    <p id="page-footer">
      Copyright © 2024–2025 Brad Losavio.<br>
      Compiled by <a href="https://github.com/blosavio/readmoi">ReadMoi</a> on 2025 March 12.<span id="uuid"><br>
      0b7db4a2-ac01-40eb-bb9b-0646700b987e</span>
    </p>
  </body>
</html>
