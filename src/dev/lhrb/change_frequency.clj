(ns dev.lhrb.change-frequency
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]))

;; count how many times a file is changed => appears in a git log message
;;
;; git log --format=format: --name-only | grep -Ev '^$' | sort | uniq -c | sort
;; git log --since='3 weeks ago' --format=format: --name-only | grep -Ev '^$' | sort | uniq -c | sort

(defn git-cmd
  [path
   {:keys [since]
    :or {since nil}}]
  (if since
    ["git" "-C" path "log" "--format=format:" "--name-only" (str "--since=" since)]
    ["git" "-C" path "log" "--format=format:" "--name-only"]))

(defn change-frequency
  "path to repository
  options {:since '3 weeks ago'} empty for max time range"
  [path options]
  (->>
   (git-cmd path options)
   (apply sh/sh)
   :out
   (str/split-lines)
   (remove empty?)
   (frequencies)))

(comment

  (git-cmd "" {:since "'3 weeks ago'"})
  (change-frequency "../spring-boot/" {:since "'3 weeks ago'"})

  ,)
