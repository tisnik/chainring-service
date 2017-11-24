(ns chainring-service.server-test
  (:require [clojure.test :refer :all]
            [chainring-service.server :refer :all]))

;
; Common functions used by tests.
;

(defn callable?
    "Test if given function-name is bound to the real function."
    [function-name]
    (clojure.test/function? function-name))

;
; Tests for functions existence
;
