(ns myapp.routes.auth
  (:require [myapp.handlers.auth :as handlers]
            [myapp.models.user   :as model]))

(defn routes [datasource jwt-secret]
  ["/auth"
   {:swagger {:tags ["auth"]}}

   ["/register"
    {:post {:summary    "Register a new user"
            :parameters {:body model/RegisterRequest}
            :responses  {201 {:body model/UserResponse}
                         409 {:body [:map [:error :string]]}}
            :handler    (handlers/register datasource)}}]

   ["/login"
    {:post {:summary    "Login and receive JWT token"
            :parameters {:body model/LoginRequest}
            :responses  {200 {:body model/TokenResponse}
                         401 {:body [:map [:error :string]]}}
            :handler    (handlers/login datasource jwt-secret)}}]])
