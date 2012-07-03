(ns is-a-protocol
  (:require clojure.set))

(declare is-a?)

(defprotocol Is-A
  (-is-a? [child parent] [child parent hierarchy]))

(def global-is-a-hierarchy (clojure.core/make-hierarchy))

;; (defn general-is-a [c p h]
;;   (or (= c p)
;;       (contains? ((:ancestors h) c) p)))
(defn general-is-a [c p h]
  (contains? ((:ancestors h) c) p))

(defn nil-is-a [c p h] (nil? p))
(defn class-is-a [c p h]
  (or
   (and (class? p) (. ^Class p isAssignableFrom c))
   (some #(contains? ((:ancestors h) %) p) (supers c))
   (general-is-a c p h)))

(extend-protocol Is-A
  nil
  (-is-a? [c p h] (nil-is-a c p h))
  Object
  (-is-a? [c p h] (general-is-a c p h))
  Class
  (-is-a? [child parent h]
    (and (class? parent)
         (. ^Class parent isAssignableFrom child)))
  clojure.lang.IPersistentVector
  (-is-a? [c p h]
    (and (vector? p)
         (= (count p) (count c))
         (loop [ret true i 0]
           (if (or (not ret) (= i (count p)))
             ret
             (recur (is-a? h (c i) (p i)) (inc i))))))) ;;TODO hmm...

(defn map-is-a? [child parent h]
  (and (map? parent)
       (let [cks (set (keys child)) pks (set (keys parent))]
         ;; child keys more specific than parent's 
         (and (clojure.set/subset? pks cks) 
              (every? #(is-a? h (child %) (parent %)) pks))))) ;;TODO hmm...

(extend-protocol Is-A
  clojure.lang.IPersistentMap
  (-is-a? [c p h] (map-is-a? c p h)))

(defn is-a?
  ([child parent] (is-a? global-is-a-hierarchy child parent))
  ([hierarchy child parent]
     (or (= child parent)
         (-is-a? child parent hierarchy))))

(defn derive*
  ([child parent]
     (alter-var-root #'global-is-a-hierarchy derive* child parent)
     nil)
  ([h child parent] (clojure.core/derive h child parent)))

;;(is-a? {:a 1 :b 2 :c 3} {:a 1 :b 2})
;;(is-a? {:a 1 :b 2} {:a 1 :b 2 :c 3})

