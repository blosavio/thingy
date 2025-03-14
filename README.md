
  <body>
    <a href="https://clojars.org/com.sagevisuals/thingy"><img src="https://img.shields.io/clojars/v/com.sagevisuals/thingy.svg"></a><br>
    <a href="#setup">Setup</a><br>
    <a href="https://blosavio.github.io/thingy/index.html">API</a><br>
    <a href="https://github.com/blosavio/thingy/blob/main/changelog.md">Changelog</a><br>
    <a href="#intro">Introduction</a><br>
    <a href="#usage">Usage</a><br>
    <a href="#examples">Examples</a><br>
    <a href="#glossary">Glossary</a><br>
    <a href="https://github.com/blosavio">Contact</a><br>
    <h1>
      thingy
    </h1><em>A Clojure library that provides a bizarre function definition mechanism</em><br>
    <section id="setup">
      <h2>
        Setup
      </h2>
      <h3>
        Leiningen/Boot
      </h3>
      <pre><code>[com.sagevisuals/thingy &quot;1&quot;]</code></pre>
      <h3>
        Clojure CLI/deps.edn
      </h3>
      <pre><code>com.sagevisuals/thingy {:mvn/version &quot;1&quot;}</code></pre>
      <h3>
        Require
      </h3>
      <pre><code>(require &apos;[thingy.core :refer [assign-thingy-fn! make-thingy]])</code></pre>
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
        A <em>thingy</em> instance, when appearing at the head of an S-expression serves as a function, consuming the arguments contained in the tail and
        yielding a value. Unlike a typical function, the internal structure of a <em>thingy</em> is manipulatable with all the functions that make up the
        vector interface. Such objects may be used in <a href="#examples">academic programming languages</a> that explore using sequential collections of
        values to define functions.
      </p>
      <p>
        Sure, it&apos;s possible to write
      </p>
      <pre><code>(application-function vector arg1 arg2...)</code></pre>
      <p>
        where <code>application-function</code> provides some kind of logic about how to interpret <code>vector</code>. But having the symbol
        &quot;application-function&quot; scattered around is visually distracting. Instead of plainly representing the concepts, the machinery is leaking onto
        the page.
      </p>
      <p>
        It is much nicer to write
      </p>
      <pre><code>(fn-vec arg1 arg2...)</code></pre>
      <p>
        as well to read and to understand: <code>fn-vec</code> is the operator. The <em>thingy</em> library enables such streamlining.
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
        Let&apos;s write a function, <code>application</code>, which dispatches on argument <code>f</code>&apos;s first element.
      </p>
      <pre><code>(defn application
&nbsp; [f x]
&nbsp; (condp = (first f)
&nbsp;   :a &quot;Hello, World!&quot;
&nbsp;   :b (inc x)
&nbsp;   :c (reverse x)))</code></pre>
      <p>
        For this demonstration, that <code>condp</code> serves as a kind of crude lookup table, but <code>application</code> could be anything, such as a
        higher-order function, or a recursive function, etc.
      </p>
      <p>
        Now we test out what we&apos;ve done.
      </p>
      <pre><code>(application Alice :an-ignored-arg) ;; =&gt; &quot;Hello, World!&quot;</code><br><br><code>(application Bob 99) ;; =&gt; 100</code><br><br><code>(application Charlie [:chocolate :strawberry :vanilla])
;; =&gt; (:vanilla :strawberry :chocolate)</code></pre>
      <p>
        That looks promising. <code>Alice</code> returns string <code>Hello, World!</code> regardless of the argument, <code>Bob</code> increments the numeric
        argument, and <code>Charlie</code> indeed reverses the sequence passed as the next argument.
      </p>
      <p>
        There&apos;s something we&apos;d like to improve: We wanted to think of <code>Alice</code>, <code>Bob</code>, and <code>Charlie</code> as functions, so
        having the symbol &quot;application&quot; sprinkled throughout kinda destroys that illusion.
      </p>
      <p>
        Perhaps we could leverage the fact that regular Clojure vectors have the capability to act as functions when at the head of an S-expression.
      </p>
      <pre><code>([97 98 99] 1) ;; =&gt; 98</code></pre>
      <p>
        We see that when a vector is at the head of the S-expression, there&apos;s an implied <code>nth</code>. It&apos;s as if we had written this.
      </p>
      <pre><code>(nth [97 98 99] 1) ;; =&gt; 98</code></pre>
      <p>
        For our Alice/Bob/Charlie trio, we&apos;d like for there to be instead an implied <code>application</code>.
      </p>
      <p>
        To do that, let&apos;s introduce a new utility, <code>make-thingy</code>, which creates a vector-like object.
      </p>
      <pre><code>(make-thingy 97 98 99) ;; =&gt; [97 98 99]</code></pre>
      <p>
        Does it do vector tasks?
      </p>
      <pre><code>(count (make-thingy 97 98 99)) ;; =&gt; 3</code><br><code>(nth (make-thingy 97 98 99) 1) ;; =&gt; 98</code><br><code>(conj (make-thingy 97 98 99) 100) ;; =&gt; [97 98 99 100]</code></pre>
      <p>
        That certainly <em>looks</em> like a vector. Each instance of a <em>thingy</em> implements the vector interface, so we have all the familiar functions
        like <code>count</code>, <code>conj</code>, etc.
      </p>
      <p>
        Now, let&apos;s re-define the our trio using <code>make-thingy</code>.
      </p>
      <pre><code>(def Alice (make-thingy :a))</code><br><code>(def Bob (make-thingy :b))</code><br><code>(def Charlie (make-thingy :c))</code></pre>
      <p>
        Let&apos;s see what happens when we try to use a <em>thingy</em> as a function.
      </p>
      <pre><code>(Alice 0) ;; =&gt; :a</code></pre>
      <p>
        Hmm. It appears to behave like a regular vector, with an implicit <code>nth</code>. This is, in fact, the default.
      </p>
      <p>
        Next, we&apos;ll re-set the invocation behavior of all instances of <em>thingy</em>s to <code>application</code> using another utility,
        <code>assign-thingy-fn!</code>.
      </p>
      <pre><code>(assign-thingy-fn! application)
