(ns dispatch.test.is-a-protocol
  (:use [clojure.test :only (are deftest is testing)]
        dispatch.is-a-protocol))

(deftest nil-is-a?
  (testing "nil"
    (testing "is-a? itself"
      (is (true? (is-a? nil nil))))
    (testing "is-a? not anything else"
      (are [x] (not (or (is-a? nil x) (is-a? x nil)))
        0 0.0 "str" ::kw Class Object
        () [] {} #{} (sorted-set)))))

(deftest classes-is-a?
  (testing "Classes:"
    (testing "a class is-a? member of its superclasses"
      (are [c] (every? true? (map #(is-a? c %) (supers c)))
        Integer Number BigInteger Thread
        clojure.lang.IPersistentMap clojure.lang.IFn
        clojure.lang.APersistentMap clojure.lang.AFn)
      (are [c] (every? true? (map #(is-a? c %) (ancestors c)))
        Integer Number BigInteger Thread
        clojure.lang.IPersistentMap clojure.lang.IFn
        clojure.lang.APersistentMap clojure.lang.AFn))
    (testing "is-a? <class> <obj> is false for <obj> not a class or keyword"
      (are [obj] (false? (is-a? Integer obj))
        0 1 1.0 "str" [] [1] {} #{}))))

(deftest equal-is-a?
  (testing "= implies is-a?"
    (testing "so everything is-a? itself"
      (are [x] (true? (is-a? x x))
          Integer clojure.lang.AFn
          clojure.lang.IFn java.lang.Comparable
          0 0.0 "str" ::kw Class Object
          () '(1) [] [1] {} #{} (sorted-set)))
    (testing "even for things that cause errors otherwise,
             like keywords with no namespace"
      (is (true? (is-a? :no-ns :no-ns))))))

(deftest vectors-is-a?
  (testing "vectors are compared elementwise"
    (testing "all corresponding elements should be is-a?"
      (are [v w] (true? (is-a? v w))
        [] []
        [nil] [nil]
        [1 2 3] [1 2 3]
        [1 Float] [1 Number]
        [Float 1] [Number 1]
        [Integer Float] [Number Number])
      (are [v w] (false? (is-a? v w))
        [] [1]
        [1] [1 2]
        [1] [1 2 3]
        [1 Number] [1 Float] 
        [Number 1] [Float 1] 
        [Number Number] [Integer Float]
        [Integer String] [Number Number]
        [Integer Float String] [Number Number Number]
        [Integer String Float Double] [Number Number Number Number]))))
        
(deftest seq-is-a?
  (testing "Seqs don't have the same semantics as vectors."
    (testing "Their elements are not compared by is-a?"
      (are [s t] (not (or (is-a? s t) (is-a? t s)))
        '(Float) '(Number) 
        '(Float Double) '(Number Number)
        '(1 Float) '(1 Number)))
    (testing "They must be exactly identical.")
      (are [s t] (and (is-a? s t) (is-a? t s))
        () ()
        '([1]) '([1])
        '(Float) '(Float)
        '(Float Double) '(Float Double))))

;;TODO kw and hierarchies and such
