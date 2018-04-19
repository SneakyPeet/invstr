(ns invstr.listed-companies
  (:require [invstr.util :as util]
            [hickory.core :as hickory]
            [hiccup-find.core :as hiccup-find]
            [clojure.string :as string]))

(def source-url "https://www.african-markets.com/en/stock-markets/jse/listed-companies")
(def output-file "data/companies.edn")

(defn refresh-company-list []
  (let [page (slurp source-url)
        table (->> page
                   hickory/parse
                   hickory/as-hiccup
                   (hiccup-find/hiccup-find [:table])
                   first
                   util/get-table-data)
        headers (->> table
                     first
                     (map #(-> % string/lower-case keyword)))
        rows  (->> table
                   rest
                   (map (fn [[name-anchor code sector]]
                          [(last name-anchor) code sector]))
                   (map #(zipmap headers %))
                   (map #(-> %
                             (assoc :full-name (:company %))
                             (update :company util/company-name))))]
    (try (set! *print-length* nil) (catch Exception e nil)) ;;Fixes things
    (println "Refreshed Company List with " (count rows) " Companies")
    (->> rows pr-str (spit output-file))))


(defn get-company-list []
  (->> (slurp output-file)
       read-string
       (map #(assoc %
                    :stockcode (:symbol %)
                    :symbol (:company %)))))



(def price-url "https://www.sharenet.co.za/v3/quickshare.php?scode=")


(defn stock-pricing [stock]
  (let [url (str price-url stock)
        tables (->> url
                    slurp
                    hickory/parse
                    hickory/as-hiccup
                    (hiccup-find/hiccup-find [:table]))
        price-table-number 0
        price-data (->> (nth tables price-table-number)
                        util/get-table-data)
        close-data (->> (nth tables (inc price-table-number))
                        util/get-table-data)]
    (->> (concat price-data close-data)
         (map (fn [[k v]]
                [(-> k
                     string/lower-case
                     (string/replace "'" "")
                     (string/replace " " "-")
                     keyword)
                 (read-string v)]))
         (into {})
         (merge {:stockcode stock})))
  )


(defn- test-stock-pricing [n]
  (let [url (str price-url "mrp")
        tables (->> url
                    slurp
                    hickory/parse
                    hickory/as-hiccup
                    (hiccup-find/hiccup-find [:table]))
        price-table-number n
        price-data (->> (nth tables price-table-number)
                        util/get-table-data)
        close-data (->> (nth tables (inc price-table-number))
                        util/get-table-data)]
    (prn price-data)
    (prn "--------")
    (prn close-data))
  )

;(test-stock-pricing 0)
