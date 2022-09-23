#!/usr/bin/env bb

(def pod-cmd
  ; ["clojure" "-M" "-m" "brisk.main"]
  ["./brisk"])

(require '[babashka.pods :as pods])
(pods/load-pod pod-cmd)

(require '[pod.brisk :as brisk])

(defn test-pod
  []
  (let [sample-data {:princess :leia}
        save-file "test.nippy"]
    (println "testing pod...")

    (or
      (do (println "save/thaw file")
          (brisk/freeze-to-file save-file sample-data)
          (when-not (= sample-data (brisk/thaw-from-file save-file))
            (println "Failed to thaw from file.")
            1))

      (do (println "save/thaw string")
          (let [frozen (brisk/freeze-to-string sample-data)]
            (when-not (= sample-data (brisk/thaw-from-string frozen))
              (println "Failed to thaw from string.")
              1)))

      (do (println "save/thaw file (salted password)")
          (brisk/freeze-to-file save-file sample-data {:password [:salted "my-password"]})
          (when-not (= sample-data (brisk/thaw-from-file save-file {:password [:salted "my-password"]}))
            (println "Failed to thaw from file.")
            1))

      (do (println "save/thaw string (salted password)")
          (let [frozen (brisk/freeze-to-string sample-data {:password [:salted "my-password"]})]
            (when-not (= sample-data (brisk/thaw-from-string frozen {:password [:salted "my-password"]}))
              (println "Failed to thaw from string.")
              1)))

      (do (println "save/thaw file (cached password)")
          (brisk/freeze-to-file save-file sample-data {:password [:cached "my-password"]})
          (when-not (= sample-data (brisk/thaw-from-file save-file {:password [:cached "my-password"]}))
            (println "Failed to thaw from file.")
            1))

      (do (println "save/thaw string (cached password)")
          (let [frozen (brisk/freeze-to-string sample-data {:password [:cached "my-password"]})]
            (when-not (= sample-data (brisk/thaw-from-string frozen {:password [:cached "my-password"]}))
              (println "Failed to thaw from string.")
              1)))

      )))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit
    (or (test-pod)
        0)))
