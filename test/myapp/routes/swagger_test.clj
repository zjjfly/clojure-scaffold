(ns myapp.routes.swagger-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [muuntaja.core :as m]
            [myapp.routes.core :as sut]))

(def app (sut/app :mock-ds {:secret "test-secret"}))

(defn- swagger-body []
  (let [resp (app (mock/request :get "/swagger.json"))]
    (m/decode m/instance "application/json" (:body resp))))

(deftest test-swagger-security-scheme
  (testing "swagger.json includes bearerAuth securityScheme"
    (let [resp (app (mock/request :get "/swagger.json"))
          body (swagger-body)]
      (is (= 200 (:status resp)))
      (is (= "http"
             (get-in body [:components :securitySchemes :bearerAuth :type])))
      (is (= "bearer"
             (get-in body [:components :securitySchemes :bearerAuth :scheme]))))))

(deftest test-users-routes-have-security
  (testing "user list route has bearerAuth security requirement"
    (let [body     (swagger-body)
          users-get (get-in body [:paths (keyword "/api/v1/users") :get])]
      (is (= [{:bearerAuth []}] (:security users-get))))))
