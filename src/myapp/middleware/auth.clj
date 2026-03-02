(ns myapp.middleware.auth
  (:require [buddy.sign.jwt :as jwt]
            [clojure.string :as str]))

(defn jwt-auth
  "Returns a Reitit data-middleware map that validates JWT Bearer tokens.
   Attaches decoded claims to :identity in the request on success."
  [secret]
  {:name ::jwt-auth
   :wrap (fn [handler]
           (fn [request]
             (let [auth-header (get-in request [:headers "authorization"])
                   token       (when auth-header
                                 (second (str/split auth-header #"\s+" 2)))]
               (if token
                 (try
                   (let [claims (jwt/unsign token secret {:alg :hs256})]
                     (handler (assoc request :identity claims)))
                   (catch Exception _
                     {:status 401
                      :body   {:error "Invalid or expired token"}}))
                 {:status 401
                  :body   {:error "Authorization header required"}}))))})
