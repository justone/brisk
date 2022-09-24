#!/usr/bin/env bb

(ns test-pod
  (:require [babashka.pods :as pods]
            [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(def pod-cmd
  ; ["clojure" "-M" "-m" "brisk.main"]
  ["./brisk"])

(pods/load-pod pod-cmd)

(require '[pod.brisk :as brisk])

(defn test-pod
  []
  (let [sample-data {:princess :leia}
        save-file "test-pod.nippy"]
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

      (do (println "save/thaw string (wrong password)")
          (let [frozen (brisk/freeze-to-string sample-data {:password [:salted "my-password"]})]
            (try
              (brisk/thaw-from-string frozen {:password [:salted "wrong-password"]})
              (println "Failed to thaw from string.")
              1
              (catch Exception e
                (when-not (string/includes? (ex-message e) "decompression failure")
                  (println "Incorrect message")
                  1)))))

      )))

(defn test-cli
  []
  (let [sample-data {:han :solo}
        save-file "test-cli.nippy"
        password "super-secret"]
    (println "testing cli...")

    (or

      (do (println "save/thaw file")
          (-> (process/process (into pod-cmd ["-f" "-o" save-file]) {:in (pr-str sample-data)})
              (process/check))
          (let [thawed (-> (process/process (into pod-cmd ["-t" "-i" save-file]) {:out :string})
                           (process/check)
                           :out
                           (edn/read-string))]
            (when-not (= thawed sample-data)
              (println "Failed to thaw from file.")
              1)))

      (do (println "save/thaw file (salted password)")
          (-> (process/process (into pod-cmd ["--salted-password" password "-f" "-o" save-file]) {:in (pr-str sample-data)})
              (process/check))
          (let [thawed (-> (process/process (into pod-cmd ["--salted-password" password "-t" "-i" save-file]) {:out :string})
                           (process/check)
                           :out
                           (edn/read-string))]
            (when-not (= thawed sample-data)
              (println "Failed to thaw from file.")
              1)))

      (do (println "save/thaw file (salted password from ENV)")
          (-> (process/process (into pod-cmd ["-f" "-o" save-file]) {:in (pr-str sample-data)
                                                                     :extra-env {"BRISK_SALTED_PASSWORD" password}})
              (process/check))
          (let [thawed (-> (process/process (into pod-cmd ["-t" "-i" save-file]) {:out :string
                                                                                  :extra-env {"BRISK_SALTED_PASSWORD" password}})
                           (process/check)
                           :out
                           (edn/read-string))]
            (when-not (= thawed sample-data)
              (println "Failed to thaw from file.")
              1)))

      )))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit
    (or (test-pod)
        (test-cli)
        0)))
