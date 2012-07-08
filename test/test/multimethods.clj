(ns test.multimethods
  (:use [clojure.test :only (are deftest is testing)])
  (:require [multimethods :as mm]))

;; stolen from clojure.test-helper (in core test suite)
(defn set-var-roots
  [maplike]
  (doseq [[var val] maplike]
    (alter-var-root var (fn [_] val))))

(defn with-var-roots*
  "Temporarily set var roots, run block, then put original roots back."
  [root-map f & args]
  (let [originals (doall (map (fn [[var _]] [var @var]) root-map))]
    (set-var-roots root-map)
    (try
     (apply f args)
     (finally
      (set-var-roots originals)))))

(defmacro with-var-roots
  [root-map & body]
  `(with-var-roots* ~root-map (fn [] ~@body)))

;; actual tests
;; TODO: is this actually working?  The problem is that defmulti creates new
;; vars.  This means tests are not isolated?  clojure.core tests don't actually
;; test multimethods, just derive and friends.

(deftest create-multimethods
  (testing "We can create multimethods"
    (testing "with no special options"
      (is (mm/defmulti f (fn [x] x))))
    (testing "with a default dispatch val"
      (is (mm/defmulti f (fn [x] x) :default :catchall)))))

(deftest calling-multimethods
  (testing "We can call multimethods"
    (do
      (mm/defmulti f (fn [x] x))
      (mm/defmethod f 1 [x] :one)
      (mm/defmethod f 2 [x] :two)
      (testing "in head position"
        (is (= :one (f 1)))
        (is (= :two (f 2))))
      (testing "by applying them"
        (is (= :one (apply f [1])))
        (is (= :two (apply f [2])))))))
