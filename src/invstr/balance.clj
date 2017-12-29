(ns invstr.balance
  (:require [invstr.listed-companies :as listed-companies]
            [invstr.portfolio :as portfolio]
            [invstr.strategies :as strategies]
            [clojure.pprint :as pprint]))


(defn- calculate-stocks [stockcode balance owned-volume allocation price]
  (let [fees-percentage 0.1
        owned-value     (* owned-volume price)
        value           (int (/ (* balance allocation) 100))
        trade-value     (- value owned-value)
        trade-volume    (double (/ trade-value price))
        trade-value-f   (* 100 (/ trade-value (+ 100 fees-percentage)))
        trade-volume-f  (double (/ trade-value-f price))
        action-volume   (long trade-volume-f)
        action-value    (* price action-volume)]
    {:stockcode    stockcode
     :allocation   allocation
     :price        price
     :value        value
     :trade-value  trade-value
     :trade-volume trade-volume
     :trade-value-with-fees trade-value-f
     :trade-volume-with-fees trade-volume-f
     :action-volume action-volume
     :action-value action-value
     :owned-volume owned-volume
     :owned-value  owned-value
     :owned-allocation (* 100 (/ owned-value balance))
     :action (cond (> action-volume 0) "BUY"
                   (< action-volume 0) "SELL"
                   :else "")}))


(defmulti balance-stock
  (fn [balance current-portfolio strategy stock-pricing]
    (let [stockcode     (:stockcode stock-pricing)
          in-portfolio? (contains? current-portfolio stockcode)
          in-strategy?  (contains? strategy stockcode)]
      (cond
        (and in-portfolio? (not in-strategy?)) :sell
        (and in-strategy? (not in-portfolio?)) :buy-new
        (and in-portfolio? in-strategy?)       :balance
        :else                                  :invalid-stock))))


(defmethod balance-stock :sell
  [balance
   current-portfolio _
   {:keys [stockcode sale]}]
  (let [{:keys [volume]} (get current-portfolio stockcode)
        values           (calculate-stocks stockcode balance volume 0 sale)]
    (-> values
        (assoc :action "SELL ALL"
               :action-volume (:trade-volume values)
               :action-value (:trade-value values)))))


(defmethod balance-stock :buy-new
  [balance _
   strategy
   {:keys [stockcode sale]}]
  (let [{:keys [allocation]} (get strategy stockcode)]
    (-> (calculate-stocks stockcode balance 0 allocation sale)
        (assoc :action "BUY NEW"))))


(defmethod balance-stock :balance
  [balance current-portfolio strategy
   {:keys [stockcode sale]}]
  (let [{:keys [allocation]} (get strategy stockcode)
        {:keys [volume]} (get current-portfolio stockcode)]
    (calculate-stocks stockcode balance volume allocation sale)))


(defn currency [s] (format "R %1.2f" (float (/ s 100))))


(defn f-percentage [s] (format "%1.1f%%" (float s)))


(defn volume [s]  (format "%1.2f" (float s)))


(defn price [s] (str s "c"))


(defn print-balance-result [{:keys [balance balanced-portfolio
                                    owned-value expected-value
                                    owned-exposure expected-exposure]}]
  (let [print-values (->> balanced-portfolio
                          (map #(-> %
                                    (update :trade-value currency)
                                    (update :value currency)
                                    (update :owned-value currency)
                                    (update :action-value currency)
                                    (update :owned-allocation f-percentage)
                                    (update :trade-volume volume)
                                    (update :price price)
                                    (update :allocation f-percentage)
                                    (update :trade-volume-with-fees volume)
                                    (update :trade-value-with-fees currency))))]
    (println)
    (println "BALANCING")
    (pprint/print-table
     [{:f "PORTFOLIO" :value (currency balance) :exposure ""}
      {:f "OWNED" :value (currency owned-value)
       :exposure (f-percentage owned-exposure)}
      {:f "EXPECTED" :value (currency expected-value)
       :exposure (f-percentage expected-exposure)}])
    (println)
    (println "ACTIONS")
    (->> print-values
         (map #(-> %
                   (assoc :code (:stockcode %)
                          :volume (:action-volume %)
                          :value  (:action-value %))))
         (filter #(not (empty? (:action %))))
         (sort-by :code)
         (pprint/print-table [:stockcode :action :volume :value :allocation :price]))
    (println)
    (println "OWNED")
    (->> print-values
         (map #(-> %
                   (assoc :volume (:owned-volume %)
                          :value (:owned-value %)
                          :allocation (:owned-allocation %)
                          :code (:stockcode %))))
         (filter #(> (:volume %) 0))
         (sort-by :code)
         (pprint/print-table [:code :name :volume :value :allocation]))
    (println)
    (println "CALCS")
    (->> print-values
         (sort-by :stockcode)
         (map #(-> %
                   (assoc :c (:stockcode %)
                          :e-val (:value %)
                          :e-al (:allocation %)
                          :o-val (:owned-value %)
                          :o-vol (:owned-volume %)
                          :o-al (:owned-allocation %)
                          :t-val (:trade-value %)
                          :t-vol (:trade-volume %)
                          :f-val (:trade-value-with-fees %)
                          :f-vol (:trade-volume-with-fees %)
                          :a (:action %))))
         (pprint/print-table [:c :a
                              :e-val :e-al
                              :o-val :o-vol :o-al
                              :t-val :t-vol
                              :f-val :f-vol]))))


(defn balance [strategy]
  (listed-companies/refresh-company-list)
  (let [companies           (listed-companies/get-company-list)
        companies-by-name   (->> companies
                                 (map (fn [{:keys [company] :as v}]
                                        [company v]))
                                 (into {}))
        companies-by-stockcode (->> companies
                                    (map (fn [{:keys [stockcode] :as v}]
                                           [stockcode v]))
                                    (into {}))
        strategy            (->> strategy
                                 :breakdown
                                 (map #(update % :allocation read-string))
                                 (map (fn [{:keys [stockcode] :as v}]
                                        [stockcode
                                         (merge
                                          v
                                          (get companies-by-stockcode stockcode))]))
                                 (into {}))
        {:keys [balance stocks]} (portfolio/parse-portfolio)
        current-portfolio   (->> stocks
                                 (map (fn [{:keys [symbol] :as v}]
                                        (let [detail (get companies-by-name symbol)]
                                          [(:stockcode detail)
                                           (merge
                                            v
                                            detail)])))
                                 (into {}))
        all-stocks-in-play (->> (concat (keys strategy) (keys current-portfolio))
                                set
                                sort)
        stock-pricings      (map listed-companies/stock-pricing all-stocks-in-play)
        balanced-portfolio (->> stock-pricings
                                (map #(balance-stock balance current-portfolio strategy %))
                                (map #(assoc % :name (->> (:stockcode %)
                                                          (get companies-by-stockcode)
                                                          :full-name))))
        owned-value (->> balanced-portfolio
                         (map :owned-value)
                         (reduce + 0))
        expected-value (->> balanced-portfolio
                            (map :value)
                            (reduce + 0))]
    {:balance balance
     :balanced-portfolio balanced-portfolio
     :owned-value owned-value
     :expected-value expected-value
     :owned-exposure (* 100 (/ owned-value balance))
     :expected-exposure (* 100 (/ expected-value balance))}))
