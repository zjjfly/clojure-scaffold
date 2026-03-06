(ns myapp.system
  (:require [integrant.core     :as ig]
            [hikari-cp.core     :as hikari]
            [ring.adapter.jetty :as jetty]
            [myapp.config       :as config]
            [myapp.db.migrations :as migrations]
            [myapp.routes.core  :as routes]
            [taoensso.timbre    :as log]))

;; ── Component: Database connection pool ──────────────────────────────────

(defmethod ig/init-key :myapp/datasource [_ db-cfg]
  (log/info "Starting database connection pool...")
  (hikari/make-datasource db-cfg))

(defmethod ig/halt-key! :myapp/datasource [_ ds]
  (log/info "Closing database connection pool...")
  (hikari/close-datasource ds))

;; ── Component: DB Migrations (returns datasource for downstream deps) ────

(defmethod ig/init-key :myapp/migrations [_ {:keys [datasource]}]
  (migrations/migrate! datasource)
  datasource)   ;; pass datasource through so :myapp/app can depend on this

;; ── Component: Ring Application ──────────────────────────────────────────

(defmethod ig/init-key :myapp/app [_ {:keys [datasource jwt-config]}]
  (routes/app datasource jwt-config))

;; ── Component: HTTP Server ───────────────────────────────────────────────

(defmethod ig/init-key :myapp/server [_ {:keys [handler port join?]}]
  (log/info "Starting HTTP server on port" port)
  (jetty/run-jetty handler {:port port :join? join?}))

(defmethod ig/halt-key! :myapp/server [_ server]
  (log/info "Stopping HTTP server...")
  (.stop server))

;; ── System config ─────────────────────────────────────────────────────────

(defn config
  "Build Integrant config map from application config."
  []
  (let [cfg (config/config)]
    (config/validate! cfg)
    {:myapp/datasource (get cfg :db)

     :myapp/migrations {:datasource (ig/ref :myapp/datasource)}

     ;; :myapp/app depends on :myapp/migrations (which returns datasource)
     ;; so migrations are guaranteed to complete before the app starts.
     :myapp/app        {:datasource (ig/ref :myapp/migrations)
                        :jwt-config (get cfg :jwt)}

     :myapp/server     {:handler (ig/ref :myapp/app)
                        :port    (get-in cfg [:server :port])
                        :join?   (get-in cfg [:server :join?])}}))
