(ns myapp.models.user)

;; ── Request schemas ────────────────────────────────────────────────────────

(def RegisterRequest
  [:map
   [:email    [:string {:min 1 :max 255}]]
   [:password [:string {:min 8 :max 128}]]
   [:name     {:optional true} [:maybe :string]]])

(def LoginRequest
  [:map
   [:email    :string]
   [:password :string]])

(def UserUpdateRequest
  [:map {:closed false}
   [:email {:optional true} [:string {:min 1 :max 255}]]
   [:name  {:optional true} [:maybe :string]]])

;; ── Response schemas ───────────────────────────────────────────────────────

(def UserResponse
  [:map
   [:id         :uuid]
   [:email      :string]
   [:name       [:maybe :string]]
   [:created-at inst?]
   [:updated-at inst?]])

(def TokenResponse
  [:map
   [:token :string]])
