(ns myapp.routes.swagger-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [muuntaja.core :as m]
            [myapp.routes.core :as sut]))

(def app (sut/app :mock-ds {:secret "test-secret"}))

(defn- swagger-body []
  (let [resp (app (mock/request :get "/swagger.json"))]
    (m/decode m/instance "application/json" (:body resp))))

(deftest test-swagger-security-definition
  (testing "swagger.json includes bearerAuth securityDefinition (Swagger 2.0)"
    (let [body (swagger-body)
          sd   (get-in body [:securityDefinitions :bearerAuth])]
      (is (= 200 (:status (app (mock/request :get "/swagger.json")))))
      (is (= "apiKey" (:type sd)))
      (is (= "Authorization" (:name sd)))
      (is (= "header" (:in sd))))))

(deftest test-users-routes-have-security
  (testing "user list route has bearerAuth security requirement"
    (let [body      (swagger-body)
          users-get (get-in body [:paths (keyword "/api/v1/users") :get])]
      (is (= [{:bearerAuth []}] (:security users-get))))))
