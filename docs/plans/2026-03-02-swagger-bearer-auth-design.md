# Swagger UI Bearer Auth Design

Date: 2026-03-02

## Problem

Swagger UI currently has no way for users to set an `Authorization` header. Protected endpoints under `/api/v1/users` require a `Bearer <JWT>` token, but the Swagger spec includes no `securitySchemes` definition, so the UI shows no Authorize button and no lock icons.

## Chosen Approach

Add OpenAPI `securitySchemes` to the Swagger spec and annotate protected routes with `security` requirements. This is the standard OpenAPI 3.0 approach and requires changes to a single file.

## Design

### Single File Changed

`src/myapp/routes/core.clj`

### Change 1 — Add `securitySchemes` to the top-level Swagger spec

```clojure
:swagger {:info       {:title   "myapp API"
                       :version "1.0.0"}
          :components {:securitySchemes
                       {:bearerAuth {:type         "http"
                                     :scheme       "bearer"
                                     :bearerFormat "JWT"}}}}
```

Result: Swagger UI renders an **Authorize** button. Users paste their JWT token (without the `Bearer ` prefix) and all subsequent requests carry `Authorization: Bearer <token>`.

### Change 2 — Annotate protected routes with `security`

For every handler under the `/api/v1/users` router:

```clojure
:swagger {:security [{:bearerAuth []}]}
```

Affected handlers: GET (list), GET (by id), PUT (update), DELETE.

Result: Each protected endpoint shows a lock icon in the UI and automatically sends the stored token when "Try it out" is used.

## Scope

| File | Change |
|------|--------|
| `src/myapp/routes/core.clj` | Add `securitySchemes`; add `security` to 4 user handlers |

No new dependencies, no middleware changes, no config changes.
