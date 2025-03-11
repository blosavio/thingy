(ns readmoi-generator
  "Script to load options and perform actions.

  CIDER eval buffer C-c C-k generates an html page and a markdown chunk."
  {:no-doc true}
  (:require
   [hiccup2.core :refer [raw]]
   [readmoi.core :refer [*project-group*
                         *project-name*
                         *project-version*
                         *wrap-at*
                         generate-all
                         prettyfy
                         print-form-then-eval]]))


(def project-metadata (read-string (slurp "project.clj")))
(def readmoi-options (load-file "resources/readmoi_options.edn"))


(generate-all project-metadata readmoi-options)


(defn -main
  [& args]
  {:UUIDv4 #uuid "60f00c64-6480-42df-9181-3048da80db73"}
  (println "generated fn-thingy ReadMe docs"))


;;;; utilities

(defn trim-evaluation
  "Remove trailing `;; => ...` stuff from a `print-form-then-eval` code block."
  {:UUIDv4 #uuid "09c46784-dcc4-45a3-b997-e75799d8babe"}
  [b]
  (let [splits (clojure.string/split-lines (nth b 1))
        trimmed (take-while #(not= ";; =>" (subs % 0 5)) splits)
        adjusted-str (clojure.string/join "\n" trimmed)]
    (assoc b 1 adjusted-str)))


(defn newline-and-indent-after-chars
  "Update a `print-form-then-eval` code block by inserting a newline, and `n` spaces after characters in `chars`."
  {:UUIDv4 #uuid "eda4200c-cb05-4061-96e5-a913010edc1f"}
  [b n chars]
  (let [adjusted-str (reduce #(clojure.string/replace %1 %2 (str %2 "\n"  (clojure.string/join (repeat n " ")))) (nth b 1) chars)]
    (assoc b 1 adjusted-str)))