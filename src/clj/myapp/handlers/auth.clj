(ns myapp.handlers.auth
  (:require [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [myapp.db.core :as db]
            [taoensso.timbre :as log]))

(defn- make-token [user secret exp-secs]
  (let [exp (+ (quot (System/currentTimeMillis) 1000) exp-secs)]
    (jwt/sign {:sub   (str (:id user))
               :email (:email user)
               :exp   exp}
              secret)))

(defn register
  "Handler factory: POST /api/v1/auth/register
   Creates a new user and returns 201 with user data."
  [datasource]
  (fn [{{{:keys [email password name]} :body} :parameters}]
    (if (db/query-one datasource {:select [:id]
                                  :from   [:users]
                                  :where  [:= :email email]})
      {:status 409 :body {:error "Email already registered"}}
      (let [pw-hash (hashers/derive password)
            user    (db/query-one datasource
                                  {:insert-into [:users]
                                   :values      [{:email         email
                                                  :password_hash pw-hash
                                                  :name          name}]
                                   :returning   [:id :email :name :created_at :updated_at]})]
        (log/info "User registered:" email)
        {:status 201 :body user}))))

(defn login
  "Handler factory: POST /api/v1/auth/login
   Returns 200 with JWT token on success, 401 on failure."
  [datasource jwt-secret]
  (fn [{{{:keys [email password]} :body} :parameters}]
    (let [user (db/query-one datasource
                             {:select [:id :email :password_hash]
                              :from   [:users]
                              :where  [:= :email email]})]
      (if (and user (hashers/check password (:password-hash user)))
        (do
          (log/info "User logged in:" email)
          {:status 200 :body {:token (make-token user jwt-secret 86400)}})
        {:status 401 :body {:error "Invalid email or password"}}))))
