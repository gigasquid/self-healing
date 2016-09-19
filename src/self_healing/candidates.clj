(ns self-healing.candidates
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]))

(s/def ::numbers (s/cat :elements (s/coll-of number?)))
(s/def ::result number?)
(s/def ::result-string string?)

(defn better-calc-average [earnings]
  (if (empty? earnings)
    0
    (/ (apply + earnings) (count earnings))))

(s/fdef better-calc-average
        :args ::numbers
        :ret ::result)

(defn bad-calc-average [earnings]
  (if (empty? earnings)
    0
    (first earnings)))

(s/fdef bad-calc-average
        :args ::numbers
        :ret ::result)

(defn bad-calc-average2 [earnings]
  (str "This is a string "
       (if (empty? earnings)
         0
         (/ (apply + earnings) (count earnings)))))

(s/fdef bad-calc-average2
        :args ::numbers
        :ret ::result-string)

(defn adder [n]
  (+ n 5))

(s/fdef adder
        :args ::result
        :ret ::result)
