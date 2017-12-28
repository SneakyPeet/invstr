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
                   (map #(zipmap headers %)))]
    (println "Refreshed Company List with " (count rows) " Companies")
    (->> rows pr-str (spit output-file))))


(defn get-company-list []
  (slurp output-file))
