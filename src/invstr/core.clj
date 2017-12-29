(ns invstr.core
  (:gen-class)
  (:require [invstr.strategies :as strategies]
            [invstr.balance :as balance]))



(defn main [{:keys [username password strat-id balance] :as opts}]
  (prn "== fetch strats")
  (let [strategy (strategies/process-latest opts)]
    (strategies/print-result strategy)
    (when balance
      (do
        (prn "== balancing")
        (let [result (balance/balance strategy)]
          (balance/print-balance-result result))))))


(defn main-repl []
  (main
   {:username "u"
    :password "p"
    :strat-id :za-rookie
    :balance false}))

;(main-repl)

(defn -main [& args]
  (->> args
       (partition 2)
       (map (fn [[k v]] [(keyword k) v]))
       (into {})
       (#(update % :strat-id keyword))
       (#(update % :balance (fn [x] (Boolean/valueOf x))))
       main))
