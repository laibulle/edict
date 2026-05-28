(ns user
  (:require [clojure.test :as test]
            [edict.logic :as logic]
            [edict.cli :as cli]))

(defn help []
  (println "Welcome to Edict Dev REPL!")
  (println "Available namespaces: edict.logic, edict.cli")
  (println "Try: (logic/greeting \"Developer\")"))

(defn run-tests []
  (test/run-all-tests #"edict.*"))

(println "user.clj loaded. Call (help) for more info.")
