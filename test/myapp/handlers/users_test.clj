(ns myapp.handlers.users-test
  (:require [clojure.test :refer [deftest is testing]]
            [myapp.handlers.users :as sut]
            [myapp.db.core :as db]))

(def user-fixture
  {:id         #uuid "550e8400-e29b-41d4-a716-446655440000"
   :email      "test@example.com"
   :name       "Test User"
   :created-at (java.util.Date.)
   :updated-at (java.util.Date.)})

(deftest test-list-users
  (testing "returns 200 with list of users"
    (with-redefs [db/query (fn [_ _] [user-fixture])]
      (let [resp ((sut/list-users :mock-ds) {})]
        (is (= 200 (:status resp)))
        (is (= 1 (count (get-in resp [:body :data]))))))))

(deftest test-get-user-found
  (testing "returns 200 with user when found"
    (with-redefs [db/query-one (fn [_ _] user-fixture)]
      (let [resp ((sut/get-user :mock-ds)
                  {:parameters {:path {:id #uuid "550e8400-e29b-41d4-a716-446655440000"}}})]
        (is (= 200 (:status resp)))
        (is (= "test@example.com" (get-in resp [:body :email])))))))

(deftest test-get-user-not-found
  (testing "returns 404 when user does not exist"
    (with-redefs [db/query-one (fn [_ _] nil)]
      (let [resp ((sut/get-user :mock-ds)
                  {:parameters {:path {:id #uuid "550e8400-e29b-41d4-a716-446655440000"}}})]
        (is (= 404 (:status resp)))))))

(deftest test-delete-user
  (testing "returns 204 when user deleted"
    (with-redefs [db/execute! (fn [_ _] [{:next.jdbc/update-count 1}])]
      (let [resp ((sut/delete-user :mock-ds)
                  {:parameters {:path {:id #uuid "550e8400-e29b-41d4-a716-446655440000"}}})]
        (is (= 204 (:status resp)))))))
