# CampusQueue — Digital Queue Management System

A production-quality, full-stack digital queue-management web application designed for college offices such as the Accounts Office, Placement Cell, Administration Office, Library Help Desk, and Student Services.

Built with **Java 21, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL, React 18, and Vite**.

---

## 📌 Architecture Highlights

* **Pessimistic Concurrency Control**: Scoped per-counter token generation using PostgreSQL row-level locks (`SELECT ... FOR UPDATE`), preventing duplicate token numbers during concurrent student requests.
* **Atomic Queue Dispatch**: Staff "Call Next" utilizes PostgreSQL `FOR UPDATE SKIP LOCKED` to atomically claim the earliest waiting ticket without race conditions.
* **Relational SQL Analytics**: High-performance native PostgreSQL aggregation queries calculating busiest counters, peak traffic hours, daily volume, and single-pass performance summaries.
* **Real Authentication & RBAC**: Session-based authentication with Spring Security and BCrypt password hashing. Three distinct roles: `STUDENT`, `STAFF`, and `ADMIN`.
* **Resource Ownership Enforcement**: Dual-layer defense preventing students from creating, viewing, or cancelling tokens belonging to other users.

---

## 🛠 Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | React 18, Vite 8, JavaScript (ES2023), Vanilla CSS, Fetch API |
| **Backend** | Java 21 / 24, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Hibernate 6.5, Maven |
| **Database** | PostgreSQL 15 (Docker), H2 (Automated Tests) |
| **Testing** | JUnit 5, Spring Boot Test, Spring Security Test, MockMvc |

---

## 🚀 Quick Start

### 1. Start PostgreSQL (Docker)
```bash
cd campusqueue-backend
docker compose up -d
```
*Port: `5433` (Host) $\rightarrow$ `5432` (Container) | Database: `campusqueue`*

### 2. Run Spring Boot Backend
```bash
cd campusqueue-backend
mvn spring-boot:run
```
*Backend runs on `http://localhost:8081`*

### 3. Run React Frontend
```bash
cd campusqueue-frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:5173`*

---

## 🔑 Demo Accounts

| Role | Email | Password | Allowed Access |
| :--- | :--- | :--- | :--- |
| **STUDENT** | `rahul.sharma@college.edu` | `student123` | Student Desk (Token issuance, position tracking, cancel own ticket) |
| **STAFF** | `sunita.rao@college.edu` | `staff123` | Student Desk, Staff Queue (Call next, complete, skip), Analytics |
| **ADMIN** | `admin@college.edu` | `admin123` | Full Access: Counter management, status toggling, analytics |

---

## 🧪 Testing

### Backend Test Suite (71 Tests)
```bash
cd campusqueue-backend
mvn clean test
```

### Frontend Production Build
```bash
cd campusqueue-frontend
npm run build
```

---

## 📋 Documentation

For detailed design specifications, database schema diagrams, concurrency analysis, and the security audit report, see:
* [`FINAL_PROJECT_AUDIT.md`](file:///d:/campusQueue/FINAL_PROJECT_AUDIT.md) — Comprehensive technical verification and audit report.
