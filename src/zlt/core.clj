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
(def cards-missed (ref (clojure.lang.PersistentQueue/EMPTY)))
(def cards-done (ref []))
(def review-deck (ref []))

(defn key-to-keyword
  "for a single map entry, change a string key to a keyword"
  [m e]
  (assoc m (keyword (key e)) (val e)))

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

(defn first-card
  "get the flashcards that are due for review" []
  (let [rd (zdb/get-review-deck)]
    (dosync 
     (ref-set review-deck rd)
     (ref-set cards-done [])
     (ref-set current-card (first @review-deck))
     @current-card
    )))

(defn review-first-card "show the first card" []
  (do
    (first-card)
    (debug "in review-first-card: " @current-card)
    (let [resp (views/front @current-card)]
      (debug "in review-first-card: " resp)
      resp
      )
    )
  )

(defn- get-sm-parameters [m]
  (if (= "character" (:type m))
    {:rep_real (:rep_char_real m)
     :rep_effective (:rep_char_effective m)
     :ef (:ef_char m)
     :next_rep (:next_rep_char m)}
    {:rep_real (:rep_pinyin_real m)
     :rep_effective (:rep_pinyin_effective m)
     :ef (:ef_pinyin m)
     :next_rep (:next_pinyin_char m)}
    )
  )
  
(defn score [m]
  "calculate the new difficulty factor and
new interval, and re-review if answer is not quick enough"
  ;; from web form I get only the score
  (let [s (:score m)]
;;        smp (get-sm-parameters *current-card*)
;;        ef (sm2/difficulty (:ef smp))
;;        i1 (sm2/interval (:rep_effective smp)
;;                         (:ef smp)
;;                         s
        ))

  ;; show just the front of the flashcard
  ;; score the card
;;  (POST "fc/score" {params :params} (score params))
;;  ;; score the card
;;  (POST "fc/score" {params :params} (score params))
  ;;(GET "/fc/next" [] (apply str (review-next-card)))
;;  (route/resources "/")
;;  (route/not-found "Page not found"))



(defn wrapit [ret]
  (-> ret
      (response)
      (content-type "text/html; charset=utf-8")
      (constantly)))

(defn app-cs [req]
  (let [params (req :params)
        simplified (get params "simplified" "not found")]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (str "Basic Ring Handler: " simplified)}))

(defn app-cs1 [{params :params}]
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "Basic Ring Handler 2: " (get params "simplified" "not found"))})

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

(def zlt-app
  (m/app
   (wrap-file "resources/public")
   (wrap-reload '(zlt.core))
   wrap-stacktrace
   wrap-params
   [""] {:get (wrapit (apply str (emit* index-layout)))}
   ["cs"] render-search-results
   ["csa"] lookup-char-to-learn
   ["fc"] (wrapit (apply str (views/cards-list-transform)))
   ["fc" "review"] (wrapit (apply str (review-first-card)))
   ["fc" "check"] (wrapit (views/back @current-card))
   ["fc" "add"] add-flashcard
   ["fc" "delete" id] (constantly (delete-card id))
   ;; when the function needs an argument other than req
   ;; I need to wrap it in "constantly"
   ["fc" "delete-confirm" id] (constantly (delete-card-confirm id))
   ;;  (GET "/fc/edit/:id" [id] (apply str (edit-card id)))
   ["fc" "edit" id] (constantly (edit-card id))
   ;;  (POST "/fc/update" {params :params} (apply str (update-card params)))
   ["fc" "update"] update-card
   ))

(defn me [mac]
  (clojure.pprint/pprint (macroexpand mac)))

(defn boot []
  (run-jetty #'zlt-app {:port 8080}))

(defn start-server []
  (future (run-jetty #'zlt-app {:port 8080 :join false})))

