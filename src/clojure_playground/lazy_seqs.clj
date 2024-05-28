(ns clojure-playground.lazy-seqs)

(take 1 (map println [1 2 3 4 5]))
;; 1
;; 2
;; 3
;; 4
;; 5
;; (nil)

(take 1 (map println '(1 2 3 4 5)))
;; 1
;; (nil)

(type (seq [1 2 3 4 5]))
;; => clojure.lang.PersistentVector$ChunkedSeq

(type (seq '(1 2 3 4 5)))
;; => clojure.lang.PersistentList

(type (map println '(1 2 3 4 5)))
;; => clojure.lang.LazySeq

(type (map println [1 2 3 4 5]))
;; => clojure.lang.LazySeq

;; map internally calls first `seq`, for the vector it returns a
;;   `clojure.lang.PersistentVector$ChunkedSeq` that is wrapped in
;;   the final LazySeq returned by map. take also returns a LazySeq,
;;   but we its realized (by printing in the REPL) then the whole
;;   chunk is realized, yes, the first 32 or less elements.
;; for the list, seq returns a `clojure.lang.PersistentList` that
;;   is wrapped inside the final LazySeq returned by map, but by
;;   the time of realization, only one element of the LazySeq computed.

(type (range 200))
;; => clojure.lang.LongRange

(chunked-seq? (map pr '(1 2 3 4)))
;; => false
(chunked-seq? (map pr (range 100)))
;; => false
(type (range 10))
;; => clojure.lang.LongRange

(defn lazy-range [i limit]
  (lazy-seq
    (when (< i limit)
      (println "REALIZED")
      (cons i (lazy-range (inc i) limit)))))

(chunked-seq? (lazy-range 0 1))
;; => false
(chunked-seq? (range 200))
;; => true
(chunked-seq? '(1 200))
;; => false
(chunked-seq? [1 2 3])
;; => false

(type (map pr (range 100)))
;; => clojure.lang.LazySeq

(do (doall 1 (map pr (range 100))) (println))
;; 012345678910111213141516171819202122232425262728293031
;; => nil

(do (doall 1 (map pr '(1 2 3 4))) (println))
;; 1 2
;; => nil

(seq? [])
;; => false
(seq? '())
;; => true

;; https://redefine.io/blog/buffered-sequences/ ;;

(def coll (map(fn [x] (println "realized" x) (inc x)) [1 2 3 4]))
(take 1 coll)
;; realized 1
;; realized 2
;; realized 3
;; realized 4
;; => (2)
;; we just get the first but the first 32 elements are already realized

(def coll2 (map (fn [x] (println "realized" x) (inc x)) '(1 2 3 4)))
(take 1 coll2)
;; realized 1
;; => (2)
;; just realized the first elem as a list is not a chunked-seq?

(defn my-lazy-seq
  ([]
   (my-lazy-seq 0))
  ([n]
   (lazy-seq
     (cons n
       (my-lazy-seq (inc n))))))

(defn load
  [v]
  (printf "Loading: %d \n" v) (flush)
  (Thread/sleep 200)
  v)

(defn process
  [v]
  (printf "Processing: %d \n" v) (flush)
  (Thread/sleep 600)
  v)

(->> (my-lazy-seq)
     (map load)
     (map process)
     (take 10)
     (doall))
;; the interleaving is cuz up until the doall, all the calls return a lazy seq
;; and first the map returns a lazy seq chain of all the my-lazy-seq elems applying
;; the load fn, then this result of lazy seq is pased to the other map
;; and for each one it maps the process call. So each lazy seq is something like:
;; lazy seq: (process (load 1)), (process (load 2)) ...
;;
;; Loading: 0
;; Processing: 0
;; Loading: 1
;; Processing: 1
;; Loading: 2
;; Processing: 2
;; Loading: 3
;; Processing: 3
;; Loading: 4
;; Processing: 4
;; Loading: 5
;; Processing: 5
;; Loading: 6
;; Processing: 6
;; Loading: 7
;; Processing: 7
;; Loading: 8
;; Processing: 8
;; Loading: 9
;; Processing: 9
;; => (0 1 2 3 4 5 6 7 8 9)

;; Chunked sequences
(def t (->> (range 100)
           (map load)
           (map process)
           (take 10)))
;; nothing is realized

(->> (range 100)
     (map load)
     (map process)
     (take 10)
     (doall))
;; Loading: 0
;; Loading: 1
;; Loading: 2
;; Loading: 3
;; Loading: 4
;; Loading: 5
;; Loading: 6
;; Loading: 7
;; Loading: 8
;; Loading: 9
;; Loading: 10
;; Loading: 11
;; Loading: 12
;; Loading: 13
;; Loading: 14
;; Loading: 15
;; Loading: 16
;; Loading: 17
;; Loading: 18
;; Loading: 19
;; Loading: 20
;; Loading: 21
;; Loading: 22
;; Loading: 23
;; Loading: 24
;; Loading: 25
;; Loading: 26
;; Loading: 27
;; Loading: 28
;; Loading: 29
;; Loading: 30
;; Loading: 31
;; Processing: 0
;; Processing: 1
;; Processing: 2
;; Processing: 3
;; Processing: 4
;; Processing: 5
;; Processing: 6
;; Processing: 7
;; Processing: 8
;; Processing: 9
;; Processing: 10
;; Processing: 11
;; Processing: 12
;; Processing: 13
;; Processing: 14
;; Processing: 15
;; Processing: 16
;; Processing: 17
;; Processing: 18
;; Processing: 19
;; Processing: 20
;; Processing: 21
;; Processing: 22
;; Processing: 23
;; Processing: 24
;; Processing: 25
;; Processing: 26
;; Processing: 27
;; Processing: 28
;; Processing: 29
;; Processing: 30
;; Processing: 31
;; => (0 1 2 3 4 5 6 7 8 9)

;; does this cuz the second map calls to clojure.core/seq
;; and inside does an unwrap that gets the coll of the
;; first map, that is the (range 100) and is a chuncked-seq? ture
;; that's why it realize the first chunk of 32 for loading then the first chunk
;; of 32 for processing.
;; See the process:
;; 1. map calls seq on coll
;; https://github.com/clojure/clojure/blob/4d409434bbda460500804556ee0698744626936a/src/clj/clojure/core.clj#L2764
;; 2. seq on clojure.core calls RT.seq
;; https://github.com/clojure/clojure/blob/4d409434bbda460500804556ee0698744626936a/src/clj/clojure/core.clj#L139
;; 3. .seq of the LazySeq
;; https://github.com/clojure/clojure/blob/4d409434bbda460500804556ee0698744626936a/src/jvm/clojure/lang/RT.java#L551
;; 4. in LazySeq.java does an unwrap
;; https://github.com/clojure/clojure/blob/4d409434bbda460500804556ee0698744626936a/src/jvm/clojure/lang/LazySeq.java#L93

;; Buffered sequences ;;
;; Their main goal is to minimize the wait time for consumers by storing a set number
;; of realized items in a buffer (or queue). Buffered sequences maintain a fixed-length buffer.
;; A Clojure agent works diligently to ensure the buffer is consistently populated with realized items.
;; Consequently, when the consumer requires a new item for processing, multiple items are readily available.
;;
;;The Clojure core to build such sequences is seque.

(->> (my-lazy-seq)
  (map load)
  (seque 5)    ;; buffer of 5
  (map process)
  (take 10)
  (doall))

;; Loading: 0
;; Loading: 1
;; Loading: 2
;; Processing: 0
;; Loading: 3
;; Loading: 4
;; Processing: 1
;; Loading: 5
;; Loading: 6
;; Loading: 7
;; Processing: 2
;; Loading: 8
;; Loading: 9
;; Processing: 3
;; Loading: 10
;; Processing: 4
;; Loading: 11
;; Processing: 5
;; Loading: 12
;; Processing: 6
;; Loading: 13
;; Loading: 14
;; Processing: 7
;; Processing: 8
;; Loading: 15
;; Loading: 16
;; Processing: 9
;; => (0 1 2 3 4 5 6 7 8 9)
;;
;;From the output, it’s evident how the agent works. Loading: and Processing:
;;statements are interleaved in a seemingly random order. This is attributable
;;to the agent operating on a separate thread in the background. By introducing
;;a buffered sequence between the load and process we have a sort of “pre-fetcher”
;;which attempts to keep at most 5 items always realized and ready to be processed.
;;As items are consumed from the buffer the agent tries to replace them with new items
;;from the unrealized sequence upstream. The key difference compared to the chunked
;;sequences is that while the chunked sequences do not realize items until the first
;;item of a chunk is requested, buffered sequences realize enough items to fill the
;;buffer ahead of the consumption.

;; One thing to notice is that seque is semi-lazy as it will try to fill the buffer as
;; soon as it called. We can see this clearly if we define a var without consuming the sequence.

(def processing-seq
  (->> (my-lazy-seq)
    (map load)
    (seque 5) ;; buffer of 5
    (map process)
    (take 10)))

;; Loading: 0
;; Loading: 1
;; Loading: 2
;; Loading: 3
;; Loading: 4
;; Loading: 5
;; Loading: 6
;; #'user/processing-seq
;;
;;Even though we created a buffer of 5 items we can see that there are 6 items
;;realized here. The buffer is backed by a blocking queue. The agent tries to
;;continuously push new items to the buffer, so when the buffer is full, there
;;is one more item being consumed from the upstream sequence by the agent who
;;is blocked on the queue offer operation. As soon as an item is consumed,
;;the offer will be accepted and the in-flight item will be inserted into the queue.


;; Parallel pre-fetcher ;;
;; With a small change we can load the items in parallel. That’s useful when the
;; upstream operation (load in this case) is dominated by IO.

(->> (my-lazy-seq)
     (map (fn [v] (future (load v)))) ;; load in parallel
     (seque 5)     ;; buffer up to 5 items (+1 in-flight)
     (map deref)   ;; deref the future
     (map process)
     (take 10)
     (doall))
;; Loading: 2
;; Loading: 3
;; Loading: 4
;; Loading: 6
;; Loading: 0
;; Loading: 5
;; Loading: 1
;; Loading: 7
;; Processing: 0
;; Processing: 1
;; Loading: 8
;; Processing: 2
;; Loading: 9
;; Processing: 3
;; Loading: 10
;; Processing: 4
;; Loading: 11
;; Processing: 5
;; Loading: 12
;; Processing: 6
;; Loading: 13
;; Processing: 7
;; Loading: 14
;; Processing: 8
;; Loading: 15
;; Processing: 9
;; Loading: 16
;; => (0 1 2 3 4 5 6 7 8 9)

;; We can easily isolate this pattern and create a pre-fetch function:

(defn pre-fetch
  "Returns a semi-lazy sequence consisting of the result of applying
   `f` to items of `coll`. It is semi-lazy as it will attempt to keep
   always `n` items in a buffer + 1 item in flight.
   Useful to minimize the waiting time of a consumer."
  [n f coll]
  (->> coll
    (map (fn [i] (future (f i))))
    (seque n)
    (map deref)))

;; and then use it as:

(->> (my-lazy-seq)
  (pre-fetch 5 load)
  (map process)
  (take 10)
  (doall))


;; What about pmap? ;;
;; pmap has a similar behaviour but you can’t control how many items are realized.
;; It depends on the number of CPU cores in available in the runtime machine.
;; pmap always uses (+ 2 (.. Runtime getRuntime availableProcessors)) threads.
(->> (my-lazy-seq)
  (pmap load)
  (map process)
  (take 10)
  (doall))

;; Loading: 0
;; Loading: 8
;; Loading: 9
;; Loading: 6
;; Loading: 7
;; Loading: 4
;; Loading: 5
;; Loading: 3
;; Loading: 2
;; Loading: 1
;; Loading: 10
;; Loading: 11
;; Loading: 12
;; Loading: 13
;; Loading: 14
;; Processing: 0
;; Processing: 1
;; Loading: 15
;; Processing: 2
;; Loading: 16
;; Processing: 3
;; Loading: 17
;; Processing: 4
;; Loading: 18
;; Processing: 5
;; Loading: 19
;; Processing: 6
;; Loading: 20
;; Processing: 7
;; Loading: 21
;; Processing: 8
;; Loading: 22
;; Processing: 9
;; Loading: 23

;; => (0 1 2 3 4 5 6 7 8 9)

;;ME: My laptop has 12 CPU cores so the load ran on 14 items.

;;Conclusions ;;
;; Throughout this post, we’ve delved into various types of lazy sequences and examined their influence on the execution of mapping functions. We explored three distinct categories of lazy sequences:
;;
;; - The “truly” lazy sequences, which realize items individually as they are requested.
;; - The chunked sequences, which process multiple items simultaneously.
;; - The buffered sequences, which consistently maintain a reservoir of realized items, ready for consumption.
;; - The salient distinction between chunked and buffered sequences lies in their approach to realization. While chunked sequences await a consumer’s request for an item, buffered sequences take a proactive stance, anticipating the need and realizing a batch of items from the preceding sequence.
;;
;; Each of these types occupies its unique niche and finds relevance in various applications. It rests upon us, the developers, to judiciously select the most fitting type tailored to the specific challenges we encounter.
