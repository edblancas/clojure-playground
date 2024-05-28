(ns clojure-playground.macros-explained)

;; https://www.braveclojure.com/writing-macros/


(defmacro report-gensym
  [to-try]
  (let [result (gensym to-try)]
    `(let [~result ~to-try]
       (if ~result
         (println (quote ~to-try) "was successful:" ~result)
         (println (quote ~to-try) "was not successful:" ~result)))))

(report-gensym (= 1 1))
; => (= 1 1) was successful: true

(report-gensym (= 1 2))
; => (= 1 2) was not successful: false

(doseq [code ['(= 1 1) '(= 1 2)]]
  (report-gensym code))
; => code was successful: (= 1 1)
; => code was successful: (= 1 2)

;; # for gensym only works inside `
;; # is special and executes and creates a unique symbol
(defmacro report
  [to-try]
  `(let [result# ~to-try]
     (if result#
       (println (quote ~to-try) "was successful:" result#)
       (println (quote ~to-try) "was not successful:" result#))))

(report (= 1 1))
; => (= 1 1) was successful: true

(report (= 1 2))
; => (= 1 2) was not successful: false

(doseq [code ['(= 1 1) '(= 1 2)]]
  (report code))
; => code was successful: (= 1 1)
; => code was successful: (= 1 2)

;; Why this happen?
;; The macro being executed at macro expansion time, doesn't have access to the values of the list
;; at runtime, so the code that is ran is:
(doseq [code ['(= 1 1) '(= 1 2)]]
  (if code
    (clojure.core/println 'code "was successful:" code)
    (clojure.core/println 'code "was not successful:" code)))
;; this because to-try only gets the symbol `code` then the part `[result# ~to-try]`
;; only saves the symbol `code` in the local var pointed by the symbol returned by `result#`.
;; that's why the parts with the `code` symbol evaluates to the list quoted `'(= 1 1)` and `'(= 1 2)`
;; on each iteration.

(defmacro doseq-macro
  [macroname & args]
  `(do
     ~@(map (fn [arg] (list macroname arg)) args)))

(doseq-macro report (= 1 1) (= 1 2))
; => (= 1 1) was successful: true
; => (= 1 2) was not successful: false

;; Macros only really compose with each other, so by using them, you might be missing out on the other
;; kinds of composition (functional, object-oriented) available to you in Clojure.
