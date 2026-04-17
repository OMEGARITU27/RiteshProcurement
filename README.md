# RiteshProcurement

Smart Procurement and Vendor Management System built with Spring Boot, MySQL, JWT security, and a role-aware web frontend.

## What This Project Does

This system digitizes the complete procurement lifecycle:

- User and vendor onboarding with approval workflow
- Requisition creation and procurement processing
- Purchase order creation and approval decisions
- Vendor portal actions on purchase orders
- Admin governance for users and vendor accounts
- Reporting support (PDF/Excel) and audit-ready workflows

## Tech Stack

- Java 17+
- Spring Boot
- Spring Security (JWT + role-based authorization)
- Spring Data JPA (Hibernate)
- MySQL
- JasperReports (`.jrxml` templates)
- Static frontend (`HTML`, `CSS`, `JavaScript`) served by Spring Boot

## Project Architecture

The codebase follows a layered architecture:

- `controller` - REST APIs per module
- `service` - business logic and workflow rules
- `repository` - database access layer
- `entity` - domain models and relationships
- `dto` + `mapper` - clean API contracts and conversion
- `security` + `config` - authentication/authorization and app config
- `static` - browser frontend integrated into backend app

## Roles and Responsibilities

- `ROLE_ADMIN`
  - Approve/reject users and vendors
  - Manage account lifecycle and admin controls
- `ROLE_PROCUREMENT_MANAGER`
  - Process procurement operations
  - Approve/reject purchase orders
- `ROLE_EMPLOYEE`
  - Create requisitions and participate in internal demand flow
- `ROLE_VENDOR`
  - Login to vendor portal
  - Accept/reject/update purchase order status

## Main Modules

- Authentication (`/api/auth`, `/api/vendor-auth`)
- Admin management (`/api/admin`, `/api/admin/vendor-accounts`)
- Vendor management (`/vendor`)
- Requisition management (`/com/procurement/requisition`)
- Purchase orders (`/com/procurement/purchase-order`)
- Approval workflow (`/com/procurement/approval`)
- Vendor portal (`/api/vendor-portal`)
- Reports (`/reports`)

## Frontend (Integrated)

The frontend is included in:

- `src/main/resources/static/index.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/styles.css`

Once the backend runs, open:

- `http://localhost:8080/`

Frontend features include:

- User and vendor login/register
- Role-aware tab visibility
- Vendor, requisition, and purchase order operations
- Approval actions
- Vendor portal actions
- Admin approval dashboards
- Vendor report download (PDF/Excel)

## Default Admin Login

The app seeds/fixes a default admin user at startup via `DataInitializer`:

- Username: `admin`
- Password: `admin123`
- Email: `adminritesh@gmail.com`

## Local Setup

### 1) Prerequisites

- Java 17 or higher
- MySQL running locally
- Maven (or use included wrapper)

### 2) Configure Database

Edit `src/main/resources/application.properties` if needed:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Create database (default):

- `procurement_db`

### 3) Run Application

Using Maven wrapper:

- Windows: `mvnw.cmd spring-boot:run`
- Linux/macOS: `./mvnw spring-boot:run`

Or build and run:

- `mvn clean install`
- `mvn spring-boot:run`

### 4) Access App

- Frontend: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Configuration Notes

- Server port is configurable with `server.port`
- `spring.jpa.hibernate.ddl-auto=update` is enabled by default
- Mail and admin notification settings are in `application.properties`
- CORS and route security are configured in `SecurityConfig` and `CorsConfig`

## Useful Documentation in Repo

- `API_Documentation.md` - endpoint list with sample payloads
- `Project_Workflow_Architecture.md` - high-level process flow
- `Entity_Structure_and_Examples.md` - entity mapping and examples
- `Security_Workflow_and_File_Usage.md` - security and usage notes

## Typical Demo Flow (For Presentation)

1. Login as admin and review pending approvals
2. Register employee/vendor and show approval workflow
3. Create requisition and purchase order
4. Approve/reject PO from manager/admin path
5. Login as vendor and update PO status from vendor portal
6. Generate and download vendor report

## License

This project includes a `LICENSE` file in the repository root.