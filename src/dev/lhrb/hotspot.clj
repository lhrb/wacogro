(ns dev.lhrb.hotspot
  (:require
   [oz.core :as oz]
   [dev.lhrb.change-frequency :refer [change-frequency]]
   [dev.lhrb.complexity-metric :refer [analyse-java-files]]
   [dev.lhrb.viz.hotspot :refer [hotspot-chart]]
   [clojure.string :as str]))

(defn assoc-path [m]
  (let [path (str/split (get m :url) #"/")]
    (assoc m :path path)))

(defn- create-url [path]
  (str/join "/" path))

(defn- parent-path [path]
 (loop [rest-path path
        acc []]
   (let [[head & tail] rest-path]
    (if (nil? (first tail))
      (conj acc {:name head :parent nil :url head})
      (recur tail (conj acc {:name head
                             :parent (create-url (reverse tail))
                             :url (create-url (reverse rest-path))}))))))

(defn- create-hierachy [elem]
   (let [path (-> elem :path reverse)
         leaf (merge
               (select-keys elem [:url :revisions :code :authors])
               {:name (first path)
               :parent (create-url (reverse (rest path)))})]
     (if (nil? (:parent leaf))
       [leaf]
       (conj (parent-path (rest path)) leaf))))

(defn normalize-revs
  "normalize revisions to the interval [0,1]"
  [files]
  (let [revs (remove nil? (map :revisions files))
        max (reduce max revs)
        min (reduce min revs)
        normalize (fn [x] (double (/ (- x min) (- max min))))]
   (->> files
        (map (fn [{:keys [revisions] :as m}]
               (if revisions
                 (update m :revisions normalize)
                 m))))))

(defn url-code-revisions
  [change-freq m]
 (let [name (get m :name)
       revisions (get change-freq name)]
   (if revisions
     {:url name
      :code (get m :lines)
      :revisions revisions}
     {:url name
      :code (get m :lines)})))

(defn prepare-for-hotspot-chart
  [change-frequency complexity-metric]
  (->> complexity-metric
       (map #(url-code-revisions change-frequency %))
       (normalize-revs)
       (map #(update % :url (fn [x] (str "root/" x))))
       (map assoc-path)
       (map create-hierachy)
       (flatten)
       (into [] (distinct))))

(comment

  (def path "../spring-boot/")
  (def change-freq (change-frequency path {:since "'3 weeks ago'"}))
  (def file-cpx (analyse-java-files path))

  (def strat (prepare-for-hotspot-chart change-freq file-cpx))

  (oz/start-server!)

  (oz/view! (hotspot-chart strat) :mode :vega)


  {:url "root/spring-boot-project/spring-boot-dependencies/build.gradle",
   :revisions 1.0,
   :code 1569,
   :name "build.gradle",
   :parent "root/spring-boot-project/spring-boot-dependencies"})
