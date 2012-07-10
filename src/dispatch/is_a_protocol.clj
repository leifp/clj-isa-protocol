(ns dispatch.is-a-protocol
  (:require clojure.set))

(declare is-a?)

;;TODO: should dispatch on parent instead, probably
(defprotocol Is-A "A protocol to implement extensible clojure.core/isa?"
  (-is-a? [child parent] [child parent hierarchy] "see clojure.core/isa?"))

(def global-is-a-hierarchy (clojure.core/make-hierarchy))

;; (defn general-is-a [c p h]
;;   (or (= c p)
;;       (contains? ((:ancestors h) c) p)))
(defn- general-is-a [c p h]
  (contains? ((:ancestors h) c) p))

(defn- nil-is-a [c p h] (nil? p))

(defn- class-is-a [c p h]
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

(defn is-a? 
  "Similar to clojure.core/isa?, but can be extended to new types
  via the Is-A protocol."
  ([child parent] (is-a? global-is-a-hierarchy child parent))
  ([hierarchy child parent]
     (or (= child parent)
         (-is-a? child parent hierarchy))))

(defn derive* 
  "Same as clojure.core/derive, with a different default global hierarchy."
  ([child parent]
     (alter-var-root #'global-is-a-hierarchy derive* child parent)
     nil)
  ([h child parent] (clojure.core/derive h child parent)))

