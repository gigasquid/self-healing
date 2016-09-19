(ns self-healing.healing
  (:require [clojure.spec :as s]
            [clojure.string :as string]))

(defn get-spec-data [spec-symb]
  (let [[_ _ args _ ret _ fn] (s/form spec-symb)]
       {:args args
        :ret ret
        :fn fn}))

(defn failing-function-name [e]
  (as-> (.getStackTrace e) ?
    (map #(.getClassName %) ?)
    (filter #(string/starts-with? % "self_healing.core") ?)
    (first ?)
    (string/split ? #"\$")
    (last ?)
    (string/replace ? #"_" "-")
    (str *ns* "/" ?)))

(defn spec-inputs-match? [args1 args2 input]
  (println "****Comparing args" args1 args2 "with input" input)
  (and (s/valid? args1 input)
       (s/valid? args2 input)))

(defn- try-fn [f input]
  (try (apply f input) (catch Exception e :failed)))

(defn spec-return-match? [fname c-fspec orig-fspec failing-input candidate]
  (let [rcandidate (resolve candidate)
        orig-fn (resolve (symbol fname))
        result-new (try-fn rcandidate failing-input)
        [[seed]] (s/exercise (:args orig-fspec) 1)
        result-old-seed (try-fn rcandidate seed)
        result-new-seed (try-fn orig-fn seed)]
    (println "****Comparing seed " seed "with new function")
    (println "****Result: old" result-old-seed "new" result-new-seed)
    (and (not= :failed result-new)
         (s/valid? (:ret c-fspec) result-new)
         (s/valid? (:ret orig-fspec) result-new)
         (= result-old-seed result-new-seed))))

(defn spec-matching? [fname orig-fspec failing-input candidate]
  (println "----------")
  (println "**Looking at candidate " candidate)
  (let [c-fspec (get-spec-data candidate)]
    (and (spec-inputs-match? (:args c-fspec) (:args orig-fspec) failing-input)
         (spec-return-match? fname c-fspec orig-fspec  failing-input candidate))))

(defn find-spec-candidate-match [fname fspec-data failing-input]
  (let [candidates (->> (s/registry)
                        keys
                        (filter #(string/starts-with? (namespace %) "self-healing.candidates"))
                        (filter symbol?))]
    (println "Checking candidates " candidates)
    (some #(if (spec-matching? fname fspec-data failing-input %) %) (shuffle candidates))))


(defn self-heal [e input orig-form]
  (let [fname (failing-function-name e)
        _ (println "ERROR in function" fname (.getMessage e) "-- looking for replacement")
        fspec-data (get-spec-data (symbol fname))
        _ (println "Retriving spec information for function " fspec-data)
        match (find-spec-candidate-match fname fspec-data [input])]
    (if match
      (do
        (println "Found a matching candidate replacement for failing function" fname " for input" input)
        (println "Replacing with candidate match" match)
        (println "----------")
        (eval `(def ~(symbol fname) ~match))
        (println "Calling function again")
        (let [new-result (eval orig-form)]
          (println "Healed function result is:" new-result)
          new-result))
      (println "No suitable replacment for failing function "  fname " with input " input ":("))))

(defmacro with-healing [body]
  (let [params (second body)]
    `(try ~body
          (catch Exception e# (self-heal e# ~params '~body)))))


