# CampusQueue — Final Project Audit & System Verification Report

**Status:** FEATURE FROZEN & INTERVIEW READY  
**Date:** September 2026  
**Backend Port:** `8081`  
**Frontend Port:** `5173`  
**PostgreSQL Port:** `5433` (Host) $\rightarrow$ `5432` (Container)  
**Database:** `campusqueue`  

---

## 1. Project Overview

**CampusQueue** is a small, production-quality, concurrency-safe digital queue and token management system designed for college service offices (e.g., Accounts Office, Placement Cell, Administration Office, Library Help Desk, and Student Services).

### Core Problem Solved
Traditional campus service desks suffer from physical congestion, lack of visibility into wait times, and disorder during peak hours. CampusQueue provides:
* **Students**: Real-time counter status, digital token issuance, position in queue tracking, estimated wait times, and ticket history.
* **Staff**: FIFO queue processing, atomic next-token dispatching, ticket completion with remarks, and no-show skipping.
* **Admins**: Dynamic service desk creation, active/inactive counter status toggling, user management, and system-wide SQL analytics.

---

## 2. Final Architecture

```text
 ┌─────────────────────────────────────────────────────────────────┐
 │                   React + Vite SPA Frontend                     │
 │                   (Plain CSS, Fetch API)                        │
 └──────────────────────────────┬──────────────────────────────────┘
                                │ HTTP Session Cookies (JSESSIONID)
                                │ REST JSON APIs / credentials: 'include'
 ┌──────────────────────────────▼──────────────────────────────────┐
 │               Spring Boot 3.3.4 (Java 21/24)                    │
 │  ┌───────────────────────────────────────────────────────────┐  │
 │  │ Spring Security Filter Chain (Session, BCrypt, RBAC)      │  │
 │  └───────────────────────────┬───────────────────────────────┘  │
 │  ┌───────────────────────────▼───────────────────────────────┐  │
 │  │ REST Controllers (DTOs, Validation, Global Exception Adv) │  │
 │  └───────────────────────────┬───────────────────────────────┘  │
 │  ┌───────────────────────────▼───────────────────────────────┐  │
 │  │ Service Layer (Business Logic, Ownership, State Machine)  │  │
 │  └───────────────────────────┬───────────────────────────────┘  │
 │  ┌───────────────────────────▼───────────────────────────────┐  │
 │  │ Spring Data JPA + Hibernate ORM Layer                     │  │
 │  │ (Pessimistic Locking, Row Locks, FOR UPDATE SKIP LOCKED)  │  │
 │  └───────────────────────────┬───────────────────────────────┘  │
 └──────────────────────────────┼──────────────────────────────────┘
                                │ JDBC Driver / Port 5433
 ┌──────────────────────────────▼──────────────────────────────────┐
 │           PostgreSQL 15 Database (`campusqueue`)                │
 │  - users (BCrypt password_hash, email UK)                       │
 │  - counters (code UK, name UK, is_active)                       │
 │  - tickets (counter_id + token_number UK, status, timestamps)   │
 └─────────────────────────────────────────────────────────────────┘
```

---

## 3. Final Tech Stack

### Frontend
* **Core**: React 18, Vite 8, JavaScript (ES2023)
* **Styling**: Vanilla CSS (Design system with custom CSS tokens, modern typography, glassmorphism, responsive grid layouts)
* **Networking**: Fetch API with `credentials: 'include'` for cross-origin session cookies
* **State Management**: React Context (`AuthContext`) and local state hooks

### Backend
* **Core**: Java 21 / 24, Spring Boot 3.3.4
* **Security**: Spring Security (Session-based auth, BCrypt hashing, RBAC, method security)
* **Web Layer**: Spring Web MVC, Bean Validation (Hibernate Validator)
* **Persistence Layer**: Spring Data JPA, Hibernate 6.5
* **Build Tool**: Apache Maven 3.9+

### Database
* **Production/Dev**: PostgreSQL 15 running in Docker on host port `5433`
* **Automated Testing**: In-memory H2 Database (`application-test.properties` with PostgreSQL mode)

