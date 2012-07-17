(ns dispatch.is-a-protocol
  (:require clojure.set))

(declare is-a?)

;;TODO: should dispatch on parent instead, probably
(defprotocol Is-A
  "A protocol to implement extensible clojure.core/isa?
  Note funny argument order so we can dispatch on parent."
  (-is-a? [parent child] [parent child hierarchy] "see clojure.core/isa?"))

(def global-is-a-hierarchy (clojure.core/make-hierarchy))

(defn search-hierarchy-is-a [p c h]
  (contains? ((:ancestors h) c) p))

(defn- nil-is-a [p c h] (nil? c))

(defn- class-is-a [p c h]
  (or
   (and (class? c) (. ^Class p isAssignableFrom c))
   (search-hierarchy-is-a p c h)
   (boolean (some #(contains? ((:ancestors h) %) p) (supers c)))))

;;TODO: core/derive and core/isa? work for derive'd kws _and_ symbols
;; (clojure.lang.Named).  Maybe extend to that instead
(defn- keyword-is-a [p c h]
  (search-hierarchy-is-a p c h))

(extend-protocol Is-A
  nil
  (-is-a? [p c h] (nil-is-a p c h))
  Class
  (-is-a? [parent child h]
    (class-is-a parent child h))
  clojure.lang.Keyword  ;;the 'Object' case technically takes care of this case
  (-is-a? [parent child h]
    (keyword-is-a parent child h))
  clojure.lang.IPersistentVector
  (-is-a? [p c h]
    (and (vector? c)
         (= (count p) (count c))
         (loop [ret true i 0]
           (if (or (not ret) (= i (count p)))
             ret
             (recur (is-a? h (c i) (p i)) (inc i))))));;TODO hmm...
  Object  ;;TODO  need this?
  (-is-a? [p c h] (search-hierarchy-is-a p c h)))

(defn is-a?
  "Similar to clojure.core/isa?, but can be extended to new types
  via the Is-A protocol."
  ([child parent] (is-a? global-is-a-hierarchy child parent))
  ([hierarchy child parent]
     (or (= child parent)
         (-is-a? parent child hierarchy))))

(defn derive*
  "Same as clojure.core/derive, with a different default global hierarchy."
  ([child parent]
   (assert (namespace parent))
   (assert (or (class? child)
               (and (instance? clojure.lang.Named child) (namespace child))))


     (alter-var-root #'global-is-a-hierarchy derive* child parent)
     nil)
  ([h child parent] (clojure.core/derive h child parent)))
