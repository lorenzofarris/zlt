(ns zlt.views
  (:use net.cgrand.enlive-html)
  (:require [zlt.db :as zdb]))

(deftemplate cs "cs.html" [s] [:div#results]
  (content s))

(deftemplate cs1 "cs.html" [m]
  [:span#simplified](content (:simplified m))
  [:span#pinyin](content (:pinyin m))
  [:span#english](content (:english m)))

(def add-layout (html-resource "public/add.html"))

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

(def cards-layout (html-resource "cards.html"))

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


;;(deftemplate front-character-template "review.html" [m]
;;  [:div#front :div.character] (do->
;;                               (content (:simplified m))
;;                               (remove-attr :hidden))
;;  [:div#check] (remove-attr :hidden)
;;  )

(defn front-character-xform [m]
  (let [layout (html-resource "review.html")]
    (at layout
        [:div#front :div.character] (do->
                                     (content (:simplified m))
                                     (remove-attr :hidden))
        [:div#check] (remove-attr :hidden))))
    

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
        [:div#score] (set-attr :style "display:in-line"))))

(deftemplate check-character-template "review.html" [m]
  [:div#front :p.character] (content (:simplified m))
  [:div#back :p.pinyin] (content (:pinyin m))
  [:div#back :p.english] (content (:english m))
  [:p.score] (set-attr :style "display:inline;")
  )

(deftemplate front-pinyin-template "review.html" [m]
  [:div#front :p.pinyin] (content (:pinyin m))
  [:div#front :p.english] (content (:english m))
  )

(defn full-pinyin-template
  "show front and back of card together"
  [m]
  (let [res (front-pinyin-template m)]
    (at res
        [:div#back :div.character] (do->
                                 (content (:character m))
                                 (remove-attr :hidden))
        [:div#score] (remove-attr :hidden))))

;;(deftemplate check-pinyin-template "review.html" [m]
;;  [:div#back :p.character] (content (:simplified m))
;;  [:div#front :p.pinyin] (content (:pinyin m))
;;  [:div#front :p.english] (content (:english m))
;;  [:p.score] (set-attr :style "display:inline;"))
  
(defn front [m]
  "render a flashcard for review with the backside hidden"
  (cond
   (= (:type m) "character") (apply str (emit* (front-character-xform m)))
   (= (:type m) "pinyin") (front-pinyin-template m)
   :else "bad card"
   )
  )

(defn back [m]
  "reveal the backside of the card"
  (cond
   (= (:type m) "character") (apply str (emit* (full-character-template m)))
   (= (:type m) "pinyin") (apply str (full-pinyin-template m))
   :else "bad card"
   )
  )