---

## 4. Authentication Architecture

* **Mechanism**: Stateful HTTP session authentication using Spring Security's `SecurityFilterChain` and `HttpSessionSecurityContextRepository`.
* **Flow**:
  1. `POST /api/auth/login` accepts `{ email, password }`.
  2. `CustomUserDetailsService` loads the user by normalized email from PostgreSQL.
  3. `BCryptPasswordEncoder` validates credentials.
  4. On match, Spring Security creates an HTTP session and sends back the `JSESSIONID` cookie with `UserResponse` JSON.
  5. `GET /api/auth/me` validates the session cookie on page refreshes to restore the authenticated state.
  6. `POST /api/auth/logout` invalidates the session (`session.invalidate()`) and clears the `SecurityContextHolder`.

---

## 5. Authorization & RBAC Architecture

CampusQueue implements three strictly separated roles:

| Capability / Resource | STUDENT | STAFF | ADMIN |
| :--- | :---: | :---: | :---: |
| View Active Counters & Queue Status | ✅ | ✅ | ✅ |
| Take / Cancel Token for Self | ✅ | ✅ | ✅ |
| Take / Cancel Token for Other Users | ❌ (403) | ✅ | ✅ |
| Call Next Waiting Student (`/call-next`) | ❌ (403) | ✅ | ✅ |
| Call Specific Token (`/call`) | ❌ (403) | ✅ | ✅ |
| Complete Ticket / Skip Ticket | ❌ (403) | ✅ | ✅ |
| View Analytics & Queue Projections | ❌ (403) | ✅ | ✅ |
| Create New Service Desks (`POST /counters`)| ❌ (403) | ❌ (403) | ✅ |
| Toggle Desk Active Status (`PATCH /toggle`)| ❌ (403) | ❌ (403) | ✅ |
| User Management (`POST /users`, `GET /users`)| ❌ (403) | ❌ (403) | ✅ |

---

## 6. Student Ownership Enforcement

In addition to URL-level role filters, the service layer explicitly enforces resource ownership via `SecurityUtils.enforceUserOwnership()`:
* **Token Creation (`POST /api/tickets`)**: If authenticated as `STUDENT`, `request.getUserId()` MUST equal the authenticated principal ID.
* **Token Cancellation (`POST /api/tickets/{id}/cancel`)**: If authenticated as `STUDENT`, `ticket.getUser().getId()` MUST equal the authenticated principal ID.
* **Viewing Ticket Details (`GET /api/tickets/{id}`)**: Students cannot inspect tickets belonging to other students.
* **Viewing Ticket History (`GET /api/tickets/user/{userId}`)**: Students cannot inspect another student's history.
* **Viewing User Profiles (`GET /api/users/{id}`)**: Students can only view their own user profile.

---

## 7. Queue Concurrency Strategy

### A. Token Generation Concurrency (Scoped Per Counter)
* **Goal**: Guarantee that token numbers are strictly sequential per counter (`ACC-001`, `ACC-002`, `PLC-001`), with no duplicate tokens and no gaps during concurrent requests.
* **Implementation**:
  1. Transaction begins (`@Transactional`).
  2. `counterRepository.findByIdForUpdate(counterId)` executes `SELECT ... FROM counters WHERE id = ? FOR UPDATE` (Row-level lock on Counter).
  3. Other transactions requesting tokens for the **same counter** wait briefly for the lock.
  4. Transactions for **different counters** execute concurrently without blocking.
  5. `ticketRepository.findMaxTokenNumberByCounterId(counterId)` calculates `maxToken + 1`.
  6. New `Ticket` is inserted and committed, releasing the lock.
  7. **Defense in Depth**: Unique composite constraint `uk_tickets_counter_token` on `(counter_id, token_number)` in PostgreSQL prevents duplicate inserts.

