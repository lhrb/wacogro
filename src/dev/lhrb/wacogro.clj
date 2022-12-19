(ns dev.lhrb.wacogro
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [criterium.core :as c])
  (:import (java.io File)))

(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

;; --------------- fsm ------------------

(defn start-state []
  {:state :start
   :lines 0
   :spaces 0})

(defn start
  [^long token state]
  (case token
    9  (update state :spaces #(+ 4 %))  ;; tab
    32 (update state :spaces inc)
    10 (update state :lines inc)
    (assoc state :state :skip)))

(defn skip
  [^long token state]
  (if (== 10 token)
    (-> state (update :lines inc) (assoc :state :start))
    state))

(defn transition [k ^long token state]
  (case k
    :start (start token state)
    :skip (skip token state)))

(defn cpx-file
  [^File f]
  (with-open [rdr (io/reader f)]
    (loop [current (start-state)
           token (.read rdr)]
      (if (== -1 token)
        (-> current (dissoc :state) (assoc :name (str f)))
        (recur (transition (get current :state) token current)
               (.read rdr))))))

(defn file-extension? [extension]
  (fn [^File f]
    (str/ends-with? (.getName f) extension)))

(defn analyze-all
  [path]
  (->> (file-seq (io/file path))
       (filter (file-extension? ".java"))
       (pmap cpx-file)
       (into [])))


(comment

  (require '[clojure.reflect :as reflect])

  (:members (reflect/reflect System))

  (def dir (str
            (System/getProperty "user.home")
            "/workspace/spring-boot/"))

  (sh/with-sh-dir (System/getProperty "user.home")
   (sh/sh "sh" "-c" "git" "-C" "/workspace/spring-boot/"))

  (str/split-lines (:out (sh/sh (str "/usr/bin/git -C " dir " status"))))

  (sh/sh "git" "log")


  (def stats (analyze-all "../elasticsearch/"))

  (->> stats
       (sort-by :spaces >)
       (take 10))

  (require '[kixi.stats.core :as kixi])

  (->> stats
       (map (fn [m] (assoc m :aggregate
                    (double (/ (:lines m) (:spaces m))))))
       (sort-by :aggregate >))

  (< 1 2)

  (transduce (comp (map (fn [m] (double (/ (:spaces m) (:lines m))))))
             kixi/max stats)

  (transduce identity (kixi/correlation :spaces :lines) stats)


  (require '[clj-async-profiler.core :as prof])
  (require '[criterium.core :as c])

  (prof/serve-ui 8080)

  (c/quick-bench (analyze-all "../elasticsearch/"))

  (prof/profile (analyze-all "../elasticsearch/"))



(require '[oz.core :as oz])

(oz/start-server!)

(oz/view!
 {:data {:values stats}
  :width 1000
  :height 1000
  :layer [{:mark {:type "point" :tooltip {:content "data"}}
           :encoding {:x {:field "lines" :type "quantitative"}
                      :y {:field "spaces" :type "quantitative"}}}
          {:mark {:type "line" :color "firebrick"}
           :transform [{:regression "lines" :on "spaces"}]
           :encoding {:x {:field "lines" :type "quantitative"}
                      :y {:field "spaces" :type "quantitative"}}}]})


    ,)
