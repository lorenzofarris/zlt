(ns zlt.core
  (:use ring.adapter.jetty)
  (:use ring.middleware.reload)
  (:use ring.middleware.stacktrace)
  (:use net.cgrand.enlive-html)
  (:use compojure.core)
  (:use [clojure.string :only (split trim)])
  (:use net.cgrand.enlive-html)
  (:require [compojure.route :as route]
	    [compojure.handler :as handler]
	    [clojure.contrib.logging :as log]
            [zlt.db :as zdb]
            [zlt.views :as views]))

;;(log/debug "testing logging subsystem")


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

(defn review-first-card
  "set up the flashcard review, and show the first card"
  []
  ;;
  (do
    ;; get the flashcards that are due for review
    (def review-deck (zdb/get-review-deck))
    )
  )

(defroutes main-routes
  ;;(GET "/" [] "<h1>Hello Worldy!</h1>")
  (GET "/" [] (apply str (emit* index-layout)))
  (GET "/cs" [] (render-search-results ""))
  (POST "/cs" [simplified] (render-search-results simplified))
  (POST "/csa" [simplified] (apply str (lookup-char-to-learn simplified)))
  (POST "/fc/add" {params :params} (apply str (add-flashcard params)))
  (GET "/fc" [] (apply str (views/cards-list-transform)))
  (GET "/fc/delete-confirm/:id" [id] (apply str (delete-card-confirm id)))
  (GET "/fc/delete/:id" [id] (apply str (delete-card id)))
  (GET "/fc/edit/:id" [id] (apply str (edit-card id)))
  (POST "/fc/update" {params :params} (apply str (update-card params)))
  (GET "/fc/review" [] (apply str (review-first-card)))
  (GET "/fc/next" [] (apply str (review-next-card)))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app-handler
  (handler/site main-routes))

(def app
  (-> #'app-handler
      (wrap-reload '(zlt.core))
      (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))

(defn start-server []
  (future (run-jetty #'app {:port 8080 :join false})))