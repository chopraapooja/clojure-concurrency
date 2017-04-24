(ns concurrency.practice)

A state is the value of an identity at a point in time -- Stuart Halloway

Concurrency vs. Parallelism

Clojure reference types - atom, agent, ref, var

What & When to use them ?

;; ==========================================================================================
ATOMS :

(def counter (atom 0))

@counter
(deref counter)

Race condition
(dotimes [_ 100]
  (future (reset! counter (inc @counter))))

(inc @counter) = Read Counter + Inc Counter + Write Counter
Here we need atomicity
Let's try to solve above problem

(defn try-to-inc-counter [counter]
  (let [old-val @counter]
    (when-not (compare-and-set! counter old-val (inc old-val))
      (try-to-inc-counter counter))))

(dotimes [_ 100]
  (future (try-to-inc-counter counter)))

@counter

Clojure provide us the wrraper to do the above thing called as swap!
(dotimes [_ 100]
  (swap! counter inc))

Since swap! retries the given function till it succeeds, we should always give a PURE function to swap!

Atoms are synchronous, returns the updated value of atom

Validators :
(def counter (atom 0 :validator #(<= 0 %)))
(swap! counter dec) ;; => will bomb

Reset atom :
(reset! counter (inc @counter))
;; can see load fn example in sketches

e.g. When to use ATOM ?
A very simple usecase is application config, we don't change config at runtime but it needs to be accessed by all request threads. SEE: sketches config.

;; ==========================================================================================
AGENTS :
When we have asynchronous updates e.g. I/O, network calls

Let's take an example of shared log file.

Let's create problem :
(dotimes [_ 100]
  (future (prn "Hello!")))

(import [java.io BufferedWriter FileWriter])

(def log-file (agent (BufferedWriter. (FileWriter. "file.log"))))

(dotimes [i 100]
  (future (send log-file
                (fn [logger msg]
                  (.write logger msg)
                  logger)
                (format "Message #%s \n" i))))

(send log-file #(.close %))

Think of agents like a queue of function that agent is applying to its state serially in the order in which they come

send [Fixed size Thread-pool] and send-off [Growing Thread-pool]

;; ==========================================================================================

STM :
STM allows updates to multiple places in memory and for those changes to become visible atomically to other threads at the same logical moment. Similar to database transactions, if for some reason all the updates can’t be made then none of the updates are made.
