(ns myapp.core
  (:require [integrant.core :as ig]
            [myapp.system   :as system]
            [taoensso.timbre :as log])
  (:gen-class))

(defn -main [& _args]
  (let [cfg (system/config)]
    (ig/load-namespaces cfg)
    (let [sys (ig/init cfg)]
      (.addShutdownHook
       (Runtime/getRuntime)
       (Thread. (fn []
                  (log/info "Shutdown signal received — halting system.")
                  (ig/halt! sys))))
      (log/info "System started. Ready to serve."))))
