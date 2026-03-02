(ns user
  "REPL development namespace.
   Start/stop/reset the system without restarting the JVM.

   Usage:
     (go)       ; start the system
     (halt)     ; stop the system
     (reset)    ; reload changed namespaces and restart
     (reset-all); reload all namespaces and restart"
  (:require [integrant.repl       :as ir]
            [integrant.repl.state :as irs]
            [myapp.system         :as system]))

(ir/set-prep! #(system/config))

(def go        ir/go)
(def halt      ir/halt)
(def reset     ir/reset)
(def reset-all ir/reset-all)

(comment
  ;; Start system
  (go)

  ;; Access running components (after (go))
  irs/system

  ;; Reload code after changes
  (reset)

  ;; Run a quick smoke test in the REPL
  (require '[ring.mock.request :as mock])
  (let [app (get irs/system :myapp/app)]
    (app (mock/request :get "/swagger-ui"))))
