(ns dispatch.test.extending-is-a
  (:use [clojure.test :only (are deftest is testing)]
        [dispatch.is-a-protocol :only [is-a? Is-A]])
  (:require [dispatch.multimethods :as mm]))

(defn- map-is-a? [parent child h]
  (and (map? child)
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
  (-is-a? [p c h] (map-is-a? p c h)))

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

(mm/defmulti handler "multimethod ring handler" (fn [req] req))
(mm/defmethod handler {}
  [req] {:status 404 :headers {} :body "Not found."})
(mm/defmethod handler {:request-method :get}
  [req] {:status 200 :headers {} :body "Generic GET."})
(mm/defmethod handler {:request-method :get, :content-type "application/json"}
  [req] {:status 200 :headers {} :body "\"GET some json.\""})
(mm/defmethod handler {:request-method :get, :content-type "application/xml"}
  [req] {:status 200 :headers {} :body "<foo>GET some XML.</foo>"})
(mm/defmethod handler {:request-method :get,
                       :content-type "application/json"
                       :headers {"app/client-status" "favored-nation"}}
  [req] {:status 200 :headers {} :body "\"GET some super-special json.\""})

(deftest multimethod-with-map-dispatch-val
  (testing "Multimethods dispatched on maps"
    (are [req resp] (= (handler req) resp)
         {:request-method :get}
         {:status 200, :headers {}, :body "Generic GET."}

         {:request-method :post}
         {:status 404, :headers {}, :body "Not found."}

         {:request-method :get :content-type "application/xml"}
         {:status 200, :headers {}, :body "<foo>GET some XML.</foo>"}

         {:request-method :get :content-type "application/json"}
         {:status 200, :headers {}, :body "\"GET some json.\""}

         {:request-method :get :headers {"anything" "really"}
          :content-type "application/json"}
         {:status 200, :headers {}, :body "\"GET some json.\""}

         {:request-method :get
          :headers {"anything" "really", "app/client-status" "favored-nation"}
          :content-type "application/json"}
         {:status 200, :headers {}, :body "\"GET some super-special json.\""})))

;; all right, let's get crazy.

(defn- fn-is-a? [parent-fn child h] (boolean (parent-fn child)))

(extend-protocol Is-A
  clojure.lang.AFunction
  (-is-a? [p c h]
    (fn-is-a? p c h)))

(defn- pattern-is-a? [parent-patt child h]
  (and (string? child)
       (boolean (re-matches parent-patt child))))

(extend-protocol Is-A
  java.util.regex.Pattern
  (-is-a? [p c h]
    (pattern-is-a? p c h)))

(deftest function-is-a?
  (testing "clojure.lang.AFunction's:"
    (testing "identical"
      (is (true? (is-a? number? number?)))
      (is (false? (is-a? float? number?)))) ;; no logical implication, sorry :)
    (testing "is the parent applied to the child truthy?"
      (is (true? (is-a? 1 number?)))
      (is (true? (is-a? 1.0 number?)))
      (is (true? (is-a? 1M number?)))
      (is (true? (is-a? "" string?)))
      (is (true? (is-a? [1] seq)))

      (is (false? (is-a? [] seq)))
      (is (false? (is-a? () seq)))
      (is (false? (is-a? {} seq)))
      (is (false? (is-a? #{} seq))))
    (testing "even anonymous fns work."
      (is (true? (is-a? [:kw "str" 1] ;; first number is a 1?
                        #(if-let [x (first (filter number? %))] (= x 1)))))
      (is (false? (is-a? [:kw 2 "str" 1] ;; first number is a 1?
                         #(if-let [x (first (filter number? %))] (= x 1))))))))

(deftest regex-is-a?
  (testing "java.util.regex.Pattern's:"
    (testing "identical"
      (is (false? (is-a? #"" #"")))        ; equality is not guaranteed; we
      (is (false? (is-a? #"foo" #"foo")))  ; could fix the fn above to compare
      (is (false? (is-a? #"bar" #"foo")))) ; them, if desired
    (testing "does the parent 're-matches' the child?"
      (is (true? (is-a? "" #"")))
      (is (true? (is-a? "foo" #"foo")))
      (is (true? (is-a? "food" #"foo.*")))
      (is (false? (is-a? "x" #"")))
      (is (false? (is-a? "food" #"foo")))
      (is (false? (is-a? "for" #"foo.*"))))))

(deftest crazy-is-a?
  (testing "any crazy combination we want"
    (is (true? (is-a? {:a 1       :b "foo"    :c Double :d "exact" :e :other}
                      {:a number? :b #"foo.*" :c Number :d "exact"})))
    (is (false? (is-a? {:a 1       :b "foo"    :c Double :d "exact" :e :other}
                       {:a number? :b #"foo.+" :c Number :d "exact"})))))
