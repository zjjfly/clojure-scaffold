(ns myapp.routes.users
  (:require [myapp.handlers.users :as handlers]
            [myapp.models.user    :as model]))

(defn routes [datasource]
  [""
   {:swagger {:tags     ["users"]
             :security [{:bearerAuth []}]}}

   [""
    {:get {:summary    "List all users"
           :parameters {:query [:map
                                [:page  {:optional true} pos-int?]
                                [:limit {:optional true} pos-int?]]}
           :responses  {200 {:body [:map
                                    [:data  [:vector model/UserResponse]]
                                    [:page  pos-int?]
                                    [:limit pos-int?]]}}
           :handler    (handlers/list-users datasource)}}]

   ["/:id"
    {:parameters {:path [:map [:id :uuid]]}}

    ["" {:get    {:summary   "Get user by ID"
                  :responses {200 {:body model/UserResponse}
                              404 {:body [:map [:error :string]]}}
                  :handler   (handlers/get-user datasource)}

         :put    {:summary    "Update user"
                  :parameters {:body model/UserUpdateRequest}
                  :responses  {200 {:body model/UserResponse}
                               404 {:body [:map [:error :string]]}}
                  :handler    (handlers/update-user datasource)}

         :delete {:summary   "Delete user"
                  :responses {204 {}
                              404 {:body [:map [:error :string]]}}
                  :handler   (handlers/delete-user datasource)}}]]])