;; =&gt; {:fn application,
;;     :left-delimiter &quot;[&quot;,
;;     :right-delimiter &quot;]&quot;}</code></pre>
      <p>
        <code>assign-thingy-fn!</code> mutates the invocation behavior for all <em>thingy</em> instances.
      </p>
      <p>
        Let&apos;s see how our trio behaves.
      </p>
      <pre><code>(Alice :an-ignored-arg) ;; =&gt; &quot;Hello, World!&quot;</code><br><br><code>(Bob 99) ;; =&gt; 100</code><br><br><code>(Charlie [:chocolate :strawberry :vanilla])
;; =&gt; (:vanilla :strawberry :chocolate)</code></pre>
      <p>
        Now our <code>Alice</code>, <code>Bob</code>, and <code>Charlie</code> <em>thingy</em>s behave like functions.
      </p>
      <h3>
        Details
      </h3>
      <p>
        There are three steps to using the <em>thingy</em> library: creating an instance, defining an invocation function, and assigning that invocation
        function.
      </p>
      <ol>
        <li>
          <p>
            <strong>Creating</strong> and manipulating a <em>thingy</em> instance is analogous to that of vectors. To create, use <code>make-thingy</code>
          </p>
          <pre><code>(make-thingy 97 98 99) ;; =&gt; [97 98 99]</code></pre>
          <p>
            similar to using <code>clojure.core/vector</code>. (There is no analogous facility to making a vector literal.) Create a new instance from some
            other existing collection like this.
          </p>
          <pre><code>(into (make-thingy) #{:foo :bar :baz}) ;; =&gt; [:baz :bar :foo]</code></pre>
          <p>
            Note that the type is distinct from a standard Clojure persistent vector.
          </p>
          <pre><code>(type (into (make-thingy) #{:foo :bar :baz}))
;; =&gt; com.sagevisuals.AltFnInvocablePersistentVector</code></pre>
          <p>
            Manipulate an instance with your favorite tools.
          </p>
          <pre><code>(update (make-thingy 97 98 99) 2 inc) ;; =&gt; [97 98 100]</code><br><br><code>(map inc (make-thingy 97 98 99)) ;; =&gt; (98 99 100)</code></pre>
          <p>
            Note that, just like Clojure vectors, sequence functions consuming a <em>thingy</em> instance will return a sequence.
          </p>
          <pre><code>(type (map inc (make-thingy 97 98 99))) ;; =&gt; clojure.lang.LazySeq</code></pre>
        </li>
        <li>
          <p>
            <strong>Defining</strong> a <em>thingy</em> invocation function is merely typical Clojure function definition, e.g., using <code>defn</code>.
          </p>
          <pre><code>(defn yippee
&nbsp;  [_ _]
&nbsp;  &quot;Hooray!&quot;)</code></pre>
          <p>
            This example defines a 2-arity function that returns a string, ignoring its arguments. Observe.
          </p>
          <pre><code>(yippee (make-thingy 1.23 4.56) 22/7) ;; =&gt; &quot;Hooray!&quot;</code></pre>
          <p>
            The invocation function must have an arity of zero to nine. When there is at least one argument, the <em>thingy</em> instance is passed as the
            first argument, followed by up to eight trailing arguments.
          </p>
        </li>
        <li>
          <p>
            <strong>Assigning</strong> a <em>thingy</em> invocation functions uses <code>assign-thingy-fn!</code>.
          </p>
          <pre><code>(assign-thingy-fn! yippee)</code></pre>
          <p>
            Evaluating <code>assign-thingy-fn!</code> synchronously mutates the invocation function for all <em>thingy</em> instances.
          </p>
          <p>
            Now, any <em>thingy</em> instance, when at the head of an S-expression, will implicitly invoke <code>yippee</code>.
          </p>
          <pre><code>(def X (make-thingy 1 2 3))</code><br><br><code>(X :an-ignored-arg) ;; =&gt; &quot;Hooray!&quot;</code></pre>
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
        <dt id="instance">
          thingy instance
        </dt>
        <dd>
          <p>
            An instance of <code>com.sagevisuals.AltFnInvocableVector</code>. In nearly every regard, identical to an instance of
            <code>clojure.lang.PersistentVector</code>. Whereas a Clojure vector at the head of an S-expression behaves as if there is an implicit
            <code>nth</code>, a <em>thingy</em> instance invokes a dynamically-mutable <a href="#inv-fn">invocation function</a>. (Defaults to
            <code>nth</code>.)
          </p>
          <p>
            Instantiated with <code>make-thingy</code>.
          </p>
        </dd>
        <dt id="inv-fn">
          invocation function
        </dt>
        <dd>
          <p>
            The function that is invoked when evaluating an S-expression with a <a href="#instance"><em>thingy</em> instance</a> at the head. In all respects,
            a standard Clojure function, that may be called from any site.
          </p>
          <p>
            Defined with regular Clojure machinery, i.e., <code>defn</code>.
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
      Compiled by <a href="https://github.com/blosavio/readmoi">ReadMoi</a> on 2025 March 14.<span id="uuid"><br>
      0b7db4a2-ac01-40eb-bb9b-0646700b987e</span>
    </p>
  </body>
</html>
