(ns myapp.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [myapp.config :as config]))

(deftest validate-config-test
  (testing "当 JWT_SECRET 使用默认 dev 值时应抛出异常"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"JWT_SECRET"
          (config/validate! {:jwt    {:secret "dev-secret-change-in-production"}
                             :db     {:jdbc-url "jdbc:postgresql://localhost/db"
                                      :username "u" :password "p"
                                      :maximum-pool-size 10}
                             :server {:port 3000 :join? false}}))))

  (testing "nil secret 应抛出异常"
    (is (thrown?
          clojure.lang.ExceptionInfo
          (config/validate! {:jwt    {:secret nil}
                             :db     {:jdbc-url "jdbc:postgresql://localhost/db"
                                      :username "u" :password "p"
                                      :maximum-pool-size 10}
                             :server {:port 3000 :join? false}}))))

  (testing "合法 config 不抛出异常"
    (is (nil? (config/validate! {:jwt    {:secret "real-production-secret"}
                                 :db     {:jdbc-url "jdbc:postgresql://localhost/db"
                                          :username "u" :password "p"
                                          :maximum-pool-size 10}
                                 :server {:port 3000 :join? false}})))))
