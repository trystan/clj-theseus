(ns theseus.core-test
  (:require [clojure.test :refer :all]
            [theseus.core :refer :all]))

(deftest stepping
  (testing "no further steps"
    (is (= [[{ :from :a :to :b }]]
           (steps [[{ :from :a :to :b }]] [{ :from :y :to :z }]))))

  (testing "one further step"
    (is (= [[{ :from :a :to :b } { :from :b :to :c }]]
           (steps [[{ :from :a :to :b }]] [{ :from :b :to :c }]))))

  (testing "many further steps")
    (is (= [[{ :from :a :to :b } { :from :b :to :c }] [{ :from :a :to :b } { :from :b :to :d }]]
           (steps [[{ :from :a :to :b }]] [{ :from :b :to :c } { :from :b :to :d }])))

  (testing "avoiding infinite loops"
    (is (= [[{ :from :a :to :b } { :from :b :to :a } { :from :a :to :c }]]
           (steps [[{ :from :a :to :b } { :from :b :to :a }]] [{ :from :a :to :b } { :from :a :to :c }])))))


(deftest walking-all-actions
  (is (= [[{ :from :a :to :b } { :from :b :to :c }] [{ :from :a :to :c }]]
         (paths [{ :from :a :to :b }
                 { :from :b :to :c }
                 { :from :b :to :d }
                 { :from :y :to :z }
                 { :from :a :to :c }]
                :a :c))))

(deftest running-actions
  (is (= { :first? true :second? true }
         (run [{ :id :a :fn #(assoc % :first? true) }
               { :id :b :fn #(assoc % :second? true) }]))))

(run-tests)
