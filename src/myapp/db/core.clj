(ns myapp.db.core
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]))

(def ^:private query-opts
  {:builder-fn rs/as-unqualified-kebab-maps})

(defn query
  "Execute a HoneySQL map, return a seq of kebab-cased maps."
  [ds sqlmap]
  (jdbc/execute! ds (sql/format sqlmap) query-opts))

(defn query-one
  "Execute a HoneySQL map, return the first row or nil."
  [ds sqlmap]
  (jdbc/execute-one! ds (sql/format sqlmap) query-opts))

(defn execute!
  "Execute a HoneySQL map, return update count vector."
  [ds sqlmap]
  (jdbc/execute! ds (sql/format sqlmap)))