### B. Call-Next Concurrency (`FOR UPDATE SKIP LOCKED`)
* **Goal**: Prevent race conditions where two staff members simultaneously calling next receive the same waiting student.
* **Implementation**:
  1. `ticketRepository.findNextWaitingTicketForUpdate(counterId)` executes:
     ```sql
     SELECT * FROM tickets 
     WHERE counter_id = :counterId AND status = 'WAITING' 
     ORDER BY created_at ASC 
     LIMIT 1 
     FOR UPDATE SKIP LOCKED
     ```
  2. PostgreSQL locks the earliest waiting ticket and skips any rows locked by other concurrent staff transactions.
  3. The current CALLED ticket at the desk is atomically auto-completed, and the selected ticket transitions to `CALLED`.

---

## 8. Ticket State Machine

```text
               ┌──────────┐
               │ WAITING  │
               └────┬─────┘
          ┌─────────┼─────────┐
          │         │         │
    (call / next) (skip)  (cancel by user)
          │         │         │
          ▼         │         ▼
     ┌─────────┐    │   ┌───────────┐
     │ CALLED  │    │   │ CANCELLED │ (Terminal)
     └────┬────┘    │   └───────────┘
     ┌────┴────┐    │
 (complete) (skip)  │
     │         │    │
     ▼         ▼    ▼
┌───────────┐ ┌─────────┐
│ COMPLETED │ │ SKIPPED │ (Terminal)
└───────────┘ └─────────┘
 (Terminal)
```

* **Invalid Transitions Rejected with 409 Conflict**:
  * `COMPLETED` $\rightarrow$ `CALLED` (Rejected)
  * `SKIPPED` $\rightarrow$ `COMPLETED` (Rejected)
  * `CANCELLED` $\rightarrow$ `CALLED` (Rejected)
  * `CALLED` $\rightarrow$ `CANCELLED` (Only `WAITING` tickets can be cancelled by students)

---

## 9. Database Schema, Constraints & Indexes

```sql
-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX idx_users_email ON users (email);

-- 2. Counters Table
CREATE TABLE counters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX idx_counters_code ON counters (code);

-- 3. Tickets Table
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    token_number INTEGER NOT NULL,
    counter_id BIGINT NOT NULL REFERENCES counters(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    called_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_tickets_counter_token UNIQUE (counter_id, token_number)
);
CREATE INDEX idx_tickets_counter_status ON tickets (counter_id, status);
CREATE INDEX idx_tickets_user_id ON tickets (user_id);
CREATE INDEX idx_tickets_created_at ON tickets (created_at);
```

---

## 10. REST API Summary (28 Endpoints)

| Method | Path | Role Required | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | **Public** | Service health status |
| `POST` | `/api/auth/login` | **Public** | Session login with email & password |
| `POST` | `/api/auth/logout` | **Public** | Session invalidation |
| `GET` | `/api/auth/me` | `STUDENT, STAFF, ADMIN` | Current user profile |
| `POST` | `/api/users` | `ADMIN` | Administrative user registration |
| `GET` | `/api/users` | `STAFF, ADMIN` | List all users |
| `GET` | `/api/users/{id}` | `STUDENT, STAFF, ADMIN` | User profile (Ownership enforced) |
| `GET` | `/api/counters` | `STUDENT, STAFF, ADMIN` | List all counters |
| `GET` | `/api/counters/active` | `STUDENT, STAFF, ADMIN` | List active counters |
| `GET` | `/api/counters/{id}` | `STUDENT, STAFF, ADMIN` | Get counter details |
| `POST` | `/api/counters` | `ADMIN` | Create new service counter |
| `PATCH`| `/api/counters/{id}/toggle-status`| `ADMIN`| Toggle active status |
| `POST` | `/api/tickets` | `STUDENT, STAFF, ADMIN` | Generate token (Ownership enforced) |
| `GET` | `/api/tickets/{id}` | `STUDENT, STAFF, ADMIN` | Get ticket by ID (Ownership enforced) |
| `POST` | `/api/tickets/{id}/call` | `STAFF, ADMIN` | Call specific ticket |
| `POST` | `/api/tickets/counter/{id}/call-next`| `STAFF, ADMIN` | Call next in FIFO queue |
| `POST` | `/api/tickets/{id}/complete` | `STAFF, ADMIN` | Complete ticket |
| `PATCH`| `/api/tickets/{id}/complete` | `STAFF, ADMIN` | Complete ticket alias |
| `POST` | `/api/tickets/{id}/skip` | `STAFF, ADMIN` | Skip ticket (no-show) |
| `PATCH`| `/api/tickets/{id}/skip` | `STAFF, ADMIN` | Skip ticket alias |
| `POST` | `/api/tickets/{id}/cancel` | `STUDENT, STAFF, ADMIN` | Cancel ticket (Ownership enforced) |
| `PATCH`| `/api/tickets/{id}/cancel` | `STUDENT, STAFF, ADMIN` | Cancel ticket alias |
| `GET` | `/api/tickets/counter/{id}/status` | `STUDENT, STAFF, ADMIN` | Counter queue metrics |
| `GET` | `/api/tickets/counter/{id}/waiting`| `STUDENT, STAFF, ADMIN` | Counter waiting queue |
| `GET` | `/api/tickets/counter/{id}/current`| `STUDENT, STAFF, ADMIN` | Serving ticket |
| `GET` | `/api/tickets/counter/{id}` | `STAFF, ADMIN` | Counter ticket history |
| `GET` | `/api/tickets/user/{userId}` | `STUDENT, STAFF, ADMIN` | Student tickets (Ownership enforced) |
| `GET` | `/api/analytics/**` (7 endpoints) | `STAFF, ADMIN` | Overview, daily volume, busiest, peak hour, stats |

