(ns dev.lhrb.wacogro
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [criterium.core :as c]
   [taoensso.nippy :as nippy])
  (:import (java.io File)))



;; ---------------------------------------  de/serialize  ---------------------------------------

(defn save-to-file! [data]
  (let [name (str "resources/"
                  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd")
                           (java.util.Date. ))
                  ".edn")]
    (nippy/freeze-to-file (io/file name) data)))

(defn read-file [name]
  (nippy/thaw-from-file (io/file name)))

(comment

  (require '[clojure.reflect :as reflect])

  (:members (reflect/reflect System))

  (def dir (str
            (System/getProperty "user.home")
            "/workspace/spring-boot/"))

  (sh/with-sh-dir (System/getProperty "user.home")
   (sh/sh "sh" "-c" "git" "-C" "/workspace/spring-boot/"))

  (str/split-lines (:out (sh/sh (str "/usr/bin/git -C " dir " status"))))

  (sh/sh "git" "-C" "../elasticsearch/" "log")


  (apply sh/sh (str/split "git log" #" "))

  (def stats (analyze-all "../elasticsearch/"))

  (->> stats
       (sort-by :spaces >)
       (take 10))

  (sh/sh "git log --since='3 weeks ago' --format=format: --name-only | grep -Ev '^$' | sort | uniq")

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
