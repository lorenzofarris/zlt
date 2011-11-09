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

;;(deftemplate ac "public/add.html" [m]
;;  [:div#lookup [:input (attr= :name "simplified")]] (set-attr :value (:simplified m))
;;  [:div#add_to_deck [:input (attr= :name "simplified")]] (set-attr :value (:simplified m))
;;  [:div#add_to_deck [:input (attr= :name "traditional")]] (set-attr :value (:traditional m))
;;  [:div#add_to_deck [:input (attr= :name "pinyin")]] (set-attr :value (:pinyin m))
;;  [:div#add_to_deck [:input (attr= :name "english")]] (set-attr :value (:english m)))
