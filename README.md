# RiteshProcurement

## Frontend (Now Included)

A fully usable web frontend is available at:

- `src/main/resources/static/index.html`

When the Spring Boot app runs, open:

- `http://localhost:8080/`

### What This Frontend Supports

- User login and registration
- Vendor login and registration
- Vendor CRUD management
- Requisition creation and listing
- Purchase order creation and listing
- Approval actions (approve/reject purchase orders)
- Vendor portal actions (accept/reject/update PO status)
- Admin user and vendor account approvals
- Vendor report download (PDF/Excel)

### Notes

- The UI uses your existing backend endpoints directly.
- Auth token and session are stored in browser local storage.
- Tabs are role-aware (admin/vendor/employee visibility).