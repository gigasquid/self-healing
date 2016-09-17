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

(s/fdef clean-bad-data
        :args ::earnings
        :ret ::cleaned-earnings
        :fn #(>= (count (->  % :elements)) (count (-> % :ret :clean-elements))))

(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(s/fdef calc-average
        :args ::cleaned-earnings
        :ret int?)

(defn display-report [avg]
  (str "The average is " avg))

(s/fdef display-report
        :args int?
        :ret string?)

(display-report 56)

(stest/instrument `clean-bad-data)
(stest/instrument `calc-average)


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

(try (report [])
     (catch Exception e
       (let [fname (failing-function-name e)
             _ (println :fname fname)
             fspec-data (get-spec-data (symbol fname))
             _ (println :fspec-data fspec-data)]
         fspec-data)))

(defn spec-inputs-match [])

(defn spec-matching? [orig-fspec failing-input candidate]
  (println :orig-fspec orig-fspec :failing-input failing-input :candidate candidate)
  (let [{:keys [args ret fn]} (get-spec-data candidate)]
    (println "trying to match " args "with " failing-input "-->"     (s/valid? args failing-input))
    (and (s/valid? args failing-input)
         (s/valid? (:args orig-fspec) failing-input))))

(defn find-spec-candidate-match [fname {:keys [args ret fn] :as fspec-data} failing-input]
  (let [candidates (->> (s/registry)
                        keys
                        (filter #(string/starts-with? (namespace %) "self-healing.candidates"))
                        (filter symbol?))]
    (map #(spec-matching? fspec-data failing-input %) candidates)))


(find-spec-candidate-match "self-healing.core/calc-average"
                           {:args :self-healing.core/cleaned-earnings, :ret clojure.core/int?, :fn nil}
                           [[]])




