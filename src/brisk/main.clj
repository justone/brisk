(ns brisk.main
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.tools.cli :refer [parse-opts]]

    [taoensso.nippy :as nippy]

    [brisk.lib.opts :as opts]
    [brisk.lib.string]
    )
  (:import
    [java.io Writer OutputStream ByteArrayOutputStream])
  (:gen-class))

(set! *warn-on-reflection* true)

(def progname "brisk")

(defn perr
  [& parts]
  (binding [*out* *err*]
    (apply println parts)))

;; Core functions

(defn get-source
  [options]
  (or (some->> (:input options) io/input-stream)
      System/in))

(defn get-output
  [options]
  (or (some->> (:output options) io/output-stream)
      System/out))

(defn bytes-from-stream
  [stream]
  (with-open [baos (ByteArrayOutputStream.)]
    (io/copy stream baos)
    (.toByteArray baos)))

(defn freeze
  [options]
  (let [data (edn/read-string (slurp (get-source options)))
        out (get-output options)
        frozen (nippy/freeze data)]
    (io/copy frozen out)
    (.close ^OutputStream out)))

(defn thaw
  [options]
  (let [in (bytes-from-stream (get-source options))
        out (io/writer (get-output options))
        thawed (nippy/thaw in)]
    (.write ^Writer out (pr-str thawed))
    (.close ^Writer out)))


;; Main

(def cli-options
  [["-h" "--help" "Show help"]
   ["-f" "--freeze" "Freeze mode"]
   ["-t" "--thaw" "Thaw mode"]
   ["-i" "--input FILENAME" "Input file"]
   ["-o" "--output FILENAME" "Output file"]
   ])

(def help
  (brisk.lib.string/dedent
    "    "
    "Command line interface to nippy data.

    Select mode of operation with --thaw or --freeze.  Specify input/output
    targets with --input and --output. If either side is not present, stdin or
    stdout will be used."))

(defn find-errors
  [parsed]
  (or (opts/find-errors parsed)
      (let [{:keys [options]} parsed]
        (cond
          (and (not (:thaw options)) (not (:freeze options)))
          {:exit 1}))))

(defn -main [& args]
  (let [parsed (parse-opts args cli-options)
        {:keys [options]} parsed]
    (or (when-some [errors (find-errors parsed)]
          (->> (opts/format-help progname help parsed errors)
               (opts/print-and-exit)))
        (cond
          (:freeze options) (freeze options)
          (:thaw options) (thaw options)))))