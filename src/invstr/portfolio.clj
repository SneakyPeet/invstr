(ns invstr.portfolio
  (:require [invstr.util :as util]
            [hickory.core :as hickory]
            [hiccup-find.core :as hiccup-find]
            [clojure.string :as string]))

(def portfolio-file "data/portfolio.html")


(defn- parse-row [row]
  (-> row
      (update :profit read-string)
      (update :volume read-string)
      (update :market-price read-string)
      (update :position read-string)
      (update :price read-string)
      (update :swap read-string)
      (update :symbol util/company-name)))



(defn parse-portfolio []
  (let [page    (slurp portfolio-file :encoding "UTF-16")
        table   (->> page
                     hickory/parse
                     hickory/as-hiccup
                     (hiccup-find/hiccup-find [:table])
                     first
                     util/get-table-data)
        data    (->> table
                     (filter #(= 12 (count %))))
        balance (->> table
                     (filter #(= "Balance:" (first %)))
                     first second last read-string
                     (* 100)
                     int)
        headers (->> data
                     first
                     (map #(-> %
                               last
                               (string/replace " / " "-")
                               (string/replace " " "-")
                               string/lower-case
                               keyword)))
        rows    (->> data
                     rest
                     (map #(zipmap headers %))
                     (map parse-row)
                     (map #(assoc % :portfolio-balance balance)))]

    {:stocks rows
     :balance balance}))
