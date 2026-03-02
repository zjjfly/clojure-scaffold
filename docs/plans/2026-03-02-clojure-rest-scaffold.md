# Clojure REST API Scaffold Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Generate a complete, production-ready Clojure REST API scaffold with Users CRUD, JWT auth, PostgreSQL, and full tooling baseline for team use.

**Architecture:** Flat-layered structure (routes → handlers → db). Reitit router with Malli coercion for request/response validation. Integrant manages component lifecycle from a data-driven config map. JWT middleware protects user routes. tools.build produces a standalone uberjar.

**Tech Stack:** Clojure 1.12, Reitit 0.7.x, Malli 0.16.x, Muuntaja 0.6.x, Integrant 0.13.x, Buddy JWT, next.jdbc + HoneySQL 2.x, HikariCP, Migratus, Timbre, Kaocha, tools.build, Docker, GitHub Actions CI

---

## Task 1: Project Foundation

**Files:**
- Create: `deps.edn`
- Create: `build.clj`
- Create: `.gitignore`

**Step 1: Create `deps.edn`**

```clojure
{:paths ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.12.0"}

        ;; Web
        ring/ring-jetty-adapter           {:mvn/version "1.12.2"}
        metosin/reitit                    {:mvn/version "0.7.2"}
        metosin/muuntaja                  {:mvn/version "0.6.10"}
        metosin/malli                     {:mvn/version "0.16.4"}

        ;; System
        integrant/integrant               {:mvn/version "0.13.1"}
        aero/aero                         {:mvn/version "1.1.6"}

        ;; Auth
        buddy/buddy-sign                  {:mvn/version "3.6.1-359"}
        buddy/buddy-hashers               {:mvn/version "2.0.167"}

        ;; Database
        com.github.seancorfield/next.jdbc {:mvn/version "1.3.939"}
        hikari-cp/hikari-cp               {:mvn/version "3.1.0"}
        com.github.seancorfield/honeysql  {:mvn/version "2.6.1126"}
        migratus/migratus                 {:mvn/version "1.5.4"}
        org.postgresql/postgresql         {:mvn/version "42.7.4"}

        ;; Logging
        com.taoensso/timbre              {:mvn/version "6.6.0"}
        org.slf4j/slf4j-nop              {:mvn/version "2.0.9"}}

 :aliases
 {:dev  {:extra-paths ["dev"]
         :extra-deps  {integrant/repl {:mvn/version "0.3.3"}}}

  :test {:extra-paths ["test"]
         :extra-deps  {lambdaisland/kaocha {:mvn/version "1.91.1392"}
                       ring/ring-mock      {:mvn/version "0.4.0"}}}

  :build {:deps        {io.github.clojure/tools.build {:git/tag "v0.10.5"
                                                        :git/sha "2a21b7a"}}
          :ns-default  build}}}
```

