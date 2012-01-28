(ns zlt.views
  (:use net.cgrand.enlive-html)
  (:use clojure.tools.logging)
  (:use [clojure.tools.trace :only [deftrace]])
  (:require [zlt.db :as zdb]))

(def add-layout (html-resource "public/add.html"))
(def cards-layout (html-resource "cards.html"))
(def review-layout (html-resource "review.html"))
(def index-layout (html-resource "public/index.html"))

(deftemplate cs "public/cs.html" [s] [:div#results]
  (content s))

(deftemplate cs1 "public/cs.html" [m]
  [:span#simplified](content (:simplified m))
  [:span#pinyin](content (:pinyin m))
  [:span#english](content (:english m)))

(defn index-msg
  "Return the main app page with a message at the top"
  [message]
  (at index-layout
      [:div#message] (content message)))

(defn add-char-transform [m]
  (at add-layout
      [:div#lookup [:input (attr= :name "simplified")]]
      (set-attr :value (:simplified m))
      [:div#add_to_deck [:input (attr= :name "simplified")]]
      (set-attr :value (:simplified m))
      [:div#add_to_deck [:input (attr= :name "traditional")]]
      (set-attr :value (:traditional m))
      [:div#add_to_deck [:input (attr= :name "pinyin")]]
      (set-attr :value (:pinyin m))
      [:div#add_to_deck [:input (attr= :name "english")]]
      (set-attr :value (:english m))))


(defn cards-list-transform []
  (let [recs (zdb/list-flashcards)]
    (emit*
     (transform
     cards-layout
     [:tr.card_row]
     (clone-for [m recs]
                [:td.simp] (content (:simplified m))
                [:td.trad] (content (:traditional m))
                [:td.pinyin] (content (:pinyin m))
                [:td.english] (content (:english m))
                [:td.index :a] (set-attr :href (str "/fc/edit/" (:index m)))
                [:td.delete :a] (set-attr :href (str "/fc/delete-confirm/" (:index m)))
                )))))

(deftemplate delete-card-confirm-template "delete_confirm.html" [m]
  [:span#simplified](content (:simplified m))
  [:span#traditional](content (:traditional m))
  [:span#pinyin](content (:pinyin m))
  [:span#english](content (:english m))
  [:a#delete_url] (set-attr :href (str "/fc/delete/" (:index m)))
  )

(deftemplate edit-card-template "edit_card.html" [m]
  [:div#edit_card [:input (attr= :name "index")]](set-attr :value (:index m))
  [:div#edit_card [:input (attr= :name "simplified")]](set-attr :value (:simplified m))
  [:div#edit_card [:input (attr= :name "traditional")]](set-attr :value (:traditional m))
  [:div#edit_card [:input (attr= :name "pinyin")]](set-attr :value (:pinyin m))
  [:div#edit_card [:input (attr= :name "english")]](set-attr :value (:english m)))

;; used 1
(defn front-character-xform [m]
  (let [layout (html-resource "review.html")]
    (debug "in front-character-xform: " (:simplified m))
    (at layout
        [:div#front :div.character] (do->
                                     (content (:simplified m))
                                     (remove-attr :hidden))
        [:div#check] (remove-attr :hidden))))

;;used 1
(defn front-pinyin-xform 
  "transform page to show only pinyin and english"
  [m]
  (at review-layout
      [:div#front :div.pinyin] (do->
                                (content (:pinyin m))
                                (remove-attr :hidden))
      [:div#front :div.english] (do->
                                 (content (:english m))
                                 (remove-attr :hidden))
      [:div#check] (remove-attr :hidden)))

;; used by back
(defn full-character-template
  "show front and back of card together"
  [m]
  (let [res (front-character-xform m)]
    (at res
        [:div#back :div.pinyin] (do->
                                 (remove-attr :hidden)
                                 (content (:pinyin m)))
        [:div#back :div.english] (do->
                                  (remove-attr :hidden)
                                  (content (:english m)))
        [:div#score] (set-attr :style "display:in-line")
        [:div#check] (set-attr :hidden "hidden")
        )
    )
  )
;; used by back
(defn full-pinyin-template
  "show front and back of card together"
  [m]
  (let [res (front-pinyin-xform m)]
    (at res
        [:div#back :div.character] (do->
                                 (content (:simplified m))
                                 (remove-attr :hidden))
        [:div#score] (set-attr :style "display:in-line")
        [:div#check] (set-attr :hidden "hidden")
        )
    )
  )

;; used in core.clj
(defn front [m]
  "render a flashcard for review with the backside hidden"
  (do
    (debug "in views/front: " (:simplified m))
    (cond
     (= (:type m) "character") (apply str (emit* (front-character-xform m)))
     (= (:type m) "pinyin") (apply str (emit* (front-pinyin-xform m)))
     :else "bad card"
     )
    )
  )

;; used in core.clj
(defn back [m]
  "reveal the backside of the card"
  (do
    (debug "in views/back: " (:simplified m))
    (cond
      (= (:type m) "character") (apply str (emit* (full-character-template m)))
      (= (:type m) "pinyin") (apply str (emit* (full-pinyin-template m)))
      :else "bad card"
      )
    )
  )