---

## 11. Test Suite Results

### Backend Automated Tests (`mvn clean test`)
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.campusqueue.CampusQueueApplicationTests (10 tests) - PASS
[INFO] Running com.campusqueue.AnalyticsServiceTest (9 tests) - PASS
[INFO] Running com.campusqueue.SecurityAuthorizationTest (20 tests) - PASS
[INFO] Running com.campusqueue.controller.HealthControllerTest (1 test) - PASS
[INFO] Running com.campusqueue.controller.AuthControllerTest (6 tests) - PASS
[INFO] Running com.campusqueue.controller.AnalyticsControllerTest (7 tests) - PASS
[INFO] Running com.campusqueue.controller.CounterControllerTest (5 tests) - PASS
[INFO] Running com.campusqueue.controller.TicketControllerTest (8 tests) - PASS
[INFO] Running com.campusqueue.controller.UserControllerTest (5 tests) - PASS
[INFO] 
[INFO] Results:
[INFO] Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### End-to-End Test Matrix Verification (`test_matrix.mjs`)
* Total Assertions: **24**
* Passed: **24**
* Failed: **0**

### Frontend Production Build (`npm run build`)
* Modules transformed: **34**
* Build time: **509ms**
* Total Bundle: `dist/assets/index-C_7BJKMd.js` (231.11 kB / gzip: 69.07 kB), `dist/assets/index-DyUSKhto.css` (13.92 kB / gzip: 3.32 kB)
* Errors / Warnings: **0**

---

## 12. Known Limitations & Production Hardening Opportunities

For interview clarity, the following intentional architectural design choices are documented:
1. **HTTP vs HTTPS**: In this local development environment, session cookies are configured for HTTP (`http://localhost:5173` $\leftrightarrow$ `http://localhost:8081`). For production, enable `server.servlet.session.cookie.secure=true` and `SameSite=Strict`.
2. **Polling vs WebSockets**: Queue status refreshes deterministically via REST polling. WebSockets or Server-Sent Events (SSE) could be added in a future enterprise iteration.
3. **Single-Instance Deployment**: Designed as a clean single-instance application using PostgreSQL row locking. Distributed deployments would require session clustering or reverse-proxy sticky sessions.
4. **Rate Limiting**: Can be hardened in production with Bucket4j or Cloudflare rate limiting on `/api/auth/login`.

---

## 13. Final Project Status

**CampusQueue is FEATURE-FROZEN, FULLY VERIFIED, AND READY FOR INTERVIEW DEMO.**
