(ns self-healing.core
  (:require [clojure.spec :as s]
            [clojure.spec.test :as stest]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            [self-healing.candidates :as candidates]
            [self-healing.healing :as healing]))


(s/def ::earnings (s/cat :elements (s/coll-of any?)))

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
        :ret ::cleaned-earnings)

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

(comment


(healing/with-healing (calc-average [1 2 3 4 5]))
(healing/with-healing (calc-average []))

(healing/with-healing (report [1 2 3 4 5 "a" "b"]))

(healing/with-healing (report []))

;; We also could have used the post fn comparisions for extra validation
 
  ;;; Worth noting that the divide by zero example would have been caught by using stest/check
(defn calc-average [earnings]
  (/ (apply + earnings) (count earnings)))

(stest/check `calc-average)

(healing/get-spec-data`calc-average)
)






