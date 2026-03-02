(ns myapp.db.migrations
  (:require [migratus.core :as migratus]
            [taoensso.timbre :as log]))

(defn- migratus-config [datasource]
  {:store         :database
   :migration-dir "migrations"
   :db            {:datasource datasource}})

(defn migrate!
  "Run pending migrations. Called at system startup."
  [datasource]
  (log/info "Running database migrations...")
  (migratus/migrate (migratus-config datasource))
  (log/info "Migrations complete."))

(defn rollback!
  "Roll back the most recent migration. For development use only."
  [datasource]
  (migratus/rollback (migratus-config datasource)))
