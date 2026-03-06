(ns myapp.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [lambdaisland.dotenv :as dotenv]))

(def merged-env
  (let [dotenv-file (io/file ".env")
        dotenv-map (when (.exists dotenv-file)
                     (dotenv/parse-dotenv (slurp ".env") {:expand true}))]
    (merge (into {} (System/getenv)) dotenv-map)))

(defmethod aero/reader 'required-env
  [_ _ var-name]
  (let [k (name var-name)
        v (get merged-env k)]
    (when (str/blank? v)
      (throw (ex-info (str "启动失败：环境变量 " var-name " 未设置")
                      {:var var-name})))
    v))

(defn config
  "Reads config.edn from classpath. Loads .env if present, then reads aero config."
  []
  (aero/read-config (io/resource "config.edn")))
