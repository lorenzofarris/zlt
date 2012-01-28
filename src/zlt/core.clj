(ns zlt.core
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use ring.middleware.file)
  (:use ring.middleware.params)
  (:use ring.util.response)
  (:use net.cgrand.enlive-html)
  (:use [clojure.string :only (split trim)])
  (:use clojure.tools.logging)
  (:use [clojure.tools.trace :only [deftrace]])
  (:require [zlt.db :as zdb]
            [zlt.views :as views]
            [zlt.sm2 :as sm2]
            [net.cgrand.moustache :as m]))

;; some variables
(def current-card (ref {}))
(def card-queue (ref (clojure.lang.PersistentQueue/EMPTY)))
;;(def cards-done (ref []))
;;(def review-deck (ref []))

(defn key-to-keyword
  "for a single map entry, change a string key to a keyword"
  [m e]
  (assoc m (keyword (key e)) (val e)))

;; reinventing the wheel, turns out an equivalent function
;; exists in clojure core
(defn keys-to-keywords
  "change map keys from strings to keywords"
  ;; need to do this with recursion
  [m]
  (if (= 0 (count m))
    {}
    (key-to-keyword (keys-to-keywords (rest m)) (first m))
    ))

(defn ac1 [m] (apply str (emit* (views/add-char-transform m))))

(def index-layout (html-resource "public/index.html"))

(defmethod print-method clojure.lang.PersistentQueue
  [q, w]
  (print-method '<- w) (print-method (seq q) w) (print-method '-< w))

;;  (route/not-found "Page not found"))

(defn wrapit
  "wrap a value being returned in a web page as a function that
returns a ring response"
  [ret]
  (-> ret
      (response)
      (content-type "text/html; charset=utf-8")
      (constantly)))

(defn app-cs
  "test ring handler"
  [req]
  (let [params (req :params)
        simplified (get params "simplified" "not found")]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (str "Basic Ring Handler: " simplified)}))

(defn app-cs1
  "test ring handler"
  [{params :params}]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "Basic Ring Handler 2: " (get params "simplified" "not found"))})

;; test ring app
(def zlt-app1 (m/app wrap-params app-cs1))

(defn render-response
  "render page, and put it into ring response format"
  [r]
  (-> r (response) (content-type "text/html; charset=utf-8"))
  )

(defn render-search-results 
  "render search results"
  [{params :params}]
  (let [s (get params "simplified")
        r (apply str
                 (if (= 0 (count s))
                   (views/cs "Results will go here")
                   (let [result (zdb/find-by-simplified s)]
                     (if (nil? result)
                       (views/cs "No match found.")
                       (views/cs1 result)))))]
    (render-response r)
    )
  )

(defn lookup-char-to-learn
  "look up a character in cedict that you want to add to your learning deck, and
prepopulate form fields to add it"
  [{params :params}]
  (let [s (get params "simplified")
        result (zdb/find-by-simplified s)
        r (if (nil? result)
            (ac1 {:simplified s :traditional s :pinyin "" :english ""})
            (ac1 result))]
    (render-response (apply str r))
    ))

(defn add-flashcard
  "add a flashcard to the database"
  [{params :params}]
  (do (debug "add-flashcard: " params)
      (debug "add-flashcard keywords: " (keys-to-keywords params))
      (zdb/add-flashcard (keys-to-keywords params))
      (render-response (apply str (views/cards-list-transform)))))

(defn delete-card
  "delete a flashcard and return to the index page"
  [id]
  (let [m (zdb/find-card-by-index id)]
    (zdb/delete-card id)
    (-> (apply str (views/cards-list-transform))
        (render-response))
    )
  )

(defn delete-card-confirm
  "confirm deletion of a flash card"
  [id]
  (let [m (zdb/find-card-by-index id)]
    (render-response (apply str (views/delete-card-confirm-template m)))
    )
  )

(defn edit-card
  "set up a page for editing a card"
  [id]
  (let [m (zdb/find-card-by-index id)]
    (render-response (apply str (views/edit-card-template m)))))

(defn update-card
  "update a flashcard in the db"
  [{m :params}]
  (do (zdb/update-card (keys-to-keywords m))
      (render-response (apply str (views/cards-list-transform)))))

(defn seq-to-queue
  "convert a sequence to a persistent queue"
  [s]
  (if (= 0 (count s))
    (clojure.lang.PersistentQueue/EMPTY)
    (conj (seq-to-queue (rest s)) (first s))))

;;(defn first-card-old
;;  "get the flashcards that are due for review"
;;  []
;;  (let [rd (zdb/get-review-deck)]
;;    (dosync 
;;     (ref-set review-deck rd)
;;     (ref-set cards-done [])
;;     (ref-set current-card (first @review-deck))
;;     @current-card
;;     )
;;    )
;;  )

