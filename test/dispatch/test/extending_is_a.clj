(ns dispatch.test.extending-is-a
  (:use [clojure.test :only (are deftest is testing)]
        [dispatch.is-a-protocol :only [is-a? Is-A]])
  (:require [dispatch.multimethods :as mm]))

;; (defn- map-is-a? [child parent h]
;;   (and (map? parent)
;;        (let [cks (set (keys child)) pks (set (keys parent))]
;;          ;; child keys more specific than parent's 
;;          (and (clojure.set/subset? pks cks) 
;;               (every? #(is-a? h (child %) (parent %)) pks))))) ;;TODO hmm...
(defn- map-is-a? [child parent h]
  (and (map? parent)
       (let [sentinel (Object.)]
         (every? 
           (fn [[pk pv]] 
             (let [cv (get child pk sentinel)]
               (if (identical? cv sentinel) 
                 false
                 (is-a? h cv pv)))) 
           parent))))

(extend-protocol Is-A
  clojure.lang.IPersistentMap
  (-is-a? [c p h] (map-is-a? c p h)))

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

