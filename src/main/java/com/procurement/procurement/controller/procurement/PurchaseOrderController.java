package com.procurement.procurement.controller.procurement;

import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderDocument;
import com.procurement.procurement.repository.procurement.PurchaseOrderDocumentRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/com/procurement/purchase-order")
public class PurchaseOrderController {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDocumentRepository documentRepository;
    private final EmailService emailService;

    public PurchaseOrderController(PurchaseOrderRepository purchaseOrderRepository,
                                   PurchaseOrderDocumentRepository documentRepository,
                                   EmailService emailService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.documentRepository = documentRepository;
        this.emailService = emailService;
    }

    // ===================== Create Purchase Order =====================
    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(
            @RequestBody PurchaseOrder purchaseOrder) {

        purchaseOrder.setCreatedAt(LocalDateTime.now());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());

        // ✅ Properly link each item back to the parent PO
        if (purchaseOrder.getItems() != null) {
            purchaseOrder.getItems().forEach(item -> item.setPurchaseOrder(purchaseOrder));
        }

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        if (saved.getVendor() != null && saved.getVendor().getEmail() != null) {
            emailService.sendVendorNewPO(saved.getVendor().getEmail(), saved.getPoNumber());
        }

        return ResponseEntity.ok(saved);
    }

    // ===================== Get all Purchase Orders =====================
    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderRepository.findAll());
    }

    // ===================== Get Purchase Order by ID =====================
    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPurchaseOrderById(@PathVariable Long id) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Purchase Order not found");
        }
        return ResponseEntity.ok(poOpt.get());
    }

    // ===================== Update Purchase Order Status =====================
    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @PatchMapping("/update-status/{id}")
    public ResponseEntity<String> updateStatus(@PathVariable Long id,
                                               @RequestParam String status) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Purchase Order not found");
        }

        PurchaseOrder po = poOpt.get();
        po.setStatus(status);
        po.setUpdatedAt(LocalDateTime.now());
        purchaseOrderRepository.save(po);

        return ResponseEntity.ok("Purchase Order status updated to " + status);
    }

    // ===================== Get PO Documents (for Managers/Admins)
    // =====================
    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<PurchaseOrderDocument>> getPODocuments(@PathVariable Long id) {
        return ResponseEntity.ok(documentRepository.findByPurchaseOrderId(id));
    }
}
