(ns brisk.main
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]

    [pod-racer.core :as pod]
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

(defn nippy-opts
  [options]
  (let [{:keys [salted-password cached-password]} options]
    (cond-> {}
      salted-password (assoc :password [:salted salted-password])
      cached-password (assoc :password [:cached cached-password]))))

(defn freeze
  [options]
  (let [data (edn/read-string (slurp (get-source options)))
        opts (nippy-opts options)
        out (get-output options)
        frozen (nippy/freeze data opts)]
    (io/copy frozen out)
    (.close ^OutputStream out)))

(defn thaw
  [options]
  (let [in (bytes-from-stream (get-source options))
        opts (nippy-opts options)
        out (io/writer (get-output options))
        thawed (nippy/thaw in opts)]
    (.write ^Writer out (pr-str thawed))
    (.close ^Writer out)))


;; Main

(def cli-options
  [["-h" "--help" "Show help"]
   ["-f" "--freeze" "Freeze mode"]
   ["-t" "--thaw" "Thaw mode"]
   ["-i" "--input FILENAME" "Input file"]
   [nil "--salted-password PASSWORD" "Salted password, for encryption."
    :default (get (System/getenv) "BRISK_SALTED_PASSWORD")]
   [nil "--cached-password PASSWORD" "Cached password, for encryption."
    :default (get (System/getenv) "BRISK_CACHED_PASSWORD")]
   ["-o" "--output FILENAME" "Output file"]
   ["-v" "--version" "Print version"]
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
          (:version options)
          {:message (string/trim (slurp (io/resource "VERSION")))
           :plain true
           :exit 0}

          (and (not (:thaw options)) (not (:freeze options)))
          {:exit 1}))))

(def pod-config
  {:pod/namespaces
   [{:pod/ns "pod.brisk"
     :pod/vars [{:var/name "freeze-to-file"
                 :var/fn #(count (apply nippy/freeze-to-file %&))}
                {:var/name "thaw-from-file"
                 :var/fn #(apply nippy/thaw-from-file %&)}]}]})

(defn -main [& args]
  (let [parsed (parse-opts args cli-options)
        {:keys [options]} parsed]
    (if (System/getenv "BABASHKA_POD")
      (pod/launch pod-config)
      (or (when-some [errors (find-errors parsed)]
            (->> (opts/format-help progname help parsed errors)
                 (opts/print-and-exit)))
          (cond
            (:freeze options) (freeze options)
            (:thaw options) (thaw options))))))
