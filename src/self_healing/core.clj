(ns self-healing.core
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            [clojure.walk :as walk]
            [self-healing.candidates :as candidates]
            [camel-snake-kebab.core :as csk]))


(s/def ::earnings (s/with-gen
                    (s/cat :elements (s/coll-of any?))
                    #(gen/return [[1 2 3 4 5 "cat" "dog"]])))
(s/def ::cleaned-earnings (s/with-gen
                            (s/cat :clean-elements (s/coll-of number?))
                            #(gen/return [[1 2 3 4 5]])))
(s/def ::average number?)
(s/def ::report-format string?)

(s/exercise ::cleaned-earnings 1)

(defn clean-bad-data [earnings]
  (filter number? earnings))

(s/fdef clean-bad-data
        :args ::earnings
        :ret ::cleaned-earnings
        :fn #(>= (count (->  % :elements)) (count (-> % :ret :clean-elements))))

(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(s/fdef calc-average
        :args ::cleaned-earnings
        :ret ::average)

(defn display-report [avg]
  (str "The average is " avg))

(s/fdef display-report
        :args ::average
        :ret ::report-format)

(display-report 56)

(defn report [earnings]
  (-> earnings
      (clean-bad-data)
      (calc-average)
      (display-report)))


(s/fdef report
        :args ::earnings
        :ret string?)

(report [1 2 3 4 5]) ;=> "The average is 3"


;;; error recovers

(defn get-spec-data [spec-symb]
  (let [[_ _ args _ ret _ fn] (s/form spec-symb)]
       {:args args
        :ret ret
        :fn fn}))

(get-spec-data`calc-average)
;=>{:args :self-healing.core/cleaned-earnings, :ret clojure.core/number?, :fn nil}

(defn failing-function-name [e]
  (as-> (.getStackTrace e) ?
    (map #(.getClassName %) ?)
    (filter #(string/starts-with? % "self_healing.core") ?)
    (first ?)
    (string/split ? #"\$")
    (last ?)
    (csk/->kebab-case ?)
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


(defn self-heal [e input]
  (let [fname (failing-function-name e)
        _ (println "ERROR in function" fname "-- looking for replacement")
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
        (let [new-result (report input)]
          (println "Healed function result is:" (calc-average input))
          new-result))
      (println "No suitable replacment for failing function "  fname " with input " input ":("))))

(defmacro with-healing [body]
  `(try
     (let [params# ~(second body)]
       (try ~body
            (catch Exception e# (self-heal e# params#))))))


(comment


(with-healing (calc-average [1 2 3 4 5]))
(with-healing (calc-average []))

(with-healing (report [1 2 3 4 5 "a" "b"]))

(with-healing (report []))
 
  ;;; Worth notint that the divide by zero example would have been caught by using stest/check
(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(stest/check `calc-average)
)






