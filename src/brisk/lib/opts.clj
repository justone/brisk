(ns brisk.lib.opts
  (:require
    [clojure.string :as string]

    [brisk.lib.string]
    ))

(defn find-errors
  [parsed]
  (let [{:keys [errors options]} parsed
        {:keys [help]} options]
    (cond
      help
      {:exit 0}

      errors
      {:message (string/join "\n" errors)
       :exit 1}
      )))

(def help-fmt
  (brisk.lib.string/dedent
    "    "
    "usage: %s [opts]

    %s

    options:
    %s"))

(defn format-help
  [progname help parsed errors]
  (let [{:keys [summary]} parsed
        {:keys [message plain exit]} errors]
    {:help (if plain message (format help-fmt progname (or message help) summary))
     :exit exit}))

(defn print-and-exit
  [{:keys [help exit]}]
  (println help)
  (System/exit exit))
