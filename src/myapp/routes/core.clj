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

(defn app
  "Build the Ring application. Called by Integrant on system start."
  [datasource {:keys [secret]}]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc  true
             :swagger {:info       {:title   "myapp API"
                                    :version "1.0.0"}
                       :components {:securitySchemes
                                    {:bearerAuth {:type         "http"
                                                  :scheme       "bearer"
                                                  :bearerFormat "JWT"}}}}
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
                         rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/swagger-ui"
                                           :url  "/swagger.json"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body {:error "Not found"}})}))))
