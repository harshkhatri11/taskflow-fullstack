
# TaskFlow — Postman Test Plan (Full Role/Ownership Matrix)
 
Run this in the order below. Each phase depends on data created in the previous one — don't skip ahead. Save every returned token/id into Postman **environment variables** as you go (`adminToken`, `managerAToken`, `managerBToken`, `employee1Token`, `employee2Token`, `projectAId`, `projectBId`, `task1Id`, `task2Id`, `comment1Id`, etc.) so later requests can reference them without copy-pasting.
 
---
 
## Phase 0 — Test data setup
 
You need this exact cast of characters for the matrix to mean anything:
 
| Var name | Role | Notes |
|---|---|---|
| `admin` | ADMIN | created directly or seeded |
| `managerA` | MANAGER | owns Project A |
| `managerB` | MANAGER | owns Project B — used to prove A can't touch B's stuff |
| `employee1` | EMPLOYEE | assigned to Task 1 in Project A |
| `employee2` | EMPLOYEE | **not** assigned to any task in Project A — used to prove employees can't see/touch each other's tasks |
 
Setup sequence:
1. Register `managerA`, `managerB`, `employee1`, `employee2` via `POST /api/auth/register` (all will land as EMPLOYEE by default — see 1.1).
2. Promote `managerA`/`managerB` to MANAGER using the ADMIN account (`POST /api/users`, ADMIN-only) — or if `admin` doesn't exist yet, create it first however your project bootstraps the first admin (seed script / DB insert), since there's no self-service path to ADMIN.
3. Log in as each user, save tokens.
4. As `managerA`: create Project A (`POST /api/projects`).
5. As `managerB`: create Project B.
6. As `managerA`: create Task 1 in Project A, assigned to `employee1`.
7. Confirm `employee2` has zero tasks anywhere.
---
 
## Phase 1 — Auth smoke test
 
