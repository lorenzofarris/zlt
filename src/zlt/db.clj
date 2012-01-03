(ns zlt.db
  (:use [clojure.string :only (split trim)])
  (:require [clojure.contrib.io :as io]
            [clojure.java.jdbc :as sql]
            [clojureql.core :as cql]
            ))

;;(def db {:classname "org.apache.derby.jdbc.EmbeddedDriver"
;;         :subprotocol "derby"
;;         :subname "resources/cedict.db"
;;         :create true})

(def h2db {:classname "com.h2database.jdbc"
           :subprotocol "h2"
           :subname "resources/cedict_db"
           :auto_server "true"})

;; read in the dictionary and convert it to a map
(def cedict-text "resources/cedict_1_0_ts_utf-8_mdbg.txt")
(def fields [:traditional :simplified :pinyin :english :line])
                
(defn trim-fields
  "trim leading and trailing white space from string elements of a vector"
  [x]
  (if (= (count x) 0)
    (vec nil)
    (let [y (first x)
          z (rest x)]
      (if (string? y)
        (cons (trim y) (trim-fields z))
        (cons y (trim-fields z))))))
    
(defn cedict-line [s]
  ;; format of a line is
  ;; Traditional Simplified [pin1 yin1] /English equivalent 1/equivalent 2/
  ;; Pinyin seems always to be between brackets, including in english def

  ;; splits into characters, pinyin, and english
  (let [v (split s #"[\[\]]" 3)
        ;; split the characters into traditional and simplified
	u (split (first v) #" " 2 )
        x  (vec (flatten [u (rest v) s]))]
    (trim-fields x)))
    

(defn not-comment-line?
  "skip comment lines in the cedict file"
  [s]
  (not (= \# (first s))))

(defn get-cedict-lines
  "read in the cedict file, and return it as a vector of strings"
  []
  (filter not-comment-line?
          (io/read-lines cedict-text)))

(defn split-cedict-lines
  "split the cedict lines into component fields, returns a vector of vectors"
  []
  (map cedict-line (get-cedict-lines)))
           

(defn create-cedict-table
  "Create a table containing all cedict entries"
  []
  (sql/create-table
   :cedict
   [:traditional "varchar(64)"]
   [:simplified "varchar(64)"]
   [:pinyin "varchar(256)"]
   [:english "varchar(1024)"]
   [:line "varchar(2048)"]
   [:index "int" "IDENTITY"]
   ))

(defn drop-cedict-table
  "Drop the cedict table so we can start clean"
  []
  (try
    (sql/drop-table :cedict)
    (catch Exception _)))

(defn cedict-text2db
  "load the cedict text db into a derby db"
  []
  (sql/with-connection h2db
    (sql/transaction
     (drop-cedict-table)
     (create-cedict-table)
     (doseq [x (split-cedict-lines)]
       (sql/insert-values :cedict fields x)))))
          
(def test-char "\u5C0F")

(defn find-by-simplified
  "find cedict line by exact match to simplified character"
  [s]
  (first @(-> (cql/table h2db :cedict)
              (cql/select
               (cql/where (= :simplified s))))))

(def flashcard-fields [:traditional
                       :simplified
                       :pinyin
                       :english
                       :rep_char_real
                       :rep_char_effective
                       :ef_char
                       :next_rep_char
                       :rep_pinyin_real
                       :rep_pinyin_effective
                       :ef_pinyin
                       :next_rep_pinyin
                       :interval_char
                       :interval_pinyin ])

(defn add-flashcard
  "add a new flashcard to the flashcard table,
and initialize supermemo parameters"
  [card]
  (let [trad (:traditional card)
        simp (:simplified card)
        pinyin (:pinyin card)
        english (:english card)
        today (java.util.Date.)]
    (sql/with-connection h2db
      (sql/transaction
       (sql/insert-records
        :flashcards
        {:traditional trad
         :simplified simp
         :pinyin pinyin
         :english english
         :rep_char_real 1
         :rep_char_effective 1
         :ef_char 2.5
         :next_rep_char today
         :rep_pinyin_real 1
         :rep_pinyin_effective 1
         :ef_pinyin 2.5
         :next_rep_pinyin today
         :interval_char 1
         :interval_pinyin 1 })))))

(defn update-card
  "add a new flashcard to the flashcard table,
and initialize supermemo parameters"
  [card]
  (let [trad (:traditional card)
        simp (:simplified card)
        pinyin (:pinyin card)
        english (:english card)
        today (java.util.Date.)]
    (cql/update-in!
     (cql/table h2db :flashcards)
     (cql/where (= :index (:index card)))
     {:traditional trad
      :simplified simp
      :pinyin pinyin
      :english english
      :rep_char_real 1
      :rep_char_effective 1
      :ef_char 2.5
      :next_rep_char today
      :rep_pinyin_real 1
      :rep_pinyin_effective 1
      :ef_pinyin 2.5
      :next_rep_pinyin today
      :interval_char 1
      :interval_pinyin 1})))


(defn create-flashcard-table
  "Create a table for flashcards"
  []
  (sql/create-table
   :flashcards
   [:traditional "varchar(64)"]
   [:simplified "varchar(64)"]
   [:pinyin "varchar(256)"]
   [:english "varchar(1024)"]
   [:rep_char_real "int"]
   [:rep_char_effective "int"]
   [:ef_char "real"]
   [:next_rep_char "timestamp"]
   [:rep_pinyin_real "int"]
   [:rep_pinyin_effective "int"]
   [:ef_pinyin "real"]
   [:next_rep_pinyin "timestamp"]
   [:interval_char "int"]
   [:interval_pinyin "int"]
   [:index "int" "IDENTITY"]
   ))

(defn list-flashcards
  "get all the defined flashcards"
  []
  @(cql/table h2db :flashcards))

(defn find-card-by-index
  "get a flashcard from db by index"
  [id]
  (first @(->
           (cql/table h2db :flashcards)
           (cql/select
            (cql/where (= :index id))))))

(defn delete-card
  "delete a flashcard from the db by index"
  [id]
  (first @(cql/disj! (cql/table h2db :flashcards)
             (cql/where (= :index id)))))

(defn get-cards-due-char
  "pull the cards that are due for review from the db"
  []
  (let [today (java.util.Date.)
        ms (. today getTime)
        now (java.sql.Timestamp. ms)]
    @(->
      (cql/table h2db :flashcards)
      (cql/select
       (cql/where (< :next_rep_char now))))))

(defn set-review-type
  "set whether this card is a review of character or pinyin"
  [t v]
  (map (fn [rec] (assoc rec :type t)) v))

(defn get-review-deck
  "pull cards that are due for character or english review"
  []
  (let [now (-> (java.util.Date. )
                (. getTime)
                (java.sql.Timestamp. ))
        char-deck (set-review-type "character"
                                   @(->
                                     (cql/table h2db :flashcards)
                                     (cql/select
                                      (cql/where (< :next_rep_char now)))))
        pinyin-deck (set-review-type "pinyin"
                                     @(->
                                       (cql/table h2db :flashcards)
                                       (cql/select
                                        (cql/where (< :next_rep_pinyin now)))))]
    (concat char-deck pinyin-deck)
   )
  )