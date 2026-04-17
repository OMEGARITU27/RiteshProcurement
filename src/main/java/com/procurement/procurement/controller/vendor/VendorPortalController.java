package com.procurement.procurement.controller.vendor;

import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderDocument;
import com.procurement.procurement.entity.vendor.VendorAccount;
import com.procurement.procurement.repository.procurement.PurchaseOrderDocumentRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.vendor.VendorAccountRepository;
import com.procurement.procurement.service.EmailService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vendor-portal")
@PreAuthorize("hasRole('VENDOR')")
public class VendorPortalController {

    private final VendorAccountRepository vendorAccountRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDocumentRepository documentRepository;
    private final EmailService emailService;

    private final String UPLOAD_DIR = "uploads/vendor_documents/";

    public VendorPortalController(VendorAccountRepository vendorAccountRepository,
                                  PurchaseOrderRepository purchaseOrderRepository,
                                  PurchaseOrderDocumentRepository documentRepository,
                                  EmailService emailService) {
        this.vendorAccountRepository = vendorAccountRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.documentRepository = documentRepository;
        this.emailService = emailService;

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    private VendorAccount getAuthenticatedVendorAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return vendorAccountRepository.findByEmail(auth.getName()).orElse(null);
    }

    // ===================== Get POs for Logged-In Vendor =====================
    @GetMapping("/purchase-orders")
    public ResponseEntity<?> getPurchaseOrders() {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null) {
            return ResponseEntity.status(403).body("Vendor profile not linked.");
        }

        List<PurchaseOrder> pos = purchaseOrderRepository.findByVendor(account.getVendor());
        return ResponseEntity.ok(pos);
    }

    // ===================== Vendor Accept / Reject PO =====================
    @PutMapping("/purchase-orders/{id}/accept")
    public ResponseEntity<?> acceptPurchaseOrder(@PathVariable Long id) {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null) {
            return ResponseEntity.status(403).body("Vendor profile not linked.");
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty() || !poOpt.get().getVendor().getId().equals(account.getVendor().getId())) {
            return ResponseEntity.status(404).body("Purchase order not found or doesn't belong to you.");
        }

        PurchaseOrder po = poOpt.get();
        if (!"PENDING".equals(po.getStatus())) {
            return ResponseEntity.badRequest().body("Only PENDING purchase orders can be accepted.");
        }

        po.setStatus("APPROVED");
        purchaseOrderRepository.save(po);

        emailService.sendPODeliveredToAdmins("Vendor accepted PO: " + po.getPoNumber());

        return ResponseEntity.ok("Purchase order accepted. Status set to APPROVED.");
    }

    @PutMapping("/purchase-orders/{id}/reject")
    public ResponseEntity<?> rejectPurchaseOrder(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, String> body) {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null) {
            return ResponseEntity.status(403).body("Vendor profile not linked.");
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty() || !poOpt.get().getVendor().getId().equals(account.getVendor().getId())) {
            return ResponseEntity.status(404).body("Purchase order not found or doesn't belong to you.");
        }

        PurchaseOrder po = poOpt.get();
        if (!"PENDING".equals(po.getStatus())) {
            return ResponseEntity.badRequest().body("Only PENDING purchase orders can be rejected.");
        }

        po.setStatus("REJECTED");
        purchaseOrderRepository.save(po);

        return ResponseEntity.ok("Purchase order rejected.");
    }

    // ===================== Update PO Status (Ship / Deliver) =====================
    @PutMapping("/purchase-orders/{id}/status")
    public ResponseEntity<?> updatePurchaseOrderStatus(@PathVariable Long id,
                                                       @RequestBody Map<String, String> request) {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null) {
            return ResponseEntity.status(403).body("Vendor profile not linked.");
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty() || !poOpt.get().getVendor().getId().equals(account.getVendor().getId())) {
            return ResponseEntity.status(404).body("Purchase order not found or doesn't belong to you.");
        }

        String newStatus = request.get("status");
        if (!newStatus.equals("SHIPPED") && !newStatus.equals("DELIVERED")) {
            return ResponseEntity.badRequest().body(
                    "Use /accept or /reject endpoints. SHIPPED and DELIVERED are the only valid status updates here.");
        }

        if ("SHIPPED".equals(newStatus) && !"APPROVED".equals(poOpt.get().getStatus())) {
            return ResponseEntity.badRequest().body("PO must be APPROVED before marking as SHIPPED.");
        }

        PurchaseOrder po = poOpt.get();
        po.setStatus(newStatus);
        purchaseOrderRepository.save(po);

        if (newStatus.equals("DELIVERED")) {
            emailService.sendPODeliveredToAdmins(po.getPoNumber());
        }

        return ResponseEntity.ok("Purchase order status updated to " + newStatus);
    }

    // ===================== Upload Document =====================
    @PostMapping("/purchase-orders/{id}/documents")
    public ResponseEntity<?> uploadDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null) {
            return ResponseEntity.status(403).body("Vendor profile not linked.");
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty() || !poOpt.get().getVendor().getId().equals(account.getVendor().getId())) {
            return ResponseEntity.status(404).body("Purchase order not found.");
        }

        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.write(filePath, file.getBytes());

            PurchaseOrderDocument document = new PurchaseOrderDocument(
                    poOpt.get(),
                    file.getOriginalFilename(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    filePath.toString());

            documentRepository.save(document);
            return ResponseEntity.ok("File uploaded successfully");

        } catch (IOException e) {
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }
    }

    // ===================== View Uploaded Documents =====================
    @GetMapping("/purchase-orders/{id}/documents")
    public ResponseEntity<?> getDocuments(@PathVariable Long id) {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null) {
            return ResponseEntity.status(403).body("Vendor profile not linked.");
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(id);
        if (poOpt.isEmpty() || !poOpt.get().getVendor().getId().equals(account.getVendor().getId())) {
            return ResponseEntity.status(404).body("Purchase order not found.");
        }

        List<PurchaseOrderDocument> docs = documentRepository.findByPurchaseOrder(poOpt.get());
        return ResponseEntity.ok(docs);
    }

    // ===================== Download Document =====================
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDoc(@PathVariable Long id) {
        VendorAccount account = getAuthenticatedVendorAccount();
        if (account == null || account.getVendor() == null)
            return ResponseEntity.status(403).build();

        Optional<PurchaseOrderDocument> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()
                || !docOpt.get().getPurchaseOrder().getVendor().getId().equals(account.getVendor().getId())) {
            return ResponseEntity.status(404).build();
        }

        try {
            Path file = Paths.get(docOpt.get().getFilePath());
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(docOpt.get().getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + docOpt.get().getFileName() + "\"")
                        .body(resource);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.notFound().build();
    }
}