### 1.1 `POST /api/auth/register`
| # | Case | Body | Expected |
|---|---|---|---|
| 1 | Valid registration | `{name, email, password}` | 201/200, role in response is `EMPLOYEE` regardless of what you send (if you try to sneak `role: ADMIN` in the body, confirm it's ignored) |
| 2 | Duplicate email | same email as #1 | 400/409, clear error message, no stack trace leaked |
| 3 | Missing/invalid fields | omit `email`, or malformed email | 400 with `errors` field-map populated |
| 4 | Weak password | password under your min length | 400 with `errors` map |
 
### 1.2 `POST /api/auth/login`
| # | Case | Expected |
|---|---|---|
| 5 | Correct credentials | 200, body has `accessToken`, `refreshToken`, `expiresIn`, `role` |
| 6 | Wrong password | 401, generic message (don't leak "user exists but wrong password" vs "no such user" — same message either way) |
| 7 | Non-existent email | 401, same generic message as #6 |
| 8 | **Password never in response** | Confirm `password`/`passwordHash` is absent from every one of the above response bodies |
 
### 1.3 Protected route with the fresh token
| # | Case | Expected |
|---|---|---|
| 9 | `GET /api/users/me` with valid `accessToken` | 200, own profile, no password field |
| 10 | Same call, no `Authorization` header | 401 via `JwtAuthenticationEntryPoint` — check the JSON shape matches `ErrorResponse` (`timestamp, status, message, path`) |
| 11 | Same call, garbage token (`Bearer abc123`) | 401, same shape |
| 12 | Same call, expired token (wait out 15 min, or temporarily shrink TTL in config to test faster) | 401, same shape |
 
### 1.4 `POST /api/auth/refresh`
| # | Case | Body | Expected |
|---|---|---|---|
| 13 | Valid refresh | `{userId, refreshToken}` from login | 200, **new** `accessToken` and **new** `refreshToken` (rotation — old refresh token should now be dead, test #15) |
| 14 | Wrong/garbage refresh token | `{userId, refreshToken: "junk"}` | 401 via `InvalidTokenException` (this one **is** caught by `GlobalExceptionHandler`, unlike filter-layer 401 — confirm both look identical to the client) |
| 15 | Reuse the *old* refresh token from #13 after rotation | same old token | 401 — proves rotation actually invalidated it |
 
### 1.5 `POST /api/auth/logout`
| # | Case | Expected |
|---|---|---|
| 16 | Logout with valid access token | 200/204 |
| 17 | Try to refresh using the now-revoked refresh token | 401 — Redis key should be gone (`DEL` on logout) |
| 18 | Old access token still works until its own 15-min expiry | 200 (expected/accepted tradeoff per your design doc — don't treat this as a bug) |
 
---
 
## Phase 2 — User endpoints
 
### 2.1 `GET /api/users`
| # | Token | Expected |
|---|---|---|
| 19 | `admin` | 200, **full** user objects (check actual JSON, not just status) |
| 20 | `managerA` | 200, **summary-only** fields (fewer fields than #19 — diff the two bodies) |
| 21 | `employee1` | 403 |
| 22 | no token | 401 |
 
### 2.2 `POST /api/users`
| # | Token | Body | Expected |
|---|---|---|---|
| 23 | `admin` | valid new user, any role incl. `ADMIN` | 201, role is whatever was sent (admin can set any role) |
| 24 | `managerA` | same valid body | 403 — this was the previously-open hole, confirm it's actually closed now |
| 25 | `employee1` | same | 403 |
 
### 2.3 `DELETE /api/users/{id}`
| # | Token | Expected |
|---|---|---|
| 26 | `admin`, deleting a user with no tasks | 200/204 |
| 27 | `admin`, deleting `employee1` (has an assigned task) | check your guard clause behavior — either 400/409 blocking the delete, or cascading/nulling per your FK design; confirm it matches what you intended, not an unhandled 500 |
| 28 | `managerA` | 403 |
| 29 | `employee1` (self-delete attempt) | 403 |
 
### 2.4 `GET /api/users/me`
| # | Token | Expected |
|---|---|---|
| 30 | any valid token | 200, that user's own profile only, no password |
 
---
 
## Phase 3 — Project endpoints
 
### 3.1 `GET /api/projects`
| # | Token | Expected |
|---|---|---|
| 31 | `admin` | 200, **all** projects (A and B both present) |
| 32 | `managerA` | 200, **only Project A** — confirm Project B is absent from the array, not just that status is 200 |
| 33 | `employee1` | 200, only projects containing tasks assigned to them (Project A, via Task 1) |
| 34 | `employee2` | 200, **empty array** (not assigned to anything) |
 
### 3.2 `GET /api/projects/{id}` — the 404-vs-403 asymmetry, most important test in this phase
| # | Token | Target | Expected |
|---|---|---|---|
| 35 | `admin` | Project A | 200 |
| 36 | `managerA` | Project A (own) | 200 |
| 37 | `managerA` | Project B (**not** own) | **404**, not 403 — this is the deliberate "don't leak existence" behavior, confirm it's actually 404 |
| 38 | `managerA` | a project id that doesn't exist at all (e.g. 99999) | 404 — same status and, ideally, same message shape as #37 (can't distinguish "not yours" from "doesn't exist" from the response) |
| 39 | `employee1` | Project A (has task there) | 200 |
| 40 | `employee2` | Project A (no task there) | 404 |
 
### 3.3 `POST /api/projects`
| # | Token | Body | Expected |
|---|---|---|---|
| 41 | `managerA` | valid, no `managerId` in body | 201, `managerId` auto-set to `managerA`'s own id |
| 42 | `managerA` | valid, but tries to set `managerId` to `managerB`'s id | should be rejected (400) or silently overridden — per your locked decision, MANAGER must never successfully supply `managerId`; confirm which behavior you implemented and that it's consistent |
| 43 | `admin` | valid, explicit `managerId` = `managerB` | 201, project created under `managerB` |
| 44 | `employee1` | valid body | 403 |
| 45 | `managerA` | missing required field (e.g. no `title`) | 400 + `errors` map |
 
### 3.4 `PUT /api/projects/{id}` — opposite asymmetry from 3.2, confirm it's actually 403 here
| # | Token | Target | Expected |
|---|---|---|---|
| 46 | `admin` | Project A | 200 |
| 47 | `managerA` | Project A (own) | 200 |
| 48 | `managerA` | Project B (not own) | **403** — not 404 this time, opposite of `getProjectById` |
| 49 | `employee1` | Project A | 403 |
 
### 3.5 `DELETE /api/projects/{id}`
| # | Token | Target | Expected |
|---|---|---|---|
| 50 | `admin` | any project | 200/204 |
| 51 | `managerA` | Project A (own) | 200/204 |
| 52 | `managerA` | Project B (not own) | 403 |
| 53 | `employee1` | any project | 403 |
| 54 | any token | project with tasks still attached | confirm your intended cascade/block behavior fires, not a raw FK constraint 500 |
 
---
 
## Phase 4 — Task endpoints
 
### 4.1 `GET /api/projects/{projectId}/tasks`
| # | Token | Target project | Expected |
|---|---|---|---|
| 55 | `admin` | Project A | 200, all tasks in it |
| 56 | `managerA` | Project A (own) | 200 |
| 57 | `managerA` | Project B (not own) | 403 or 404 — **check which your gate-vs-scope design produces here** and confirm it's intentional (this endpoint wasn't explicitly called out in the `getProjectById` exception list, so it likely goes through the normal `@PreAuthorize` 403 path — verify) |
| 58 | `employee1` | Project A | 200, but confirm the array is scoped to **only their own tasks**, not every task in the project |
| 59 | `employee2` | Project A | 200, empty array (or 403/404 depending on your design — verify against your intended spec) |
| 60 | any token | with `?page=0&size=1&sort=dueDate,asc` | pagination metadata correct (`totalElements`, `totalPages`, single item returned) |
| 61 | any token | `?sort=priority,desc` | confirm sort actually applies, not silently ignored |
 
### 4.2 `POST /api/projects/{projectId}/tasks`
| # | Token | Body | Expected |
|---|---|---|---|
| 62 | `managerA` | valid, `assignedToId` = `employee1` | 201 |
| 63 | `managerA` | `assignedToId` = a MANAGER's id (not an EMPLOYEE) | 400 — backend must re-validate role of assignee, don't trust client |
| 64 | `managerA` | into Project B (not own) | 403 |
| 65 | `employee1` | any project | 403 |
| 66 | `managerA` | `dueDate` in the past | 400 + `errors` map |
 
### 4.3 `PATCH /api/tasks/{id}/status` — narrow endpoint, test the width carefully
| # | Token | Target task | Body | Expected |
|---|---|---|---|---|
| 67 | `employee1` | Task 1 (own) | `{status: "IN_PROGRESS"}` | 200 |
| 68 | `employee1` | Task 1 | body includes `title`/other fields | confirm those are ignored/rejected — this endpoint should only ever touch `status` |
| 69 | `employee2` | Task 1 (not theirs) | `{status: "DONE"}` | 403 |
| 70 | `managerA` | Task 1 (owns the project) | `{status: "DONE"}` | 200 |
| 71 | `managerB` | Task 1 (not their project) | `{status: "DONE"}` | 403 |
| 72 | `admin` | Task 1 | `{status: "DONE"}` | 200 |
| 73 | `employee1` | invalid status string (`"BANANA"`) | 400 |
 
### 4.4 `PUT /api/tasks/{id}` — full edit, employee must never reach this
| # | Token | Expected |
|---|---|---|
| 74 | `admin` | 200, full edit works |
| 75 | `managerA` (own project's task) | 200 |
| 76 | `managerB` (not their task) | 403 |
| 77 | `employee1` (their **own assigned** task, using the full PUT instead of PATCH) | **403** — this is the key test proving the endpoint split is real; employee must not be able to bypass PATCH's narrowness by hitting PUT directly, even on their own task |
 
### 4.5 `DELETE /api/tasks/{id}`
| # | Token | Expected |
|---|---|---|
| 78 | `admin` | 200/204 |
| 79 | `managerA` (own project) | 200/204 |
| 80 | `managerB` (not own) | 403 |
| 81 | `employee1` (even on their own assigned task) | 403 — employees never delete, full stop |
 
---
 
## Phase 5 — Comment endpoints
 
Confirm actual paths first — Status.md flags these as **inferred, not spec-defined**: `POST/GET /api/tasks/{taskId}/comments`, `DELETE /api/comments/{commentId}`. Adjust below if yours differ.
 
### 5.1 `GET /api/tasks/{taskId}/comments` — broad
| # | Token | Target task | Expected |
|---|---|---|---|
| 82 | `admin` | Task 1 | 200, all comments |
| 83 | `managerA` (owns project) | Task 1 | 200 |
| 84 | `employee1` (assigned to this exact task) | Task 1 | 200 |
| 85 | `employee2` (same **project**, but not assigned to Task 1) | Task 1 | **200** — this is the "broad" half of the asymmetry, confirm employee2 CAN view even though not assigned |
| 86 | `employee2` (unrelated to the project entirely) | Task 1 | 403 |
 
### 5.2 `POST /api/tasks/{taskId}/comments` — narrow, this is the one most likely to be backwards
| # | Token | Target task | Expected |
|---|---|---|---|
| 87 | `employee1` (assigned to Task 1) | Task 1 | 201 |
| 88 | `employee2` (same project, not assigned to Task 1) | Task 1 | **403** — deliberately opposite of test #85. If this returns 200, the asymmetry is backwards; flag and fix before moving on |
| 89 | `managerA` (owns project) | Task 1 | 201 |
| 90 | `managerB` (not their project) | Task 1 | 403 |
 
### 5.3 `DELETE /api/comments/{commentId}`
| # | Token | Comment owner | Expected |
|---|---|---|---|
| 91 | `employee1` | own comment (from #87) | 200/204 |
| 92 | `employee2` | someone else's comment | 403 |
| 93 | `managerA` | any comment in own project, not authored by them | 200/204 — manager override |
| 94 | `admin` | any comment anywhere | 200/204 — admin override |
 
---
 
## Phase 6 — Cross-cutting checks (run once at the end)
 
| # | Check | How |
|---|---|---|
| 95 | No entity ever serialized directly | Skim every 200-response body from the phases above; every field should map to a documented DTO, no unexpected entity-internal fields (e.g. Hibernate proxy artifacts, back-references) |
| 96 | `errors` map shape is consistent | Compare the `errors` field from #3, #4, #45, #66, #73 — same shape every time |
| 97 | 401 vs 403 body shape identical | Diff a 401 response (#10) against a 403 response (#21) — both should be the same `ErrorResponse` shape, differing only in `status`/`message` |
| 98 | `JWT_SECRET` actually read from env, not a hardcoded fallback | Restart the app with the env var unset; startup should fail loudly, not silently sign tokens with a default key |
| 99 | Redis reachability | Kill Redis mid-session, attempt a refresh — should fail cleanly (not 500 with a stack trace), confirms you're not swallowing connection errors |
| 100 | Full regression after any fix | If any test above fails and you patch it, re-run that entire phase, not just the one failing case — ownership-check bugs tend to cluster |
 
---
 
## Suggested Postman organization
 
- One **Postman Collection** with folders matching the phases above (`0 — Setup`, `1 — Auth`, `2 — Users`, `3 — Projects`, `4 — Tasks`, `5 — Comments`, `6 — Cross-cutting`).
- One **Environment** per "identity" isn't necessary — a single environment with `adminToken`, `managerAToken`, `managerBToken`, `employee1Token`, `employee2Token` variables, switched via the `Authorization` header (`Bearer {{managerAToken}}`) per request, is simpler to maintain.
- Use a **Postman "Tests" script** on each request to assert status code automatically (`pm.test("status is 403", () => pm.response.to.have.status(403));`) so you can re-run the whole collection after any backend fix and get a pass/fail summary instead of eyeballing each response.