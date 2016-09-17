(ns self-healing.candidates
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]))

(s/def ::numbers (s/cat :elements (s/coll-of int?)))
(s/def ::result int?)

(defn better-calc-average [earnings]
  (if (seq earnings)
    (/ (apply + earnings) (count earnings))
    0))

(s/fdef better-calc-average
        :args ::numbers
        :ret ::result)


(defn adder [n]
  (+ n 5))

(s/fdef adder
        :args ::result
        :ret ::result)
