(ns invstr.core
  (:require [feedparser-clj.core :as feedparser]
            [clj-http.client :as http]
            [clj-http.cookies :as cookies]
            [hickory.core :as hickory]
            [hiccup-find.core :as hiccup-find]
            [clojure.pprint :as pprint]
            [clojure.string :as string]))


;;;; CONFIG


(def login-url "https://invstr.io/login-2/")


(def strats
  {:za-rookie "https://invstr.io/category/south-africa/za-rookie/feed/"
   :za-adventurer "https://invstr.io/category/south-africa/za-adventurer/feed/"
   :za-maverick "https://invstr.io/category/south-africa/za-maverick/feed/"})


;;;; FETCH


(defn latest-strat-url [feed-url]
  (->> feed-url
       feedparser/parse-feed
       :entries
       (sort-by :published-date)
       reverse
       first
       :link))


(defn login [{:keys [username password]}]
  (let [{:keys [status cookies]} (http/post login-url
                                            {:form-params
                                             {:log username
                                              :pwd password
                                              :rememberme false}})]
    (if (not= 302 status)
      (do
        (println (str "Login Status Should Be 302 but was " status))
        (System/exit 1))
      cookies)))


(defn pull-strategy [cookies strat-url]
  (->> (http/get strat-url {:cookies cookies})
       :body
       hickory/parse
       hickory/as-hiccup))


;;;; PARSE


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


(defn parse-breakdown-table [table]
  (let [headers (->> table
                     first
                     (map #(-> % (string/replace #"# " "") (string/replace #" " "-")  string/lower-case keyword)))
        rows (->> table
                  rest
                  (map
                   (fn [row]
                     (->> row
                          (map #(->> %
                                     second :value))))))]
    (->> rows
         (map #(zipmap headers %))
         (map #(dissoc % :ideal-value :shares)))))


(defn parse-title [hiccup]
  (->> hiccup
       (hiccup-find/hiccup-find [:h1])
       (filter (fn [[_ opt]]
                 (= "entry-title" (:class opt))))
       first
       last))


(defn parse-strategy [hiccup]
  {:title (parse-title hiccup)
   :asset-summary (get-table "assetSummary" hiccup)
   :breakdown (->> hiccup (get-table "breakdown") parse-breakdown-table)})


(defn write-file [strat-id result]
  (let [path (-> (str "data/" (name strat-id) "/" (:title result) ".edn")
                 (string/replace #":" ""))
        exists? (.exists (java.io.File. path))]
    (clojure.java.io/make-parents path)
    (when exists?
      (do (println)
          (println "** OLD ENTRY **")))
    (spit path (pr-str result))))


(defn print-result [result]
  (println)
  (println (:title result))
  (println)
  (doseq [[k v] (:asset-summary result)]
    (println (str k "  " v)))
  (pprint/print-table (:breakdown result)))


(defn process-latest [{:keys [username password strat-id] :as opts}]
  (let [strat-feed-url (get strats strat-id)
        cookies (login opts)
        result (->> strat-feed-url
                    latest-strat-url
                    (pull-strategy cookies)
                    parse-strategy)
        file-name (str "/" (name strat-id) "/" (:title result) ".edn")
        file (java.io.File. file-name)]
    (write-file strat-id result)
    (print-result result)
    (println)
    (println)))


(defn main-repl []
  (process-latest
   {:username "u"
    :password "p"
    :strat-id :za-rookie}))


(defn -main [& args]
  (->> args
       (partition 2)
       (map (fn [[k v]] [(keyword k) v]))
       (into {})
       (#(update % :strat-id keyword))
       process-latest))
