# Entity Structure and Examples

## 1. Entity Diagram (Mermaid)

```mermaid
erDiagram
    US [[User]] {
        Long id
        String username
        String password
        String email
    }
    RO [[Role]] {
        Long id
        String name
    }
    PE [[Permission]] {
        Long id
        String name
    }
    VE [[Vendor]] {
        Long id
        String companyName
        String contactEmail
        String status
    }
    REQ [[Requisition]] {
        Long id
        String description
        String status
        Date requiredDate
    }
    PO [[PurchaseOrder]] {
        Long id
        String orderNumber
        String status
        Double totalAmount
    }
    APP [[Approval]] {
        Long id
        String status
        String comments
        Date approvalDate
    }

    US }|--|{ RO : "has"
    RO }|--|{ PE : "grants"
    US ||--o{ REQ : "creates (requestedBy)"
    VE ||--o{ PO : "fulfills"
    PO ||--o{ APP : "receives"
    REQ ||--o{ APP : "receives"
    US ||--o{ APP : "approves/rejects"
```

---

## 2. Entity Explanations with Examples

### A. Requisition (The Request)
A **Requisition** is an internal request made by an employee when they need something to be purchased. It acts as a formal "wish list" that requires management approval before a real order is placed.

*   **Example:** John, a software engineer, needs a new laptop because his current one is broken. He logs into the system and creates a **Requisition**. He sets the description to "MacBook Pro M3", sets the required date, and submits it. The requisition starts in a `PENDING` state.

### B. Vendor (The Supplier)
A **Vendor** is an external company or supplier that provides the goods or services your organization wants to purchase. The system tracks their details, documents, and ratings.

*   **Example:** "TechSupplies Inc." is a company that sells computers. They are registered in the system as a **Vendor**. Once John's requisition for a laptop is approved, the company will buy the laptop from "TechSupplies Inc.".

### C. Procurement Manager (The Buyer & Approver)
The **Procurement Manager** (a `User` with a specific `Role`) is responsible for reviewing approved requisitions, selecting vendors, and officially creating a Purchase Order to buy the items. In some workflows, they might also be the ones approving the initial requisition.

*   **Example:** Sarah is the Procurement Manager. She sees John's approved Requisition for a new laptop. She selects the vendor "TechSupplies Inc.", negotiates the price, and generates a **Purchase Order (PO)** in the system.

### D. Purchase Order (PO) (The Official Order)
A **Purchase Order** is the legally binding document sent to the Vendor to officially order the goods. It links the required items to a specific supplier.

*   **Example:** Sarah's PO #PO-1002 is sent to "TechSupplies Inc." requesting 1 MacBook Pro M3 for $2000.

### E. Approval (The Check & Balance)
The **Approval** entity tracks who approved or rejected a document (Requisition or PO), when they did it, and any comments they left.

*   **Example:** Before Sarah could create the PO, John's manager, Mike, reviewed the Requisition. He clicked "Approve" and added a comment: "Approved, John's old laptop is out of warranty". This action is saved as an **Approval** record linked to John's Requisition and Mike's User ID.

---

## Summary Workflow Example
1. **Employee (User)** creates a **Requisition** for a laptop.
2. **Manager (User)** creates an **Approval** record by approving the Requisition.
3. **Procurement Manager (User)** reviews the approved Requisition, selects a **Vendor**, and generates a **Purchase Order**.
4. The system tracks all these links permanently in the database.
