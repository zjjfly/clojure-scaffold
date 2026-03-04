(ns myapp.handlers.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [buddy.hashers :as hashers]
            [myapp.handlers.auth :as sut]
            [myapp.db.core :as db]))

(def secret "test-secret")

(deftest test-login-success
  (testing "returns 200 with token on valid credentials"
    (let [pw-hash (hashers/derive "password123")
          fake-user {:id (java.util.UUID/randomUUID)
                     :email "user@example.com"
                     :password-hash pw-hash}]
      (with-redefs [db/query-one (fn [_ _] fake-user)]
        (let [handler (sut/login :mock-ds secret)
              resp    (handler {:parameters {:body {:email    "user@example.com"
                                                    :password "password123"}}})]
          (is (= 200 (:status resp)))
          (is (string? (get-in resp [:body :token]))))))))

(deftest test-login-wrong-password
  (testing "returns 401 on wrong password"
    (let [pw-hash (hashers/derive "correct-password")
          fake-user {:id (java.util.UUID/randomUUID)
                     :email "user@example.com"
                     :password-hash pw-hash}]
      (with-redefs [db/query-one (fn [_ _] fake-user)]
        (let [handler (sut/login :mock-ds secret)
              resp    (handler {:parameters {:body {:email    "user@example.com"
                                                    :password "wrong-password"}}})]
          (is (= 401 (:status resp))))))))

(deftest test-login-user-not-found
  (testing "returns 401 when user does not exist"
    (with-redefs [db/query-one (fn [_ _] nil)]
      (let [handler (sut/login :mock-ds secret)
            resp    (handler {:parameters {:body {:email    "nobody@example.com"
                                                  :password "password"}}})]
        (is (= 401 (:status resp)))))))
