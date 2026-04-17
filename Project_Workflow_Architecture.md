# Smart Procurement & Vendor Management System - Architecture & Workflow

## 1. Project Purpose
The **Smart Procurement Vendor Management System** is a Spring Boot application designed to streamline and secure the procurement lifecycle within an organization. It provides a centralized platform to manage vendors, create and approve purchase requisitions, generate purchase orders, and handle role-based user access.

## 2. Core Workflows & Actions
The system supports several key business workflows:

*   **Authentication & Authorization:** Users can register and log in to receive a JSON Web Token (JWT). Access to specific endpoints is governed by the user's assigned roles and permissions.
*   **Vendor Management:** Users can onboard new vendors, update their details, manage vendor documents, and track vendor ratings.
*   **Requisition Management:** Employees can create Purchase Requisitions for needed items. These requisitions start in a `PENDING` state and require approval.
*   **Purchase Order (PO) Processing:** Procurement managers can generate Purchase Orders linked to specific vendors. POs contain multiple items and track total costs.
*   **Approval Workflow:** Designated approvers can review `PENDING` Requisitions and Purchase Orders to either `APPROVE` or `REJECT` them, leaving comments for their decisions.
*   **Reporting:** The system can generate downloadable reports (in PDF or Excel formats) summarizing vendor and purchase order activity based on date ranges.

---

## 3. Entity Relationships (Data Model)
The system uses JPA/Hibernate to map Java objects to relational database tables.

### User & Security Entities
*   **`User`**: The central user entity.
    *   *Relationship:* `@ManyToMany` with **`Role`**.
    *   *Relationship:* `@OneToMany` with **`Requisition`** (as the requester).
    *   *Relationship:* `@OneToMany` with **`Approval`** (as the approver).
*   **`Role`**: Represents a job function (e.g., `ROLE_ADMIN`, `ROLE_VENDOR`).
    *   *Relationship:* `@ManyToMany` with **`Permission`**.
*   **`Permission`**: Granular access rights (e.g., `READ_VENDOR`, `CREATE_PO`).

### Procurement Entities
*   **`Vendor`**: Represents a supplier.
    *   *Relationship:* `@OneToMany` with **`VendorDocument`** and **`VendorRating`**.
    *   *Relationship:* `@OneToMany` with **`PurchaseOrder`**.
*   **`PurchaseOrder`**: Represents a finalized order sent to a vendor.
    *   *Relationship:* `@ManyToOne` with **`Vendor`**.
    *   *Relationship:* `@OneToMany` with **`PurchaseOrderItem`** (Cascade ALL).
    *   *Relationship:* `@OneToMany` with **`Approval`**.
*   **`Requisition`**: Internal request for items.
    *   *Relationship:* `@ManyToOne` with **`User`** (requestedBy).
    *   *Relationship:* `@OneToMany` with **`RequisitionItem`** (Cascade ALL).
    *   *Relationship:* `@OneToMany` with **`Approval`**.
*   **`Approval`**: Tracks the audit trail of approvals/rejections for both POs and Requisitions.
    *   *Relationship:* `@ManyToOne` with **`User`** (approver).
    *   *Relationship:* `@ManyToOne` with **`PurchaseOrder`** or **`Requisition`**.

---

## 4. Repositories
The data access layer utilizes Spring Data JPA. All repositories extend `JpaRepository<Entity, Long>`.
*   **Purpose:** They provide out-of-the-box CRUD operations (`save`, `findById`, `findAll`, `deleteById`) without writing SQL.
*   **Custom Queries:** They utilize Spring Data query derivation. For example, `UserRepository` uses `findByUsername(String username)` and `PurchaseOrderRepository` uses `findByVendor(Vendor vendor)` and `findByStatus(String status)`.
*   **Key Repositories:** `UserRepository`, `RoleRepository`, `VendorRepository`, `PurchaseOrderRepository`, `RequisitionRepository`, `ApprovalRepository`.

---

## 5. Services (Business Logic)
Service classes (annotated with `@Service`) sit between Controllers and Repositories to handle business logic.
*   **`AuthService`:** Interacts with the `AuthenticationManager` and `JwtTokenProvider` to validate credentials, generate JWTs, and return user roles.
*   **`PurchaseOrderService`:** Handles the creation logic (e.g., setting timestamps) and updates PO statuses while linking them to `Vendor` entities.
*   **`ReportService`:** Contains the logic to format database data into downloadable PDF or Excel streams.

---

## 6. Security (Spring Security & JWT)
The application secures its endpoints using a stateless, token-based authentication mechanism.

*   **Authentication Flow:**
    1.  User submits credentials to `/api/auth/login`.
    2.  `AuthService` uses `AuthenticationManager` to verify them against the database.
    3.  If valid, `JwtTokenProvider` generates a signed JWT containing the username.
    4.  The client includes this JWT in the `Authorization: Bearer <token>` header of subsequent requests.
*   **`JwtAuthenticationFilter`:** A custom filter (`OncePerRequestFilter`) that intercepts all incoming HTTP requests, extracts the JWT, validates its signature and expiration, and sets the authenticated `User` in the `SecurityContextHolder`.
*   **Role-Based Access Control (RBAC):** `CustomUserDetailsService` loads the user from the database and maps their associated `Roles` and nested `Permissions` into Spring Security `SimpleGrantedAuthority` objects. This allows endpoints to be secured using annotations like `@PreAuthorize("hasRole('ADMIN')")` or `@PreAuthorize("hasAuthority('CREATE_PO')")`.

---

## 7. Key Framework Annotations Used
*   **Controller Layer:** `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@RequestBody`, `@PathVariable`, `@RequestParam`.
*   **Service Layer:** `@Service`, `@Autowired`.
*   **Data/Entity Layer:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@JoinColumn`, `@JoinTable`.
*   **Security Layer:** `@Component`, `@Configuration`, `@EnableWebSecurity`.
