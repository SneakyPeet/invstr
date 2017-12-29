(ns invstr.util
  (:require [hiccup-find.core :as hiccup-find]
            [clojure.string :as string]))


(defn get-table-data
  ([table-hiccup]
   (get-table-data [table-hiccup] []))
  ([elements result]
   (cond
     ;result
     (empty? elements) (filter #(not (empty? %)) result)
     ;parents
     (contains? #{:table :tbody :thead} (first (first elements)))
     (let [children (->> elements
                         first
                         (drop 2)
                         (filter #(not (string? %))))]
       (recur (concat (rest elements) children) result))
     ;rows
     (= :tr (first (first elements)))
     (let [children (->> elements
                         first
                         (drop 2)
                         (filter #(not (string? %))))]
       (recur (concat children (rest elements)) (concat result [[]])))
     ;cells
     (contains? #{:td :th} (first (first elements)))
     (let [cell (->> elements
                     first
                     (drop 2)
                     (map #(if (string? %) (string/trim %) %))
                     (filter #(not (empty? %)))
                     first)
           last-row (concat (last result) [cell])
           result (concat (drop-last result) [last-row])]
       (recur (rest elements) result)))))


(defn get-table [class hiccup]
  (->> hiccup
       (hiccup-find/hiccup-find [:table])
       (filter (fn [[_ opts]]
                 (= class (get opts :class))))
       first
       get-table-data))


(defn company-name [s]
  (-> s
      string/lower-case
      (string/replace "-n-" "")
      (string/replace "limited" "ltd")
      (string/replace "holdings" "hldgs")
      (string/replace " " "")
      (string/replace "glencoreplc" "glencorexstrataplc")
      string/trim))
