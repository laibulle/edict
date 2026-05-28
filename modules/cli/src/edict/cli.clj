(ns edict.cli
  (:require [babashka.cli :as cli]
            [edict.logic :as logic]))

(defn -main [& args]
  (let [opts (cli/parse-opts args {:alias {:n :name}})]
    (if (:name opts)
      (println (logic/greeting (:name opts)))
      (println "Usage: bb edict-cli --name <name>"))))
