# zlt

Online tools for learning chinese.

## Usage

Not going to be available for public usage, may eventually host it on appengine.

## Todo
* Add table for characters I am learning
* Add pages to edit and add dictionary entries
* Add pages to view, edit, add, learning table entries
* Add learning routines

## Notes
For each review, I will pull out a set of items to 
review. As I answer each question, I create a new
record for reviewed questions. 
Learning items:

* character to english and pinyin
* english to pinyin and character
* pinyin phrases to english


## Data Structures
### CEDICT entries
trad_chars simp_chars [pin yin] /english 1/english 2/
I have added an index column in the h2db table, and also set an
index on the simplified characters
### Flashcard datastructures
For each item, I need 
* the item definition, 
* when I last reviewed it
* next scheduled review
* repetition number
* effective repetition number (in supermemo algo, go back to one when you get it wrong)
* difficulty factor
* number of reviews
* number of correct reviews
* quality of current answer

## Algorithms Used
### Supermemo
From http://www.supermemo.com/english/ol/sm2.htm

Algorithm SM-2 used in the computer-based variant of the SuperMemo method and 
involving 
the calculation of easiness factors for particular items:

Split the knowledge into smallest possible items.
With all items associate an E-Factor equal to 2.5.
Repeat items using the following intervals:
I(1):=1
I(2):=6
for n>2: I(n):=I(n-1)*EF
where:
I(n) - inter-repetition interval after the n-th repetition (in days),
EF - E-Factor of a given item
If interval is a fraction, round it up to the nearest integer.
After each repetition assess the quality of repetition response in 0-5 grade scale:
5 - perfect response
4 - correct response after a hesitation
3 - correct response recalled with serious difficulty
2 - incorrect response; where the correct one seemed easy to recall
1 - incorrect response; the correct one remembered
0 - complete blackout.
After each repetition modify the E-Factor of the recently repeated item according 
to the formula:
EF':=EF+(0.1-(5-q)*(0.08+(5-q)*0.02))
where:
EF' - new value of the E-Factor,
EF - old value of the E-Factor,
q - quality of the response in the 0-5 grade scale.
If EF is less than 1.3 then let EF be 1.3.
If the quality response was lower than 3 then start repetitions for the item from 
the beginning without changing the E-Factor (i.e. use intervals I(1), I(2) etc. as 
if the item was memorized anew).
After each repetition session of a given day repeat again all items that scored 
below four in the quality assessment. Continue the repetitions until all of 
these items score at least four.


## License

Copyright (C) 2011 Lorenzo Farris

Distributed under the Eclipse Public License, the same as Clojure.
