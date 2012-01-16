(ns zlt.core
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use ring.middleware.file)
  (:use ring.util.response)
  (:use net.cgrand.enlive-html)
  ;;(:use compojure.core)
  (:use [clojure.string :only (split trim)])
  (:use clojure.tools.logging)
  (:use [clojure.tools.trace :only [deftrace]])
  ;;[compojure.route :as route]
  ;;[compojure.handler :as handler]
  (:require [zlt.db :as zdb]
            [zlt.views :as views]
            [zlt.sm2 :as sm2]
            [net.cgrand.moustache :as mst]))

;; some variables
(def current-card (ref {}))
(def cards-missed (ref (clojure.lang.PersistentQueue/EMPTY)))
(def cards-done (ref []))
(def review-deck (ref []))
;;(def current-card)
;;(def cards-missed)
;;(def cards-done)
;;(def review-deck)

(defn ac1 [m] (apply str (emit* (views/add-char-transform m))))

(defn render-search-results
  "render search results"
  [s]
  (apply str
         (if (= 0 (count s))
           (views/cs "Results will go here")
           (let [result (zdb/find-by-simplified s)]
             (if (nil? result)
               (views/cs "No match found.")
               (views/cs1 result))))))
  
(defn lookup-char-to-learn
  "look up a character in cedict that you want to add to your learning deck, and
prepopulate form fields to add it"
  [s]
  (let [result (zdb/find-by-simplified s)]
    (if (nil? result)
      (ac1 {:simplified s :traditional s :pinyin "" :english ""})
      (ac1 result)
      )))

(defn add-flashcard
  "add a flashcard to the database"
  [m]
  (do (zdb/add-flashcard m)
      (views/cards-list-transform)))

(defn delete-card
  "delete a flashcard and return to the index page"
  [id]
  (let [m (zdb/find-card-by-index id)]
    (zdb/delete-card id)
    (views/cards-list-transform)))

(defn delete-card-confirm
  "confirm deletion of a flash card"
  [id]
  (let [m (zdb/find-card-by-index id)]
    (views/delete-card-confirm-template m)))

(def index-layout (html-resource "public/index.html"))

(defn edit-card
  "set up a page for editing a card"
  [id]
  (let [m (zdb/find-card-by-index id)]
    (views/edit-card-template m)))

(defn update-card
  "update a flashcard in the db"
  [m]
  (do (zdb/update-card m)
      (views/cards-list-transform)))

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

;;(defroutes main-routes
;;  (GET "/cs" [] (render-search-results ""))
;;  (POST "/cs" [simplified] (render-search-results simplified))
;;  (POST "/csa" [simplified] (apply str (lookup-char-to-learn simplified)))
;;  (POST "/fc/add" {params :params} (apply str (add-flashcard params)))
;;  (GET "/fc" [] (apply str (views/cards-list-transform)))
;;  (GET "/fc/delete-confirm/:id" [id] (apply str (delete-card-confirm id)))
;;  (GET "/fc/delete/:id" [id] (apply str (delete-card id)))
;;  (GET "/fc/edit/:id" [id] (apply str (edit-card id)))
;;  (POST "/fc/update" {params :params} (apply str (update-card params)))
  ;; show just the front of the flashcard
;;  (GET "/fc/review" []
;;       (let [rsps (apply str (review-first-card))]
;;         (debug "in /fc/review route" (:simplified @current-card))
;;         rsps
;;         )
;;       )
  ;; show the whole flashcard
;;  (GET "/fc/check" [] (views/back @current-card))
  ;; score the card
;;  (POST "fc/score" {params :params} (score params))
;;  (GET "/fc/check" []
;;    (do
;;      (debug "in /fc/check route: " (:simplified @current-card))
;;      (views/back @current-card)))
    
;;  ;; score the card
;;  (POST "fc/score" {params :params} (score params))
  ;;(GET "/fc/next" [] (apply str (review-next-card)))
;;  (route/resources "/")
;;  (route/not-found "Page not found"))

;;(def app-handler
;;  (handler/site main-routes))

;;(def app
;;  (-> #'app-handler
;; 
;; using ring only for testing
;;(defn app [req]
;;  {:status 200
;;   :headers {"Content-Type" "text/html; charset=utf-8"}
;;   :body  (apply str (views/back @current-card))
;;   }
;;  )

;;  (GET "/cs" [] (render-search-results ""))
;;  (POST "/cs" [simplified] (render-search-results simplified))
;;  (POST "/csa" [simplified] (apply str (lookup-char-to-learn simplified)))
;;  (POST "/fc/add" {params :params} (apply str (add-flashcard params)))
;;  (GET "/fc" [] (apply str (views/cards-list-transform)))
;;  (GET "/fc/delete-confirm/:id" [id] (apply str (delete-card-confirm id)))
;;  (GET "/fc/delete/:id" [id] (apply str (delete-card id)))
;;  (GET "/fc/edit/:id" [id] (apply str (edit-card id)))
;;  (POST "/fc/update" {params :params} (apply str (update-card params)))


(defn wrapit [ret]
  (-> ret
      (response)
      (content-type "text/html; charset=utf-8")
      (constantly))) 

(def zlt-app
  (->
   (mst/app [""] (wrapit (apply str (emit* index-layout)))
            ["fc" "review"] (wrapit (apply str (review-first-card)))
            ["fc" "check"] (wrapit (views/back @current-card))
            )
   (wrap-file "resources/public")
   (wrap-reload '(zlt.core))
   (wrap-stacktrace)
  )

(defn boot []
  (run-jetty #'zlt-app {:port 8080}))

(defn start-server []
  (future (run-jetty #'zlt-app {:port 8080 :join false})))