> **Note:** Check [Clojars](https://clojars.org) for the latest stable versions before starting. The above are known-good versions as of early 2026.

**Step 2: Create `build.clj`**

```clojure
(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib     'myapp/myapp)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis     (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'myapp.core}))
```

**Step 3: Create `.gitignore`**

```
/target
/.cpcache
/.clj-kondo/.cache
/.lsp
*.jar
*.class
.DS_Store
.env
```

**Step 4: Verify `deps.edn` resolves**

```bash
cd /Users/zijunjie/Desktop/clojure-scaffold
clojure -P
```

Expected: Dependencies download without errors.

**Step 5: Commit**

```bash
git init
git add deps.edn build.clj .gitignore
git commit -m "feat: add project foundation (deps.edn, build.clj)"
```

---

## Task 2: Resources — Config, Logging, Migrations

**Files:**
- Create: `resources/config.edn`
- Create: `resources/logback.xml`
- Create: `resources/migrations/20260302000001-create-users.up.sql`
- Create: `resources/migrations/20260302000001-create-users.down.sql`
- Create: `scripts/init-test-db.sql`

**Step 1: Create `resources/config.edn`**

```edn
{:db     {:jdbc-url             #or [#env DATABASE_URL
                                     "jdbc:postgresql://localhost:5432/myapp"]
          :username             #or [#env DB_USER "myapp"]
          :password             #or [#env DB_PASSWORD "password"]
          :maximum-pool-size    10}

 :server {:port  #or [#env PORT 3000]
          :join? false}

 :jwt    {:secret   #or [#env JWT_SECRET "dev-secret-change-in-production"]
          :exp-secs 86400}}
```

> `#or` and `#env` are Aero reader tags: `#env FOO` reads the environment variable `FOO`, `#or [a b]` returns the first non-nil value.

**Step 2: Create `resources/logback.xml`**

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  <root level="WARN">
    <appender-ref ref="STDOUT"/>
  </root>
</configuration>
```

**Step 3: Create `resources/migrations/20260302000001-create-users.up.sql`**

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name          VARCHAR(255),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**Step 4: Create `resources/migrations/20260302000001-create-users.down.sql`**

```sql
DROP TABLE IF EXISTS users;
```

**Step 5: Create `scripts/init-test-db.sql`** (used by docker-compose to create test DB)

```sql
CREATE DATABASE myapp_test;
GRANT ALL PRIVILEGES ON DATABASE myapp_test TO myapp;
```

**Step 6: Commit**

```bash
git add resources/ scripts/
git commit -m "feat: add config, migrations, and logging resources"
```

---

## Task 3: Configuration and Database Layer

**Files:**
- Create: `src/myapp/config.clj`
- Create: `src/myapp/db/core.clj`
- Create: `src/myapp/db/migrations.clj`

**Step 1: Create `src/myapp/config.clj`**

```clojure
(ns myapp.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn config
  "Reads config.edn from classpath. Supports #env and #or reader tags."
  []
  (aero/read-config (io/resource "config.edn")))
```

**Step 2: Create `src/myapp/db/core.clj`**

```clojure
(ns myapp.db.core
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]))

(def ^:private query-opts
  {:builder-fn rs/as-unqualified-kebab-maps})

(defn query
  "Execute a HoneySQL map, return a seq of kebab-cased maps."
  [ds sqlmap]
  (jdbc/execute! ds (sql/format sqlmap) query-opts))

(defn query-one
  "Execute a HoneySQL map, return the first row or nil."
  [ds sqlmap]
  (jdbc/execute-one! ds (sql/format sqlmap) query-opts))

(defn execute!
  "Execute a HoneySQL map, return update count vector."
  [ds sqlmap]
  (jdbc/execute! ds (sql/format sqlmap)))
```

> `as-unqualified-kebab-maps` converts `created_at` → `:created-at` automatically.

**Step 3: Create `src/myapp/db/migrations.clj`**

```clojure
(ns myapp.db.migrations
  (:require [migratus.core :as migratus]
            [taoensso.timbre :as log]))

(defn- migratus-config [datasource]
  {:store         :database
   :migration-dir "migrations"
   :db            {:datasource datasource}})

(defn migrate!
  "Run pending migrations. Called at system startup."
  [datasource]
  (log/info "Running database migrations...")
  (migratus/migrate (migratus-config datasource))
  (log/info "Migrations complete."))

(defn rollback!
  "Roll back the most recent migration. For development use only."
  [datasource]
  (migratus/rollback (migratus-config datasource)))
```

**Step 4: Commit**

```bash
git add src/
git commit -m "feat: add config reader, db query helpers, and migrations runner"
```

---

## Task 4: Malli Schemas (User Model)

**Files:**
- Create: `src/myapp/models/user.clj`

**Step 1: Create `src/myapp/models/user.clj`**

```clojure
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
```

**Step 2: Commit**

```bash
git add src/myapp/models/
git commit -m "feat: add Malli user schemas for request and response coercion"
```

---

## Task 5: JWT Middleware

**Files:**
- Create: `src/myapp/middleware/auth.clj`

**Step 1: Write the failing test**

Create `test/myapp/middleware/auth_test.clj`:

```clojure
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
```

**Step 2: Run test to confirm it fails**

```bash
clojure -M:test -m kaocha.runner --focus myapp.middleware.auth-test
```

Expected: FAIL — `myapp.middleware.auth` namespace not found.

**Step 3: Create `src/myapp/middleware/auth.clj`**

```clojure
(ns myapp.middleware.auth
  (:require [buddy.sign.jwt :as jwt]
            [clojure.string :as str]))

