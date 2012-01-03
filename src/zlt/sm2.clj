(ns zlt.sm2)
            
;; calculations of optimal next repetition of flash card items

(defn difficulty
  "calculate new difficulty factor, based on old difficulty factor, and
quality of response of repetition, where
5 - perfect response;
4 - correct response after a hesitation;
3 - correct response recalled with serious difficulty;
2 - incorrect response; where the correct one seemed easy to recall;
1 - incorrect response; the correct one remembered;
0 - complete blackout."
  [d q]
  (let [e (- 5 q)
        f (+ d (- 0.1 (* e (+ 0.08 (* e 0.02)))))]
    (if (< f 1.3) 1.3 f)))


(defn interval
  "calculate interval until next review of card, given repetition number,
difficulty factor, and quality of current answer. Arguments are
n: repetition number
d: current difficulty factor
q: quality of response
i: old interval"
  [n d q i]
  (cond
   (= n 1) 1
   (= n 2) 6
   (< q 3) 1
   true (* i (difficulty d q))))

;; for each item, I will need
;; rep number
;; difficulty factor
;; and after each response I will calculate
;; new interval
;; new difficulty
;; I will want to maintain statistics on real repetition number,
;; and effective repetition number, and use effective repetition
;; number for calculation of interval
      