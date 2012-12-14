(ns leiningen.partest
  (:require
   [leiningen.core.eval :as eval]
   [leiningen.core.project :as project]))

(defn emit-warmup [f w]
  `(do (println " warmup ...")
       (dotimes [n# ~w]
         (~f -1))))

(defn emit-run [fs ns]
  `(let [start# (System/nanoTime)]
     (~fs ~ns)
     (printf "  #%3d, elapsed: %12.2f msecs\n" ~ns (/ (double (- (System/nanoTime) start#)) 10000000.0))))

(let [fs (gensym "f")
      ps (gensym "p")
      ws (gensym "w")
      ns (gensym "n")]

  (def par-process-form
    `(fn ~[fs ns ws]
       ~(emit-warmup fs ws)
       ~(emit-run fs ns)))

  (def par-future-form
    `(fn ~[fs ps ws]
       ~(emit-warmup fs ws)
       (doseq [fu# (mapv (fn ~[ns] (future ~(emit-run fs ns)))
                         (range ~ps))]
         @fu#)))

  (def ser-form
    `(fn ~[fs ps ws]
       ~(emit-warmup fs ws)
       (dotimes ~[ns ps] ~(emit-run fs ns)))))

(defn one-process [project f p w req form msg exit]
  (eval/eval-in-project
   project
   `(do (print "#####################" ~msg) (flush)
        (~form ~f ~p ~w)
        (flush)
        ~exit)
   req))

(defn multi-process [project f p w req msg exit]
  (print "#####################" msg) (flush)
  (let [fus (mapv (fn [n] (future (eval/eval-in-project
                                  project
                                  `(do (~par-process-form ~f ~n ~w)
                                       (flush)
                                       ~exit)
                                  req)))
                  (range p))]
    (doseq [fu fus]
      @fu)))

(defn partest
  "Parallel benchmark harness: lein partest org.example/test-fn 6 2"
  [project test-fn parallelism & [warmup]]
  (let [f (symbol test-fn)
        p (Long/parseLong parallelism)
        w (Long/parseLong (or warmup "2"))
        req `(require '~(symbol (namespace f)))]
    (one-process (assoc project :eval-in 'subprocess)
                 f p w req ser-form "Run serial ..."
                 `(System/exit 0))
    (one-process (assoc project :eval-in 'subprocess)
                 f p w req par-future-form "Run parallel, same classloader ..."
                 `(System/exit 0))
    (multi-process (assoc project :eval-in 'classloader)
                   f p w req "Run parallel, distinct classloaders ..."
                   nil)
    (multi-process (assoc project :eval-in 'subprocess)
                   f p w req "Run parallel, distinct processes ..."
                   `(System/exit 0))))
