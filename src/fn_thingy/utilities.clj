(ns fn-thingy.utilities)


(defn scaffold
  "Prints the public methods of a interface/class `iface`. Adapted from
  Daniel Gregoire's [GitHub](gist https://gist.github.com/semperos/3835392)."
  {:UUIDv4 (random-uuid)}
  [iface]
  (doseq [[iface methods] (->> iface .getMethods 
                               (map #(vector (.getName (.getDeclaringClass %)) 
                                             (symbol (.getName %))
                                             (count (.getParameterTypes %))))
                               (group-by first))]
    (println (str ";;  " iface))
    (doseq [[_ name argcount] (sort methods)]
      (println 
       (str ";;    " 
            (list name (into ['this] (take argcount (repeatedly gensym)))))))))


(defn print-n-newlines
  "Print `n` newlines to `*out*`."
  {:UUIDv4 (random-uuid)}
  [n]
  (doseq [nline (repeat n "\n")]
    (println nline)))
