(ns myapp.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn config
  "Reads config.edn from classpath. Supports #env and #or reader tags."
  []
  (aero/read-config (io/resource "config.edn")))

(def ^:private dev-jwt-secret "dev-secret-change-in-production")

(defn validate!
  "校验已解析的 config map。校验失败抛出 ExceptionInfo。
   返回 nil 表示校验通过。"
  [cfg]
  (let [secret (get-in cfg [:jwt :secret])]
    (when (str/blank? secret)
      (throw (ex-info "启动失败：JWT_SECRET 未设置，请配置环境变量 JWT_SECRET。"
                      {:key :jwt/secret
                       :hint "请设置环境变量 JWT_SECRET"})))
    (when (= secret dev-jwt-secret)
      (throw (ex-info "启动失败：JWT_SECRET 使用了默认 dev 值，生产环境必须设置真实密钥。"
                      {:key :jwt/secret
                       :hint "请设置环境变量 JWT_SECRET"})))))
