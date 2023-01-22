(ns dev.lhrb.complexity-metric
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import (java.io File)))

;; calculates indentation-based complexity metric
;; and counts lines of code


;; --------------- fsm -----------------
;; parsing is implemented as finite state machine

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

(defn remove-path-from-filename [path m]
  (update m :name #(str/replace-first % path "")))

(defn analyse-java-files
  "Since our fsm operates on bytes and we want to analyse our
  codebase reasonably fast we need to filter for extension we
  want to allow. Currently we only support java files."
  [path]
  (let [remove-path-from-name (partial remove-path-from-filename path)]
   (->> (file-seq (io/file path))
        (filter (file-extension? ".java"))
        (pmap cpx-file)
        (pmap remove-path-from-name)
        (into []))))

(comment
  (def cpx-metric (analyse-java-files "../spring-boot/"))
  ,)
