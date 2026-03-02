(ns myapp.middleware.auth-test
  (:require [clojure.test :refer [deftest is testing]]
            [buddy.sign.jwt :as jwt]
            [myapp.middleware.auth :as sut]))

(def secret "test-secret")

(deftest test-jwt-auth-missing-token
  (testing "returns 401 when Authorization header is absent"
    (let [handler (fn [_] {:status 200})
          mw      ((get-in (sut/jwt-auth secret) [:wrap]) handler)
          resp    (mw {:headers {}})]
      (is (= 401 (:status resp))))))

(deftest test-jwt-auth-invalid-token
  (testing "returns 401 when token is invalid"
    (let [handler (fn [_] {:status 200})
          mw      ((get-in (sut/jwt-auth secret) [:wrap]) handler)
          resp    (mw {:headers {"authorization" "Bearer bad-token"}})]
      (is (= 401 (:status resp))))))

(deftest test-jwt-auth-valid-token
  (testing "passes request to handler and attaches :identity when token is valid"
    (let [claims  {:sub "user-id" :email "a@b.com"}
          token   (jwt/sign claims secret)
          handler (fn [req] {:status 200 :body (:identity req)})
          mw      ((get-in (sut/jwt-auth secret) [:wrap]) handler)
          resp    (mw {:headers {"authorization" (str "Bearer " token)}})]
      (is (= 200 (:status resp)))
      (is (= "a@b.com" (get-in resp [:body :email]))))))