(defn first-card
  "get the flashcards that are due for review, and queue them up"
  []
  (let [rd (zdb/get-review-deck)]
    (dosync
     (ref-set card-queue (seq-to-queue rd))
     (ref-set current-card (peek @card-queue))
     )
    )
  )

(defn review-first-card "show the first card" [req]
  (do
    (first-card)
    (debug "review-first-card: " @current-card)
    (render-response (apply str (views/front @current-card)))
    )
  )

(defn review-card
  "review the flashcards that are due"
  [req]
  (if (= 0 (count card-queue))
    ;; this might be first time through, or trying again
    (do
      (first-card)
      (if (= 0 (count card-queue))
        ;; no more cards due, go to finish screen
        (render-response
         (apply str
                (views/index-msg "Congratulations, no cards due for review.")))
        (render-response (apply str (views/front @current-card)))
        ) ; end of if
      ) ; end of do
    (render-response (apply str (views/front @current-card)))
    ); end of if
  )
    
(defn repeat-card
  "take a card from the front of the queue and put it on the back"
  []
  (dosync
   (alter card-queue conj @current-card)
   (alter card-queue pop)
   (ref-set current-card (peek @card-queue))
   )
  )

(defn- get-sm-parameters [m]
  (if (= "character" (:type m))
    {:rep_real (:rep_char_real m)
     :rep_effective (:rep_char_effective m)
     :ef (:ef_char m)
     :next_rep (:next_rep_char m)
     :interval (:interval_char m)}
    {:rep_real (:rep_pinyin_real m)
     :rep_effective (:rep_pinyin_effective m)
     :ef (:ef_pinyin m)
     :next_rep (:next_rep_pinyin m)
     :interval (:interval_pinyin m)}
    )
  )

(defn update-sm2-params
  "Returns a map made from current-card with parameters
related to SuperMemo2 algorithm updated based on last
flashcard review"
  [i d]
  (let [next-rep (java.util.Date.)
        time (.getTime next-rep)]
    ;; one day is 86400000 ms
    (.setTime next-rep (* i 86400000))
    (cond
     (= (:type @current-card) "character")
     (let [rep_real (:rep_char_real @current-card)
           rep_effective (:rep_char_effective @current-card)]
           (assoc @current-card
             :rep_char_real (inc rep_real)
             :rep_char_effective (inc rep_effective)
             :ef_char d
             :interval_char i
             :next_rep_char next-rep))
            
     (= (:type @current-card) "pinyin")
     (let [rep_real (:rep_pinyin_real @current-card)
           rep_effective (:rep_pinyin_effective @current-card)]
           (assoc @current-card
             :rep_pinyin_real (inc rep_real)
             :rep_pinyin_effective (inc rep_effective)
             :ef_pinyin d
             :interval_pinyin i
             :next_rep_pinyin next-rep))
     true {})
    )
  )

(defn record-score [score]
  (let [smp (get-sm-parameters score)
        [i d] (sm2/efi (smp :rep_effective)
                       (smp :ef)
                       score
                       (smp :interval))
        nc (update-sm2-params i d)]
    (zdb/update-card-sm2 nc)))

(defn get-next-card
  "discard the card just scored, and take the next card off the deck"
  []
  (dosync
   (alter card-queue pop)
   (ref-set current-card (peek @card-queue))
   )

(defn score-card
  "Take the user's self-assigned score for the card.
If score is 3 or less, put it back in the queue to review.
If score is 4 or 5, compute new difficulty and interval,
and update in the DB."
  [{m :params}]
  (let [score (m "score")]
    ;; if score is less than 4, just put it in the queue
    (if (< score 4)
      (repeat-card)
      (do
        (record-score score)
        (get-next-card)
        (if (= 0 (count card-queue))
          ;; return completion screen with links to main page
          ;; go to page for next card
        )
      )
    )
  )

(def zlt-app
  (m/app
   (wrap-file "resources/public")
   ;; it would appear to be wrap-reload that is killing
   ;; my references
   ;;(wrap-reload '(zlt.core))
   wrap-stacktrace
   wrap-params
   [""] {:get (wrapit (apply str (views/index-msg "")))}
   ["cs"] render-search-results
   ["csa"] lookup-char-to-learn
   ["fc"] (wrapit (apply str (views/cards-list-transform)))
   ["fc" "review"] review-card
   ["fc" "check"]  (wrapit (views/back @current-card))
   ["fc" "add"] add-flashcard
   ["fc" "delete" id] (constantly (delete-card id))
   ;; when the function needs an argument other than req
   ;; I need to wrap it in "constantly"
   ["fc" "delete-confirm" id] (constantly (delete-card-confirm id))
   ["fc" "edit" id] (constantly (edit-card id))
   ["fc" "update"] update-card
   ["fc" "score"] score-card
   )
  )


(defn me [mac]
  (clojure.pprint/pprint (macroexpand mac)))

(defn boot []
  (run-jetty #'zlt-app {:port 8080}))

(defn start-server []
  (future (run-jetty #'zlt-app {:port 8080 :join false})))

