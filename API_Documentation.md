# Smart Procurement Vendor Management System - API Documentation

This document contains a comprehensive list of all APIs in the system, including their endpoints, HTTP methods, and exact JSON request bodies for easy testing in Postman.

## 1. Authentication (`AuthController`)
Base Path: `/api/auth`

### 1.1. Login
- **Endpoint:** `POST /api/auth/login`
- **Description:** Authenticate a user and receive a JWT token.
- **Request Body (JSON):**
```json
{
  "username": "admin",
  "password": "password123"
}
```

### 1.2. Register
- **Endpoint:** `POST /api/auth/register`
- **Description:** Register a new user in the system.
- **Request Body (JSON):**
```json
{
  "username": "vendor1",
  "password": "password123"
}
```

---

## 2. Procurement & Approvals (`ApprovalController`)
Base Path: `/procurement/approval`

### 2.1. Get All Approvals
- **Endpoint:** `GET /procurement/approval/all`
- **Description:** Retrieve a list of all approvals.

### 2.2. Approve a Purchase Order
- **Endpoint:** `POST /procurement/approval/approve/{poId}?approverId={userId}`
- **Description:** Approve a specific purchase order by its ID. Requires the approver's user ID as a query parameter.
- **Example:** `/procurement/approval/approve/1?approverId=2`

### 2.3. Reject a Purchase Order
- **Endpoint:** `POST /procurement/approval/reject/{poId}?approverId={userId}&reason={reasonText}`
- **Description:** Reject a specific purchase order. Requires approver ID and reason as query parameters.
- **Example:** `/procurement/approval/reject/1?approverId=2&reason=BudgetExceeded`

---

## 3. Purchase Orders (`PurchaseOrderController`)
Base Path: `/procurement/purchase-order`

### 3.1. Create Purchase Order
- **Endpoint:** `POST /procurement/purchase-order/create`
- **Description:** Create a new purchase order along with its items.
- **Request Body (JSON):**
```json
{
  "poNumber": "PO-2026-001",
  "vendor": {
    "id": 1
  },
  "status": "PENDING",
  "items": [
    {
      "itemName": "Laptops",
      "quantity": 10,
      "unitPrice": 1200.50
    },
    {
      "itemName": "Monitors",
      "quantity": 20,
      "unitPrice": 300.00
    }
  ]
}
```

### 3.2. Get All Purchase Orders
- **Endpoint:** `GET /procurement/purchase-order/all`
- **Description:** Retrieve all purchase orders.

### 3.3. Get Purchase Order by ID
- **Endpoint:** `GET /procurement/purchase-order/{id}`
- **Description:** Retrieve a purchase order by its ID.

### 3.4. Update Purchase Order Status
- **Endpoint:** `PATCH /procurement/purchase-order/update-status/{id}?status={newStatus}`
- **Description:** Update the status of a PO.
- **Example:** `/procurement/purchase-order/update-status/1?status=APPROVED`

---

## 4. Requisitions (`RequisitionController`)
Base Path: `/procurement/requisition`

### 4.1. Create Requisition
- **Endpoint:** `POST /procurement/requisition/create`
- **Description:** Create a new requisition. Status defaults to PENDING.
- **Request Body (JSON):**
```json
{
  "requisitionNumber": "REQ-2026-001",
  "requestedBy": {
    "id": 1
  },
  "items": [
    {
      "itemName": "Office Chairs",
      "quantity": 5,
      "estimatedPrice": 150.00
    }
  ]
}
```
*(Note: Item fields may vary slightly depending on RequisitionItem definition, but generally follow name and quantity).*

### 4.2. Get All Requisitions
- **Endpoint:** `GET /procurement/requisition/all`
- **Description:** Retrieve all requisitions in the system.

### 4.3. Get Requisition by ID
- **Endpoint:** `GET /procurement/requisition/{id}`
- **Description:** Retrieve a requisition by its ID.

### 4.4. Update Requisition Status
- **Endpoint:** `PATCH /procurement/requisition/update-status/{id}?status={newStatus}`
- **Description:** Update the status of a requisition.
- **Example:** `/procurement/requisition/update-status/1?status=APPROVED`

---

## 5. Reports (`ReportController`)
Base Path: `/reports`

### 5.1. Generate Vendor Report
- **Endpoint:** `POST /reports/vendor?format={pdf|excel}`
- **Description:** Generate and download a vendor report in PDF or Excel format. `format` query param defaults to `pdf`.
- **Request Body (JSON):** *(All fields are optional for filtering)*
```json
{
  "vendorId": 1,
  "poId": 2,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31"
}
```

---

## 6. Vendors (`VendorController`)
Base Path: `/vendor`

### 6.1. Create Vendor
- **Endpoint:** `POST /vendor/create`
- **Description:** Add a new vendor to the system (defaults to ACTIVE status).
- **Request Body (JSON):**
```json
{
  "name": "Tech Corp Suppliers",
  "email": "contact@techcorp.com",
  "contactNumber": "+1234567890",
  "address": "123 Tech Park, Silicon Valley",
  "status": "ACTIVE"
}
```

### 6.2. Get All Vendors
- **Endpoint:** `GET /vendor/all`
- **Description:** Fetch a list of all vendors.

### 6.3. Get Vendor by ID
- **Endpoint:** `GET /vendor/{id}`
- **Description:** Fetch a single vendor by ID.

### 6.4. Update Vendor
- **Endpoint:** `PUT /vendor/update/{id}`
- **Description:** Update an existing vendor's details.
- **Request Body (JSON):**
```json
{
  "name": "Tech Corp Suppliers Updated",
  "email": "newcontact@techcorp.com",
  "contactNumber": "+0987654321",
  "address": "456 Innovation Drive",
  "status": "ACTIVE"
}
```

### 6.5. Delete Vendor
- **Endpoint:** `DELETE /vendor/delete/{id}`
- **Description:** Delete a vendor by ID.
