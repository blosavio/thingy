(defproject com.sagevisuals/thingy "0-SNAPSHOT0"
  :description "A Clojure library that provides a bizarre function definition"
  :url "https://github.com/blosavio/thingy"
  :license {:name "MIT License"
            :url "https://opensource.org/license/mit"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.12.0"]]
  :repl-options {:init-ns thingy.core}
  :profiles {:dev {:dependencies [[com.sagevisuals/chlog "1"]
                                  [com.sagevisuals/readmoi "3"]]
                   :plugins [[dev.weavejester/lein-cljfmt "0.12.0"]
                             [lein-codox "0.10.8"]]}
             :repl {}}
  :codox {:metadata {:doc/format :markdown}
          :namespaces [#"^thingy\.(?!scratch)(?!tree-demo)"]
          :target-path "doc"
          :output-path "doc"
          :source-uri "https://github.com/blosavio/thingy/blob/main/{filepath}#L{line}"
          :html {:transforms [[:div.sidebar.primary] [:append [:ul.index-link [:li.depth-1 [:a {:href "https://github.com/blosavio/thingy"} "Project Home"]]]]]}
          :project {:name "thingy" :version "version 0-SNAPSHOT0"}}
  :scm {:name "git" :url "https://github.com/blosavio/thingy"})
