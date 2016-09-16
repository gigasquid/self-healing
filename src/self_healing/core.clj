(ns self-healing.core
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [clojure.string :as string]))


(defn clean-bad-data [earnings]
  (filter int? earnings))

(s/def ::earnings (s/cat :elements (s/coll-of any?)))
(s/def ::cleaned-earnings (s/cat :clean-elements (s/coll-of int?)))

(s/fdef clean-bad-data
        :args ::earnings
        :ret ::cleaned-earnings
        :fn #(>= (count (->  % :elements)) (count (-> % :ret :clean-elements))))

(stest/instrument `clean-bad-data)

(clean-bad-data [2 3 4 "boo"])
;=>(2 3 4)

(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(calc-average [1 2 3 4 5])
;=> 3

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

(defn failing-function)

(try (report [])
     (catch Exception e (def x (.getStackTrace e))))

(defn failing-function-name [e]
  (as-> (.getStackTrace e) ?
    (map #(.getClassName %) ?)
    (filter #(string/starts-with? % "self_healing.core") ?)
    (first ?)
    (string/split ? #"\$")
    (last ?)))

(try (report [])
     (catch Exception e (failing-function-name e)))


(map #(.getClassName %) x)
(first (filter #(string/starts-with? (.getClassName %)  "self_healing.core") x))

(string/starts-with? "self_healing$core" "self_healing")

(s/describe `clean-bad-data)
;=>(fspec :args (cat :earnings (coll-of any?)) :ret (cat
;:cleaned-earnings (coll-of int?)) :fn (>= (count (-> % :earnings))
;(count (-> % :ret :cleaned-earnings))))

(s/conform (eval (nth (s/form `clean-bad-data) 2)) [[1 2 3]])

(defn get-spec-data [spec-symb]
  (let [[_ _ args _ ret _ fn] (s/form spec-symb)]
       {:args args
        :ret ret
        :fn fn}))

(get-spec-data `calc-average)
;=>{:args :self-healing.core/cleaned-earnings, :ret clojure.core/int?, :fn nil}



