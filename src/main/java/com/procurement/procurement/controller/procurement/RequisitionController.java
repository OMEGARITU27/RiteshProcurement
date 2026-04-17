package com.procurement.procurement.controller.procurement;

import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.service.procurement.RequisitionService;
import com.procurement.procurement.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/com/procurement/requisition")
public class RequisitionController {

    private final RequisitionService requisitionService;
    private final EmailService emailService;

    public RequisitionController(RequisitionService requisitionService, EmailService emailService) {
        this.requisitionService = requisitionService;
        this.emailService = emailService;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<Requisition> createRequisition(@RequestBody Requisition requisition) {
        return ResponseEntity.ok(requisitionService.createRequisition(requisition));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<List<Requisition>> getMyRequisitions() {
        return ResponseEntity.ok(requisitionService.getMyRequisitions());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteRequisition(@PathVariable Long id) {
        try {
            requisitionService.deleteRequisition(id);
            return ResponseEntity.ok("Requisition deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Requisition>> getAllRequisitions() {
        return ResponseEntity.ok(requisitionService.getAllRequisitions());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequisitionById(@PathVariable Long id) {
        try {
            Requisition req = requisitionService.getRequisitionById(id);
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @PatchMapping("/update-status/{id}")
    public ResponseEntity<String> updateRequisitionStatus(@PathVariable Long id,
                                                          @RequestParam String status) {
        try {
            Requisition updatedReq = requisitionService.updateRequisition(id, buildStatusUpdate(status));

            if (updatedReq.getRequestedBy() != null && updatedReq.getRequestedBy().getEmail() != null) {
                if (status.equals("APPROVED")) {
                    emailService.sendRequisitionApproved(updatedReq.getRequestedBy().getEmail(),
                            updatedReq.getRequisitionNumber());
                } else if (status.equals("REJECTED")) {
                    emailService.sendRequisitionRejected(updatedReq.getRequestedBy().getEmail(),
                            updatedReq.getRequisitionNumber(), "System update");
                }
            }

            return ResponseEntity.ok("Requisition status updated to " + status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== Employee marks requisition as RECEIVED
    // =====================
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @PatchMapping("/mark-received/{id}")
    public ResponseEntity<String> markAsReceived(@PathVariable Long id) {
        try {
            boolean isPrivileged = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PROCUREMENT_MANAGER")
                            || a.getAuthority().equals("ROLE_ADMIN"));

            List<Requisition> myReqs = requisitionService.getMyRequisitions();
            boolean isOwner = myReqs.stream().anyMatch(r -> r.getId().equals(id));

            if (!isOwner && !isPrivileged) {
                return ResponseEntity.status(403).body("You can only mark your own requisitions as received.");
            }

            Requisition updated = requisitionService.markRequisitionReceived(id);

            if (updated.getRequestedBy() != null && updated.getRequestedBy().getEmail() != null) {
                emailService.sendRequisitionApproved(updated.getRequestedBy().getEmail(),
                        "Goods Received: " + updated.getRequisitionNumber());
            }
            return ResponseEntity.ok("Requisition marked as RECEIVED. Goods receipt confirmed.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== DEBUG - remove after fixing =====================
    @GetMapping("/debug-roles")
    public ResponseEntity<String> debugRoles() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return ResponseEntity.ok("User: " + auth.getName()
                + " | Authorities: " + auth.getAuthorities().toString());
    }

    private Requisition buildStatusUpdate(String status) {
        Requisition r = new Requisition();
        r.setStatus(status);
        return r;
    }
}