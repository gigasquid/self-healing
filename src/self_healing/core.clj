(ns self-healing.core
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [clojure.string :as string]
            [self-healing.candidates :as candidates]
            [camel-snake-kebab.core :as csk]))

(defn clean-bad-data [earnings]
  (filter int? earnings))

(s/def ::earnings (s/cat :elements (s/coll-of any?)))
(s/def ::cleaned-earnings (s/cat :clean-elements (s/coll-of int?)))
(s/def ::average int?)
(s/def ::report-format string?)

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
;=>{:args :self-healing.core/cleaned-earnings, :ret clojure.core/int?, :fn nil}

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
  (and (s/valid? args1 input)
       (s/valid? args2 input)))

(defn spec-return-match? [ret1 ret2 failing-input candidate]
  (let [result (try (apply (resolve candidate) failing-input) (catch Exception e :failed))]
    (and (not= :failed result)
         (s/valid? ret1 result)
         (s/valid? ret2 result))))

(defn spec-matching? [orig-fspec failing-input candidate]
  (let [{:keys [args ret fn]} (get-spec-data candidate)]
    (and (spec-inputs-match? args (:args orig-fspec) failing-input)
         (spec-return-match? ret (:ret orig-fspec) failing-input candidate))))

(defn find-spec-candidate-match [fname {:keys [args ret fn] :as fspec-data} failing-input]
  (let [candidates (->> (s/registry)
                        keys
                        (filter #(string/starts-with? (namespace %) "self-healing.candidates"))
                        (filter symbol?))]
    (some #(if (spec-matching? fspec-data failing-input %) %) candidates)))


(try
  (let [input []]
    (try
      (report input)
      (catch Exception e
        (let [fname (failing-function-name e)
              fspec-data (get-spec-data (symbol fname))
              match (find-spec-candidate-match fname fspec-data [[]])]
          (if match
            (do
              (println "Found a matching candidate replacement for failing function" fname " for input" input)
              (println "Replacing with candidate match" match)
              (println "----------")
              (eval  `(def ~(symbol fname) ~match))
              (println "Calling function again")
              (let [new-result (report input)]
                (println "Healed function result is:" (report input))
                new-result))
            (println "No suitable replacment for failing function "  fname " with input " input ":(")))))))


(comment
 
  ;;; Worth notint that the divide by zero example would have been caught by using stest/check
(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))
  (stest/check `calc-average)
  )






