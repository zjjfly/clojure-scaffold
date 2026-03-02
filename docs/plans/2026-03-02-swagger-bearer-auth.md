# Swagger Bearer Auth Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an Authorize button to Swagger UI so users can set a Bearer JWT token that is sent automatically on protected endpoint requests.

**Architecture:** Add an OpenAPI `securitySchemes` definition to the swagger spec in `core.clj`, then annotate the users route group in `users.clj` with `security: [{bearerAuth: []}]`. Reitit deep-merges route data from parent to child, so one annotation on the group covers all four user handlers.

**Tech Stack:** reitit (routing + swagger), clojure.test + ring.mock.request (testing)

---

### Task 1: Write a failing test for the swagger securitySchemes

**Files:**
- Create: `test/myapp/routes/swagger_test.clj`

**Step 1: Write the failing test**

```clojure
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
```

**Step 2: Run the test to confirm it fails**

```bash
clojure -M:test -m kaocha.runner :unit --focus myapp.routes.swagger-test
```

Expected: FAIL — `bearerAuth` key not found in response body.

---

### Task 2: Add `securitySchemes` to the Swagger spec

**Files:**
- Modify: `src/myapp/routes/core.clj:33-37`

**Step 1: Update the swagger metadata**

In `src/myapp/routes/core.clj`, replace:

```clojure
["/swagger.json"
 {:get {:no-doc  true
        :swagger {:info {:title   "myapp API"
                         :version "1.0.0"}}
        :handler (swagger/create-swagger-handler)}}]
```

With:

```clojure
["/swagger.json"
 {:get {:no-doc  true
        :swagger {:info       {:title   "myapp API"
                               :version "1.0.0"}
                  :components {:securitySchemes
                               {:bearerAuth {:type         "http"
                                             :scheme       "bearer"
                                             :bearerFormat "JWT"}}}}
        :handler (swagger/create-swagger-handler)}}]
```

**Step 2: Run the first test to confirm it passes**

```bash
clojure -M:test -m kaocha.runner :unit --focus myapp.routes.swagger-test/test-swagger-security-scheme
```

Expected: PASS

---

### Task 3: Add `security` annotation to users routes

**Files:**
- Modify: `src/myapp/routes/users.clj:7`

**Step 1: Update the users route group metadata**

In `src/myapp/routes/users.clj`, replace:

```clojure
{:swagger {:tags ["users"]}}
```

With:

```clojure
{:swagger {:tags     ["users"]
           :security [{:bearerAuth []}]}}
```

**Step 2: Run all tests**

```bash
clojure -M:test -m kaocha.runner :unit
```

Expected: All tests PASS including `test-users-routes-have-security`.

**Step 3: Commit**

```bash
git add src/myapp/routes/core.clj src/myapp/routes/users.clj test/myapp/routes/swagger_test.clj
git commit -m "feat: add bearerAuth security scheme to Swagger UI"
```

---

### Task 4: Manual verification

Start the REPL and smoke-test:

```clojure
;; In dev/user.clj REPL
(go)

(require '[ring.mock.request :as mock])
(let [app (get integrant.repl.state/system :myapp/app)]
  (-> (app (mock/request :get "/swagger.json"))
      :body
      (get-in [:components :securitySchemes])))
;; Expected: {:bearerAuth {:type "http", :scheme "bearer", :bearerFormat "JWT"}}
```

Then open `http://localhost:3000/swagger-ui` in a browser and confirm:
- **Authorize** button appears in the top-right
- `/api/v1/users` endpoints show a lock icon
- Clicking "Authorize", entering a token, then "Try it out" sends `Authorization: Bearer <token>`
