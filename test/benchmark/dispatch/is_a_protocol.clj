(ns benchmark.dispatch.is-a-protocol
  (:use [clojure.test :only (are deftest is testing)]
        dispatch.is-a-protocol))

;; ensure that is-a? is extended to maps
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

(defmacro timeit [msg n & body] 
  `(println ~msg (with-out-str (time (dotimes [n# ~n] ~@body)))))

;;TODO  Should benchmarks really be tests?
(deftest benchmark-is-a
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
