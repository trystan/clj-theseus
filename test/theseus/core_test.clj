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

(deftest requirements
  (is (= true
         (compatable-requirements { :a 1 } { :a 1 })))

  (is (= true
         (compatable-requirements { :a 1 } { :b 2 })))

  (is (= false
         (compatable-requirements { :a 1 } { :a 2 }))))

(deftest walking-all-actions
  (is (= [[{ :from :a :to :b } { :from :b :to :c }] [{ :from :a :to :c }]]
         (paths [{ :from :a :to :b }
                 { :from :b :to :c }
                 { :from :b :to :d }
                 { :from :y :to :z }
                 { :from :a :to :c }]
                :a :c)))

  (testing "allow compatable requirements"
    (is (= [[{ :from :a :to :b :requires { :foo true } } { :from :b :to :c :requires { :bar true } }]]
           (paths [{ :from :a :to :b :requires { :foo true } }
                   { :from :b :to :c :requires { :bar true } }]
                  :a :c))))

  (testing "allow matching requirements"
    (is (= [[{ :from :a :to :b :requires { :foo true } } { :from :b :to :c :requires { :foo true } }]]
           (paths [{ :from :a :to :b :requires { :foo true } }
                   { :from :b :to :c :requires { :foo true } }]
                  :a :c))))

  (testing "avoid incompatable requirements"
    (is (= [[{ :from :a :to :b :requires { :foo true } }]]
           (paths [{ :from :a :to :b :requires { :foo true } }
                   { :from :b :to :c :requires { :foo false } }]
                  :a :c)))))

(deftest before-all
  (is (= [[{ :before :all :a 1 } { :before :all :b 1 } { :from :a :to :b } { :from :b :to :c }]
          [{ :before :all :a 1 } { :before :all :b 1 } { :from :a :to :c }]]
         (let [facts [{ :from :a :to :b }
                      { :from :b :to :c }
                      { :from :b :to :d }
                      { :before :all :a 1 }
                      { :from :y :to :z }
                      { :before :all :b 1 }
                      { :from :a :to :c }]]
           (map (partial add-ancillary facts ) (paths facts :a :c))))))

(deftest before-each
  (is (= [[{ :before :each :a 1 } { :before :each :b 1 } { :from :a :to :b }
           { :before :each :a 1 } { :before :each :b 1 } { :from :b :to :c }]
          [{ :before :each :a 1 } { :before :each :b 1 } { :from :a :to :c }]]
         (let [facts [{ :from :a :to :b }
                      { :from :b :to :c }
                      { :from :b :to :d }
                      { :before :each :a 1 }
                      { :from :y :to :z }
                      { :before :each :b 1 }
                      { :from :a :to :c }]]
           (map (partial add-ancillary facts ) (paths facts :a :c))))))

(deftest after-all
  (is (= [[{ :from :a :to :b } { :from :b :to :c } { :after :all :a 1 } { :after :all :b 1 }]
          [{ :from :a :to :c } { :after :all :a 1 } { :after :all :b 1 }]]
         (let [facts [{ :from :a :to :b }
                 { :from :b :to :c }
                 { :from :b :to :d }
                 { :after :all :a 1 }
                 { :from :y :to :z }
                 { :after :all :b 1 }
                 { :from :a :to :c }]]
           (map (partial add-ancillary facts ) (paths facts :a :c))))))

(deftest after-each
  (is (= [[{ :from :a :to :b } { :after :each :a 1 } { :after :each :b 1 }
           { :from :b :to :c } { :after :each :a 1 } { :after :each :b 1 }]
          [{ :from :a :to :c } { :after :each :a 1 } { :after :each :b 1 }]]
         (let [facts [{ :from :a :to :b }
                      { :from :b :to :c }
                      { :from :b :to :d }
                      { :after :each :a 1 }
                      { :from :y :to :z }
                      { :after :each :b 1 }
                      { :from :a :to :c }]]
           (map (partial add-ancillary facts ) (paths facts :a :c))))))

(deftest before-id
  (is (= [[{ :from :a :to :b } { :before :pizza :a 1 } { :from :b :to :c :id :pizza }] [{ :from :a :to :c }]]
         (let [facts [{ :from :a :to :b }
                      { :from :b :to :c :id :pizza }
                      { :from :b :to :d }
                      { :before :pizza :a 1 }
                      { :from :y :to :z }
                      { :from :a :to :c }]]
           (map (partial add-ancillary facts ) (paths facts :a :c))))))

(deftest after-id
  (is (= [[{ :from :a :to :b :id :pizza } { :after :pizza :a 1 } { :from :b :to :c }] [{ :from :a :to :c }]]
         (let [facts [{ :from :a :to :b :id :pizza }
                      { :from :b :to :c }
                      { :from :b :to :d }
                      { :after :pizza :a 1 }
                      { :from :y :to :z }
                      { :from :a :to :c }]]
           (map (partial add-ancillary facts ) (paths facts :a :c))))))

(def verify-counter (atom 0))

(deftest running-actions
  (testing "runs fn"
    (is (= { :first? true :second? true }
           (run [{ :id :a :fn #(assoc % :first? true) }
                 { :something :else }
                 { :id :b :fn #(assoc % :second? true) }]))))

  (testing "runs verify"
    (let [before @verify-counter
          result (run [{ :id :a :fn #(assoc % :first? true) }
                       { :verify (fn [state] (swap! verify-counter inc)) }
                       { :id :b :fn #(assoc % :second? true) }])
          after @verify-counter]
      (is (= { :first? true :second? true }
             result))
      (is (= (inc before)
             after))))

  (testing "merges :requires"
    (is (= { :a 1 :b 2}
           (run [{ :requires { :a 1 }} { :requires { :b 2 } }])))))

(run-tests)
