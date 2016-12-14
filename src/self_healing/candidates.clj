(ns self-healing.candidates
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]))

(s/def ::numbers (s/coll-of number?))
(s/def ::numbers-params (s/cat :elements ::numbers))
(s/def ::result number?)
(s/def ::result-string string?)
(s/def ::result-params (s/cat :elements ::result))

(defn better-calc-average [earnings]
  (if (empty? earnings)
    0
    (/ (apply + earnings) (count earnings))))

(s/fdef better-calc-average
        :args ::numbers-params
        :ret ::result)

(defn bad-calc-average [earnings]
  (if (empty? earnings)
    0
    (first earnings)))

(s/fdef bad-calc-average
        :args ::numbers-params
        :ret ::result)

(defn bad-calc-average2 [earnings]
  (str "This is a string "
       (if (empty? earnings)
         0
         (/ (apply + earnings) (count earnings)))))

(s/fdef bad-calc-average2
        :args ::numbers-params
        :ret ::result-string)

(defn adder [n]
  (+ n 5))

(s/fdef adder
        :args ::result-params
        :ret ::result)
