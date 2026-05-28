(ns edict.logic-test
  (:require [clojure.test :refer [deftest is testing]]
            [edict.logic :as logic]))

(deftest greeting-test
  (testing "Greeting logic"
    (is (= "Hello, Alice! This is shared logic." (logic/greeting "Alice")))))
