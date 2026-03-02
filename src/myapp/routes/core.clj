(ns myapp.routes.core
  (:require [reitit.ring                        :as ring]
            [reitit.coercion.malli]
            [reitit.ring.coercion               :as rrc]
            [reitit.ring.middleware.muuntaja    :as muuntaja]
            [reitit.ring.middleware.parameters  :as parameters]
            [reitit.swagger                     :as swagger]
            [reitit.swagger-ui                  :as swagger-ui]
            [muuntaja.core                      :as m]
            [myapp.routes.auth                  :as auth]
            [myapp.routes.users                 :as users]
            [myapp.middleware.auth              :as auth-mw]
            [malli.error                        :as me]
            [taoensso.timbre                    :as log]))

(defn- wrap-exception
  "Global exception middleware: catches unhandled errors and returns 500."
  []
  {:name ::exception
   :wrap (fn [handler]
           (fn [request]
             (try
               (handler request)
               (catch Exception e
                 (log/error e "Unhandled exception")
                 {:status 500
                  :body   {:error "Internal server error"}}))))})

(defn- wrap-coercion-errors
  "Formats reitit coercion errors into a clean API response.
   Request errors → 400 {:error ... :details {field [messages]}}.
   Response errors → 500 (logged, details hidden from client)."
  []
  {:name ::coercion-errors
   :wrap (fn [handler]
           (fn [request]
             (try
               (handler request)
               (catch clojure.lang.ExceptionInfo e
                 (let [{:keys [type errors]} (ex-data e)]
                   (case type
                     :reitit.coercion/request-coercion
                     {:status 400
                      :body   {:error   "Validation failed"
                               :details (me/humanize {:errors errors})}}

                     :reitit.coercion/response-coercion
                     (do (log/error e "Response coercion error")
                         {:status 500
                          :body   {:error "Internal server error"}})

                     (throw e)))))))})

(defn app
  "Build the Ring application. Called by Integrant on system start."
  [datasource {:keys [secret]}]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc  true
             :swagger {:info                {:title   "myapp API"
                                            :version "1.0.0"}
                       :securityDefinitions {:bearerAuth {:type "apiKey"
                                                          :name "Authorization"
                                                          :in   "header"}}}
             :handler (swagger/create-swagger-handler)}}]

     ["/api/v1"
      (auth/routes datasource secret)

      ["/users"
       {:middleware [(auth-mw/jwt-auth secret)]}
       (users/routes datasource)]]]

    {:data {:coercion   reitit.coercion.malli/coercion
            :muuntaja   m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         (wrap-exception)
                         (wrap-coercion-errors)
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/swagger-ui"
                                           :url  "/swagger.json"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body {:error "Not found"}})}))))
