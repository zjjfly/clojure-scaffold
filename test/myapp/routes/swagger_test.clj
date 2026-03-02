(ns myapp.routes.swagger-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [myapp.routes.core :as sut]))

(def app (sut/app :mock-ds {:secret "test-secret"}))

(deftest test-swagger-security-scheme
  (testing "swagger.json includes bearerAuth securityScheme"
    (let [resp (app (mock/request :get "/swagger.json"))
          body (:body resp)]
      (is (= 200 (:status resp)))
      (is (= "http"
             (get-in body [:components :securitySchemes :bearerAuth :type])))
      (is (= "bearer"
             (get-in body [:components :securitySchemes :bearerAuth :scheme]))))))

(deftest test-users-routes-have-security
  (testing "user list route has bearerAuth security requirement"
    (let [resp (app (mock/request :get "/swagger.json"))
          paths (get-in resp [:body :paths])
          users-get (get-in paths ["/api/v1/users" :get])]
      (is (= [{:bearerAuth []}] (:security users-get))))))
