(ns myapp.handlers.users
  (:require [myapp.db.core :as db]))

(def ^:private user-cols
  [:id :email :name :created_at :updated_at])

(defn list-users
  "Handler factory: GET /api/v1/users
   Returns paginated list of all users."
  [datasource]
  (fn [{{{:keys [page limit] :or {page 1 limit 20}} :query} :parameters}]
    (let [offset (* (dec page) limit)
          users  (db/query datasource {:select user-cols
                                       :from   [:users]
                                       :limit  limit
                                       :offset offset})]
      {:status 200 :body {:data users :page page :limit limit}})))

(defn get-user
  "Handler factory: GET /api/v1/users/:id"
  [datasource]
  (fn [{{{:keys [id]} :path} :parameters}]
    (if-let [user (db/query-one datasource {:select user-cols
                                            :from   [:users]
                                            :where  [:= :id id]})]
      {:status 200 :body user}
      {:status 404 :body {:error "User not found"}})))

(defn update-user
  "Handler factory: PUT /api/v1/users/:id"
  [datasource]
  (fn [{{{:keys [id]} :path body :body} :parameters}]
    (let [now     (java.util.Date.)
          updates (cond-> {}
                    (:email body) (assoc :email (:email body))
                    (:name body)  (assoc :name (:name body))
                    true          (assoc :updated_at now))
          user    (db/query-one datasource {:update    :users
                                            :set       updates
                                            :where     [:= :id id]
                                            :returning user-cols})]
      (if user
        {:status 200 :body user}
        {:status 404 :body {:error "User not found"}}))))

(defn delete-user
  "Handler factory: DELETE /api/v1/users/:id"
  [datasource]
  (fn [{{{:keys [id]} :path} :parameters}]
    (let [result (db/execute! datasource {:delete-from [:users]
                                          :where       [:= :id id]})]
      (if (pos? (:next.jdbc/update-count (first result)))
        {:status 204}
        {:status 404 :body {:error "User not found"}}))))
