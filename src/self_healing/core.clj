(ns self-healing.core
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            [self-healing.candidates :as candidates]
            [self-healing.healing :as healing]))


(s/def ::earnings (s/coll-of any?))
(s/def ::earnings-params (s/cat :elements ::earnings))

(s/def ::cleaned-earnings (s/with-gen
                            (s/coll-of number?)
                            #(gen/return [1 2 3 4 5])))
(s/def ::cleaned-earnings-params (s/cat :clean-elements ::cleaned-earnings))

(s/def ::average number?)
(s/def ::average-params (s/cat :elements ::average))
(s/def ::report-format string?)

(s/exercise ::cleaned-earnings 1)
;=> ([[1 2 3 4 5] [1 2 3 4 5]])

(defn clean-bad-data [earnings]
  (filter number? earnings))

(clean-bad-data [1 2 "cat" 3])
;=>(1 2 3)

(s/fdef clean-bad-data
        :args ::earnings-params
        :ret ::cleaned-earnings)

(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(s/fdef calc-average
        :args ::cleaned-earnings-params
        :ret ::average)

(defn display-report [avg]
  (str "The average is " avg))

(s/fdef display-report
        :args ::average-params
        :ret ::report-format)

(display-report 56)

(defn report [earnings]
  (-> earnings
      (clean-bad-data)
      (calc-average)
      (display-report)))


(s/fdef report
        :args ::earnings-params
        :ret ::report-format)

(report [1 2 3 4 5])
;=> "The average is 3"



(comment


(healing/with-healing (calc-average [1 2 3 4 5]))
(healing/with-healing (calc-average []))

(healing/with-healing (report [1 2 3 4 5 "a" "b"]))
;=>"The average is 3"

(healing/with-healing (report []))
;=>"The average is 0"

;; We also could have used the post fn comparisions for extra validation
 
  ;;; Worth noting that the divide by zero example would have been
  ;;; caught by using stest/check
(s/def ::cleaned-earnings (s/coll-of number?))
(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(stest/check `calc-average)

(healing/get-spec-data`calc-average)
)






