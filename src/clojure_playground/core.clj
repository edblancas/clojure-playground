(ns clojure-playground.core
  (:require
   [clojure.string :as string]
   [clojure.string :as str]))

(defn hello []
  (print "hello"))

(print "holi")

;; -> hello

(def pers-list (atom [1 2 3]))
(defn get-elem [a]
  (let [first-elem (first @a)]
    (swap! pers-list rest)
    first-elem))

(comment
  (take-while (complement nil?) (repeatedly #(get-elem pers-list)))
  (take 2 (repeatedly get-elem))

  (case "jello"
    "jello" "hey")

  (let [v (take-while (complement nil?) (repeatedly #(constantly nil)))]
    (handler v))
  #_())

(defn variadic-args [& args]
  (println [1 2 3])
  (println (vector? args))
  (print args))

(variadic-args 1 2 3)

(defn handler
  [& args]
  (println "handler fn----" args)
  args
  )

(apply handler '()) ;; handler is called with no args
(handler)  ;; args is nill
(comment
  (apply #(str "args=" %&) '())
  #_())
#_(let [v (take-while (complement nil?) [nil])]
  (apply handler v))

(defn handler2
  [& args]
  (println "handler fn----" args)
  (let [vec-args (vec args)]
    (case (string/lower-case (get vec-args 2))
      "ping" "+PONG\r\n"
      "echo" (let [s (get vec-args 4)]
               (format "$%d\r\n%s\r\n" (count s) s))
      "default"  "+PONG\r\n")))

(vec nil)  ;; []
(get (vec nil) 2)  ;; nil
;;(string/lower-case nil)  ;; exception same as below
;;(handler2)
(seq [])

(defmacro do-do [x afn]
    `(do ~(afn x)))

;; ~(afn x ) is called at macro-expansion time
;; print is not evaluated so is a symbol and ~('inc 1) returns nil
(do-do 1 print)

(defmacro do-do2 [x afn]
    `(do (~afn ~x)))

(do-do2 1 inc)  ;; 2

(defmacro do-do3 [x afn]
  ;; afn and x are namespaced simbols
    `(do (afn x)))

(macroexpand '(do-do3 2 inc))


(def vampire-database
  {0 {:drinks-blood? false, :name "John"}
   1 {:drinks-blood? false, :name "Amy"}
   2 {:drinks-blood? true, :name "Bill"}
   3 {:drinks-blood? true, :name "Denise"}})

(defn get-vampire [id]
  (Thread/sleep 100)
  (get vampire-database id))

(defn vampire? [record]
  (and (:drinks-blood? record)
       record))

;; will create a lazy-seq
(defn identify-vampires [ids]
  (->> ids
       (map get-vampire)
       (filter vampire?)))

(time (first (identify-vampires (range 300000))))
;; "Elapsed time: 3300.758236 msecs"
;; first will realize the first chunk, i.e. the first 32 elements, hence the time 100ms penalty times 32 elements = 3200ms aprox
;; internally first calls the .seq methond of the LazySeq for realization

(time (take 1 (identify-vampires (range 300000))))
;;"Elapsed time: 0.065594 msecs"
;; why the difference in time for take and first?
;;   `take` returns a lazy seq and `time` (is a macro) realize it
;; in the docs of `time`:
;when working with lazy seqs
;(time (doall (...)))
(time (doall (take 1 (identify-vampires (range 300000)))))
;; now the time is right! 3200ms aprox

;; From the "Making Clojure Lazier" docs,
;; here's an example using a step function.
(defn filter_
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns true. pred must be free of side-effects."
  [pred coll]
  (let [step (fn [p c]
               (when-let [s (seq c)]
                 (if (p (first s))
                   (cons (first s) (filter_ p (rest s)))
                   (recur p (rest s)))))]
    (lazy-seq (step pred coll))))

;; almost the same as the below, although here we go sequentially through the db, and not by a coll of ids
;;   and create a lazy-seq of vampires
(def identify-vampires-filter-no-chunk (filter vampire? (vals vampire-database)))
(time (take 1 (filter vampire? (vals vampire-database))))

(defn identify-vampires-no-chunk [ids]
  (lazy-seq
    ;;(println "REALIZED")
    ;;(println (vampire? (get-vampire (first ids))))
    (when-let [ids (seq ids)]
      (if (vampire? (get-vampire (first ids)))
        (cons (get-vampire (first ids)) (identify-vampires-no-chunk (rest ids)))
        (identify-vampires-no-chunk (rest ids))))))

(time (take 1 (identify-vampires-no-chunk (range 300000))))

(defn identify-vampires-no-chunk-step [ids]
  (let [step (fn [ids]
               (when-let [s (seq ids)]
                 (if (vampire? (get-vampire (first s)))
                   ;; only call again the main fn cuz the step is only created when the vampire? is true
                   (cons (get-vampire (first ids)) (identify-vampires-no-chunk-step (rest s)))
                   ;; if the vampire? is false then there is no reason to create a step, so we recur inside the lambda with the next id
                   (recur (rest ids)))))]
    (lazy-seq (step ids))))

(time (take 1 (identify-vampires-no-chunk-step (range 300000))))

;; TLDR: Lazy sequence don't get along with side effects!!!