(defn jwt-auth
  "Returns a Reitit data-middleware map that validates JWT Bearer tokens.
   Attaches decoded claims to :identity in the request on success."
  [secret]
  {:name ::jwt-auth
   :wrap (fn [handler]
           (fn [request]
             (let [auth-header (get-in request [:headers "authorization"])
                   token       (when auth-header
                                 (second (str/split auth-header #"\s+" 2)))]
               (if token
                 (try
                   (let [claims (jwt/unsign token secret)]
                     (handler (assoc request :identity claims)))
                   (catch Exception _
                     {:status 401
                      :body   {:error "Invalid or expired token"}}))
                 {:status 401
                  :body   {:error "Authorization header required"}}))))})
```

**Step 4: Run tests to confirm they pass**

```bash
clojure -M:test -m kaocha.runner --focus myapp.middleware.auth-test
```

Expected: 3 tests pass.

**Step 5: Commit**

```bash
git add src/myapp/middleware/ test/myapp/middleware/
git commit -m "feat: add JWT auth middleware with tests"
```

---

## Task 6: Auth Handlers (Register + Login)

**Files:**
- Create: `src/myapp/handlers/auth.clj`
- Create: `test/myapp/handlers/auth_test.clj`

**Step 1: Write failing tests**

Create `test/myapp/handlers/auth_test.clj`:

```clojure
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
```

**Step 2: Run tests to confirm failure**

```bash
clojure -M:test -m kaocha.runner --focus myapp.handlers.auth-test
```

Expected: FAIL — namespace not found.

**Step 3: Create `src/myapp/handlers/auth.clj`**

```clojure
(ns myapp.handlers.auth
  (:require [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [myapp.db.core :as db]
            [taoensso.timbre :as log]))

(defn- make-token [user secret exp-secs]
  (let [exp (+ (quot (System/currentTimeMillis) 1000) exp-secs)]
    (jwt/sign {:sub   (str (:id user))
               :email (:email user)
               :exp   exp}
              secret)))

(defn register
  "Handler factory: POST /api/v1/auth/register
   Creates a new user and returns 201 with user data."
  [datasource]
  (fn [{{{:keys [email password name]} :body} :parameters}]
    (if (db/query-one datasource {:select [:id]
                                  :from   [:users]
                                  :where  [:= :email email]})
      {:status 409 :body {:error "Email already registered"}}
      (let [pw-hash (hashers/derive password)
            user    (db/query-one datasource
                                  {:insert-into [:users]
                                   :values      [{:email         email
                                                  :password_hash pw-hash
                                                  :name          name}]
                                   :returning   [:id :email :name :created_at :updated_at]})]
        (log/info "User registered:" email)
        {:status 201 :body user}))))

(defn login
  "Handler factory: POST /api/v1/auth/login
   Returns 200 with JWT token on success, 401 on failure."
  [datasource jwt-secret]
  (fn [{{{:keys [email password]} :body} :parameters}]
    (let [user (db/query-one datasource
                             {:select [:id :email :password_hash]
                              :from   [:users]
                              :where  [:= :email email]})]
      (if (and user (hashers/check password (:password-hash user)))
        (do
          (log/info "User logged in:" email)
          {:status 200 :body {:token (make-token user jwt-secret 86400)}})
        {:status 401 :body {:error "Invalid email or password"}}))))
```

**Step 4: Run tests to confirm they pass**

```bash
clojure -M:test -m kaocha.runner --focus myapp.handlers.auth-test
```

Expected: 3 tests pass.

**Step 5: Commit**

```bash
git add src/myapp/handlers/auth.clj test/myapp/handlers/auth_test.clj
git commit -m "feat: add auth handlers (register, login) with tests"
```

---

## Task 7: User Handlers (CRUD)

**Files:**
- Create: `src/myapp/handlers/users.clj`
- Create: `test/myapp/handlers/users_test.clj`

**Step 1: Write failing tests**

Create `test/myapp/handlers/users_test.clj`:

```clojure
(ns myapp.handlers.users-test
  (:require [clojure.test :refer [deftest is testing]]
            [myapp.handlers.users :as sut]
            [myapp.db.core :as db]))

(def user-fixture
  {:id         #uuid "550e8400-e29b-41d4-a716-446655440000"
   :email      "test@example.com"
   :name       "Test User"
   :created-at (java.util.Date.)
   :updated-at (java.util.Date.)})

(deftest test-list-users
  (testing "returns 200 with list of users"
    (with-redefs [db/query (fn [_ _] [user-fixture])]
      (let [resp ((sut/list-users :mock-ds) {})]
        (is (= 200 (:status resp)))
        (is (= 1 (count (get-in resp [:body :data]))))))))

(deftest test-get-user-found
  (testing "returns 200 with user when found"
    (with-redefs [db/query-one (fn [_ _] user-fixture)]
      (let [resp ((sut/get-user :mock-ds)
                  {:parameters {:path {:id #uuid "550e8400-e29b-41d4-a716-446655440000"}}})]
        (is (= 200 (:status resp)))
        (is (= "test@example.com" (get-in resp [:body :email])))))))

(deftest test-get-user-not-found
  (testing "returns 404 when user does not exist"
    (with-redefs [db/query-one (fn [_ _] nil)]
      (let [resp ((sut/get-user :mock-ds)
                  {:parameters {:path {:id #uuid "550e8400-e29b-41d4-a716-446655440000"}}})]
        (is (= 404 (:status resp)))))))

(deftest test-delete-user
  (testing "returns 204 when user deleted"
    (with-redefs [db/execute! (fn [_ _] [{:next.jdbc/update-count 1}])]
      (let [resp ((sut/delete-user :mock-ds)
                  {:parameters {:path {:id #uuid "550e8400-e29b-41d4-a716-446655440000"}}})]
        (is (= 204 (:status resp)))))))
```

**Step 2: Run tests to confirm failure**

```bash
clojure -M:test -m kaocha.runner --focus myapp.handlers.users-test
```

Expected: FAIL — namespace not found.

**Step 3: Create `src/myapp/handlers/users.clj`**

```clojure
(ns myapp.handlers.users
  (:require [myapp.db.core :as db]))

(def ^:private user-cols
  [:id :email :name :created_at :updated_at])

(defn list-users
  "Handler factory: GET /api/v1/users
   Returns paginated list of all users."
  [datasource]
  (fn [{{{:keys [page limit] :or {page 1 limit 20}} :query} :parameters}]
    (let [offset (* (dec page) limit)
          users  (db/query datasource {:select user-cols
                                       :from   [:users]
                                       :limit  limit
                                       :offset offset})]
      {:status 200 :body {:data users :page page :limit limit}})))

(defn get-user
  "Handler factory: GET /api/v1/users/:id"
  [datasource]
  (fn [{{{:keys [id]} :path} :parameters}]
    (if-let [user (db/query-one datasource {:select user-cols
                                            :from   [:users]
                                            :where  [:= :id id]})]
      {:status 200 :body user}
      {:status 404 :body {:error "User not found"}})))

(defn update-user
  "Handler factory: PUT /api/v1/users/:id"
  [datasource]
  (fn [{{{:keys [id]} :path body :body} :parameters}]
    (let [now     (java.util.Date.)
          updates (cond-> {}
                    (:email body) (assoc :email (:email body))
                    (:name body)  (assoc :name (:name body))
                    true          (assoc :updated_at now))
          user    (db/query-one datasource {:update    :users
                                            :set       updates
                                            :where     [:= :id id]
                                            :returning user-cols})]
      (if user
        {:status 200 :body user}
        {:status 404 :body {:error "User not found"}}))))

(defn delete-user
  "Handler factory: DELETE /api/v1/users/:id"
  [datasource]
  (fn [{{{:keys [id]} :path} :parameters}]
    (let [result (db/execute! datasource {:delete-from [:users]
                                          :where       [:= :id id]})]
      (if (pos? (:next.jdbc/update-count (first result)))
        {:status 204}
        {:status 404 :body {:error "User not found"}}))))
```

**Step 4: Run tests to confirm they pass**

```bash
clojure -M:test -m kaocha.runner --focus myapp.handlers.users-test
```

Expected: 4 tests pass.

**Step 5: Commit**

```bash
git add src/myapp/handlers/users.clj test/myapp/handlers/users_test.clj
git commit -m "feat: add user CRUD handlers with tests"
```

---

## Task 8: Reitit Routes

**Files:**
- Create: `src/myapp/routes/auth.clj`
- Create: `src/myapp/routes/users.clj`
- Create: `src/myapp/routes/core.clj`

**Step 1: Create `src/myapp/routes/auth.clj`**

```clojure
(ns myapp.routes.auth
  (:require [myapp.handlers.auth :as handlers]
            [myapp.models.user   :as model]))

(defn routes [datasource jwt-secret]
  ["/auth"
   {:swagger {:tags ["auth"]}}

   ["/register"
    {:post {:summary    "Register a new user"
            :parameters {:body model/RegisterRequest}
            :responses  {201 {:body model/UserResponse}
                         409 {:body [:map [:error :string]]}}
            :handler    (handlers/register datasource)}}]

   ["/login"
    {:post {:summary    "Login and receive JWT token"
            :parameters {:body model/LoginRequest}
            :responses  {200 {:body model/TokenResponse}
                         401 {:body [:map [:error :string]]}}
            :handler    (handlers/login datasource jwt-secret)}}]])
```

**Step 2: Create `src/myapp/routes/users.clj`**

```clojure
(ns myapp.routes.users
  (:require [myapp.handlers.users :as handlers]
            [myapp.models.user    :as model]))

(defn routes [datasource]
  [""
   {:swagger {:tags ["users"]}}

   [""
    {:get {:summary    "List all users"
           :parameters {:query [:map
                                [:page  {:optional true} pos-int?]
                                [:limit {:optional true} pos-int?]]}
           :responses  {200 {:body [:map
                                    [:data  [:vector model/UserResponse]]
                                    [:page  pos-int?]
                                    [:limit pos-int?]]}}
           :handler    (handlers/list-users datasource)}}]

   ["/:id"
    {:parameters {:path [:map [:id :uuid]]}}

    ["" {:get    {:summary   "Get user by ID"
                  :responses {200 {:body model/UserResponse}
                              404 {:body [:map [:error :string]]}}
                  :handler   (handlers/get-user datasource)}

         :put    {:summary    "Update user"
                  :parameters {:body model/UserUpdateRequest}
                  :responses  {200 {:body model/UserResponse}
                               404 {:body [:map [:error :string]]}}
                  :handler    (handlers/update-user datasource)}

         :delete {:summary   "Delete user"
                  :responses {204 {}
                              404 {:body [:map [:error :string]]}}
                  :handler   (handlers/delete-user datasource)}}]]])
```

**Step 3: Create `src/myapp/routes/core.clj`**

```clojure
(ns myapp.routes.core
  (:require [reitit.ring                        :as ring]
            [reitit.coercion.malli              :as malli-coercion]
            [reitit.ring.coercion               :as rrc]
            [reitit.ring.middleware.muuntaja    :as muuntaja]
            [reitit.ring.middleware.parameters  :as parameters]
            [reitit.swagger                     :as swagger]
            [reitit.swagger-ui                  :as swagger-ui]
            [muuntaja.core                      :as m]
            [myapp.routes.auth                  :as auth]
            [myapp.routes.users                 :as users]
            [myapp.middleware.auth              :as auth-mw]
            [taoensso.timbre                    :as log]))

(defn- wrap-exception
  "Global exception middleware: catches unhandled errors and returns 500."
  []
  {:name ::exception
   :wrap (fn [handler]
           (fn [request]
             (try
               (handler request)
               (catch Exception e
                 (log/error e "Unhandled exception")
                 {:status 500
                  :body   {:error "Internal server error"}}))))})

(defn app
  "Build the Ring application. Called by Integrant on system start."
  [datasource {:keys [secret]}]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc  true
             :swagger {:info {:title   "myapp API"
                              :version "1.0.0"}}
             :handler (swagger/create-swagger-handler)}}]

     ["/api/v1"
      (auth/routes datasource secret)

      ["/users"
       {:middleware [(auth-mw/jwt-auth secret)]}
       (users/routes datasource)]]]

    {:data {:coercion   reitit.coercion.malli/coercion
            :muuntaja   m/instance
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         (wrap-exception)
                         rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/swagger-ui"
                                           :url  "/swagger.json"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body {:error "Not found"}})}))))
```

**Step 4: Commit**

```bash
git add src/myapp/routes/
git commit -m "feat: add Reitit routes for auth and users with Swagger"
```

---

## Task 9: Integrant System and Entry Point

**Files:**
- Create: `src/myapp/system.clj`
- Create: `src/myapp/core.clj`

**Step 1: Create `src/myapp/system.clj`**

```clojure
(ns myapp.system
  (:require [integrant.core     :as ig]
            [hikari-cp.core     :as hikari]
            [ring.adapter.jetty :as jetty]
            [myapp.config       :as config]
            [myapp.db.migrations :as migrations]
            [myapp.routes.core  :as routes]
            [taoensso.timbre    :as log]))

;; ── Component: Database connection pool ──────────────────────────────────

(defmethod ig/init-key :myapp/datasource [_ db-cfg]
  (log/info "Starting database connection pool...")
  (hikari/make-datasource db-cfg))

(defmethod ig/halt-key! :myapp/datasource [_ ds]
  (log/info "Closing database connection pool...")
  (hikari/close-datasource ds))

;; ── Component: DB Migrations (returns datasource for downstream deps) ────

(defmethod ig/init-key :myapp/migrations [_ {:keys [datasource]}]
  (migrations/migrate! datasource)
  datasource)   ;; pass datasource through so :myapp/app can depend on this

;; ── Component: Ring Application ──────────────────────────────────────────

(defmethod ig/init-key :myapp/app [_ {:keys [datasource jwt-config]}]
  (routes/app datasource jwt-config))

;; ── Component: HTTP Server ───────────────────────────────────────────────

(defmethod ig/init-key :myapp/server [_ {:keys [handler port join?]}]
  (log/info "Starting HTTP server on port" port)
  (jetty/run-jetty handler {:port port :join? join?}))

(defmethod ig/halt-key! :myapp/server [_ server]
  (log/info "Stopping HTTP server...")
  (.stop server))

;; ── System config ─────────────────────────────────────────────────────────

(defn config
  "Build Integrant config map from application config."
  []
  (let [cfg (config/config)]
    {:myapp/datasource (get cfg :db)

     :myapp/migrations {:datasource (ig/ref :myapp/datasource)}

     ;; :myapp/app depends on :myapp/migrations (which returns datasource)
     ;; so migrations are guaranteed to complete before the app starts.
     :myapp/app        {:datasource (ig/ref :myapp/migrations)
                        :jwt-config (get cfg :jwt)}

     :myapp/server     {:handler (ig/ref :myapp/app)
                        :port    (get-in cfg [:server :port])
                        :join?   (get-in cfg [:server :join?])}}))
```

**Step 2: Create `src/myapp/core.clj`**

```clojure
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
```

**Step 3: Commit**

```bash
git add src/myapp/system.clj src/myapp/core.clj
git commit -m "feat: add Integrant system and -main entry point"
```

---

## Task 10: REPL Development Setup

**Files:**
- Create: `dev/user.clj`

**Step 1: Create `dev/user.clj`**

```clojure
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
```

**Step 2: Verify REPL startup works**

```bash
clojure -M:dev -e "(require 'user) (println \"REPL namespace loaded OK\")"
```

Expected: `REPL namespace loaded OK` — no errors.

**Step 3: Commit**

```bash
git add dev/
git commit -m "feat: add REPL dev namespace with integrant.repl"
```

---

## Task 11: Kaocha Test Configuration

**Files:**
- Create: `tests.edn`

**Step 1: Create `tests.edn`**

```edn
#kaocha/v1
{:tests [{:id          :unit
           :test-paths  ["test"]
           :source-paths ["src"]}]
 :reporter kaocha.report/documentation
 :color?   true}
```

**Step 2: Run all tests**

```bash
clojure -M:test -m kaocha.runner
```

Expected: All tests in `test/` pass (at minimum the middleware and handler unit tests from Tasks 5–7).

**Step 3: Commit**

```bash
git add tests.edn
git commit -m "feat: add kaocha test runner configuration"
```

---

## Task 12: Docker Setup

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `.dockerignore`

**Step 1: Create `Dockerfile`**

```dockerfile
# ── Stage 1: Build uberjar ─────────────────────────────────────────────────
FROM clojure:tools-deps-1.12.0.1488 AS builder

WORKDIR /app

# Cache dependencies first (layer caching)
COPY deps.edn .
RUN clojure -P

# Copy source and build
COPY . .
RUN clojure -T:build uber

# ── Stage 2: Minimal runtime image ────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/myapp-0.1.0-standalone.jar app.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Create `docker-compose.yml`**

```yaml
version: '3.9'

services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB:       myapp
      POSTGRES_USER:     myapp
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-test-db.sql:/docker-entrypoint-initdb.d/01-test-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U myapp"]
      interval: 5s
      timeout: 5s
      retries: 10

  app:
    build: .
    ports:
      - "3000:3000"
    environment:
      DATABASE_URL: jdbc:postgresql://db:5432/myapp
      DB_USER:      myapp
      DB_PASSWORD:  password
      JWT_SECRET:   change-in-production-please
    depends_on:
      db:
        condition: service_healthy

volumes:
  postgres_data:
```

**Step 3: Create `.dockerignore`**

```
target/
.cpcache/
.git/
.github/
dev/
test/
*.md
.gitignore
.dockerignore
```

**Step 4: Verify Docker build**

```bash
docker-compose build
```

Expected: Build completes successfully.

**Step 5: Smoke-test full stack**

```bash
docker-compose up -d
sleep 5
curl -s http://localhost:3000/swagger-ui | head -5
docker-compose down
```

Expected: Returns HTML for Swagger UI.

**Step 6: Commit**

```bash
git add Dockerfile docker-compose.yml .dockerignore
git commit -m "feat: add Dockerfile and docker-compose for local development"
```

---

## Task 13: GitHub Actions CI

**Files:**
- Create: `.github/workflows/ci.yml`

**Step 1: Create `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    name: Test & Build
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB:       myapp_test
          POSTGRES_USER:     myapp
          POSTGRES_PASSWORD: password
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 10

    steps:
      - uses: actions/checkout@v4

      - name: Setup Clojure CLI
        uses: DeLaGuardo/setup-clojure@12
        with:
          cli: latest

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key:  ${{ runner.os }}-maven-${{ hashFiles('deps.edn') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Download dependencies
        run: clojure -P && clojure -P -M:test

      - name: Run tests
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/myapp_test
          DB_USER:      myapp
          DB_PASSWORD:  password
          JWT_SECRET:   ci-test-secret
        run: clojure -M:test -m kaocha.runner

      - name: Build uberjar
        run: clojure -T:build uber

      - name: Upload uberjar
        uses: actions/upload-artifact@v4
        with:
          name: myapp-jar
          path: target/myapp-0.1.0-standalone.jar
```

**Step 2: Commit**

```bash
git add .github/
git commit -m "feat: add GitHub Actions CI workflow"
```

---

## Task 14: Final Verification

**Step 1: Run all unit tests**

```bash
clojure -M:test -m kaocha.runner
```

Expected: All unit tests pass.

**Step 2: Start full stack and run smoke tests**

```bash
docker-compose up -d
sleep 8

# Register a user
curl -s -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password123","name":"Admin"}' | jq .

# Login
TOKEN=$(curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password123"}' | jq -r .token)

echo "Token: $TOKEN"

# List users
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:3000/api/v1/users | jq .

docker-compose down
```

Expected: All three requests succeed with correct status codes.

**Step 3: Build uberjar**

```bash
clojure -T:build uber
ls -lh target/*.jar
```

Expected: `target/myapp-0.1.0-standalone.jar` exists, ~20–40 MB.

**Step 4: Final commit**

```bash
git add .
git commit -m "feat: complete Clojure REST API scaffold"
```

---

## Directory Structure (Final)

```
clojure-scaffold/
├── deps.edn
├── build.clj
├── tests.edn
├── .gitignore
├── .dockerignore
├── Dockerfile
├── docker-compose.yml
├── dev/
│   └── user.clj
├── docs/
│   └── plans/
│       ├── 2026-03-02-clojure-rest-scaffold-design.md
│       └── 2026-03-02-clojure-rest-scaffold.md
├── resources/
│   ├── config.edn
│   ├── logback.xml
│   └── migrations/
│       ├── 20260302000001-create-users.up.sql
│       └── 20260302000001-create-users.down.sql
├── scripts/
│   └── init-test-db.sql
├── src/
│   └── myapp/
│       ├── core.clj
│       ├── config.clj
│       ├── system.clj
│       ├── db/
│       │   ├── core.clj
│       │   └── migrations.clj
│       ├── handlers/
│       │   ├── auth.clj
│       │   └── users.clj
│       ├── middleware/
│       │   └── auth.clj
│       ├── models/
│       │   └── user.clj
│       └── routes/
│           ├── auth.clj
│           ├── users.clj
│           └── core.clj
├── test/
│   └── myapp/
│       ├── handlers/
│       │   ├── auth_test.clj
│       │   └── users_test.clj
│       └── middleware/
│           └── auth_test.clj
└── .github/
    └── workflows/
        └── ci.yml
```
