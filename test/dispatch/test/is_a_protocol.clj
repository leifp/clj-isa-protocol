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

(deftest maps-is-a?
  (testing "Maps:"
    (testing "identical"
      (is (true? (is-a? {} {})))
      (is (true? (is-a? {:ans 42} {:ans 42})))
      (is (true? (is-a? {:ans 42 :q :?} {:ans 42 :q :?}))))
    (testing "empty map is not a child of any other map"
      (is (false? (is-a? {} {:a 1})))
      (is (false? (is-a? {} {:a 1 :b 2})))
      (is (false? (is-a? {} {:a 1 :b 2 :c 3}))))
    (testing "A more specific map is-a? instance of a more general one"
      (are [spec gen] (true? (is-a? spec gen))
        {:a 1 :b 2 :c 3}  {}
        {:a 1 :b 2 :c 3}  {:a 1}
        {:a 1 :b 2}       {:b 2}
        {:a 1 :b 2 :c 3}  {:a 1 :b 2})
      (is (false? (is-a? {:a 1 :b 2} {:a 1 :b 2 :c 3}))))
    (testing "The values of a map are compared recursively"
      (are [x y] (true? (is-a? x y))
        {:a Integer}         {:a Number}
        {:a [Integer Float]} {:a [Number Number]}
        {:a {:b Integer}}    {:a {:b Number}})
      (are [x y] (false? (is-a? x y))
        {:a Integer}          {:a String}
        {:a Float}            {:a Integer}
        {:a [Integer String]} {:a [Number Number]}
        {:a {:b String}}      {:a {:b Number}}))
    (testing "Maps are not comparable to other types"
      (are [m o] (not (or (is-a? m o) (is-a? o m)))
        {} 1
        {} ::kw
        {} []
        {0 :a 1 :b} [:a :b]
        {:k :v} [[:k :v]]))))

;;benchmarks
(defmacro timeit [msg n & body] 
  `(println ~msg (with-out-str (time (dotimes [n# ~n] ~@body)))))

(deftest benchmarks
  (is 
    (do
      (timeit "isa? zero" 10000000 (isa? 0 0))
      (timeit "is-a? zero" 10000000 (is-a? 0 0))
      (timeit "isa? nil" 10000000 (isa? nil nil))
      (timeit "is-a? nil" 10000000 (is-a? nil nil))
      (timeit "isa? class" 5000000 (isa? Float Number))
      (timeit "is-a? class" 5000000 (is-a? Float Number))
      (timeit "isa? deep class" 5000000 
              (isa? clojure.lang.PersistentHashMap Object))
      (timeit "is-a? deep class" 5000000 
              (is-a? clojure.lang.PersistentHashMap Object))
      (timeit "isa? vector" 500000 (isa? [1 2 3 Float] [1 2 3 Number]))
      (timeit "is-a? vector" 500000 (is-a? [1 2 3 Float] [1 2 3 Number]))
      (timeit "isa? vector standing in for a map" 100000 
              (isa? [:a :b Float] [:a :b Number]))
      (timeit "isa? map" 100000 
              (is-a? {0 :a 1 :b 2 Float} {0 :a 1 :b 2 Number}))
      true)))
;;TODO kw and hierarchies and such
