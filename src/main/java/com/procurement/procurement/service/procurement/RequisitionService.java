package com.procurement.procurement.service.procurement;

import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RequisitionService(RequisitionRepository requisitionRepository,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.requisitionRepository = requisitionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ===================== CREATE =====================
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_EMPLOYEE', 'CREATE_REQUISITION')")
    public Requisition createRequisition(Requisition requisition) {

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        requisition.setRequestedBy(currentUser);
        requisition.setStatus("PENDING");
        requisition.setCreatedAt(LocalDateTime.now());
        requisition.setUpdatedAt(LocalDateTime.now());

        if (requisition.getItems() != null) {
            requisition.getItems().forEach(item -> item.setRequisition(requisition));
        }

        Requisition saved = requisitionRepository.save(requisition);
        auditService.log("Requisition", saved.getId(), "CREATE", "Requisition created successfully");
        return saved;
    }

    // ===================== UPDATE =====================
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER', 'UPDATE_REQUISITION')")
    public Requisition updateRequisition(Long id, Requisition updatedReq) {

        Requisition existingReq = requisitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found with id: " + id));

        // ✅ Only update fields that are actually provided
        if (updatedReq.getRequisitionNumber() != null) {
            existingReq.setRequisitionNumber(updatedReq.getRequisitionNumber());
        }
        if (updatedReq.getStatus() != null) {
            existingReq.setStatus(updatedReq.getStatus());
        }
        existingReq.setUpdatedAt(LocalDateTime.now());

        // FIX: The `items` property in updatedReq may be null or empty list depending
        // on client request.
        // We only replace the collection if it's explicitly non-null AND non-empty,
        // to prevent accidentally wiping out existing items during a simple status
        // update.
        if (updatedReq.getItems() != null && !updatedReq.getItems().isEmpty()) {
            existingReq.setItems(updatedReq.getItems());
        }

        Requisition saved = requisitionRepository.save(existingReq);
        auditService.log("Requisition", saved.getId(), "UPDATE", "Requisition updated successfully");
        return saved;
    }

    // ===================== MARK AS RECEIVED =====================
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_EMPLOYEE')")
    public Requisition markRequisitionReceived(Long id) {
        Requisition existingReq = requisitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found with id: " + id));
        existingReq.setStatus("RECEIVED");
        existingReq.setUpdatedAt(LocalDateTime.now());
        Requisition saved = requisitionRepository.save(existingReq);
        auditService.log("Requisition", saved.getId(), "UPDATE", "Requisition marked as RECEIVED");
        return saved;
    }

    // ===================== GET ALL =====================
    // ✅ FIXED: hasAnyRole → hasAnyAuthority with full ROLE_ prefix
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER')")
    public List<Requisition> getAllRequisitions() {
        return requisitionRepository.findAll();
    }

    // ===================== GET MY REQUISITIONS =====================
    // ✅ FIXED: hasRole → hasAuthority with full ROLE_ prefix
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER')")
    public List<Requisition> getMyRequisitions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return requisitionRepository.findByRequestedBy(currentUser);
    }

    // ===================== DELETE =====================
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER')")
    public void deleteRequisition(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isPrivileged = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROCUREMENT_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        Requisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found"));

        // Employees can only delete their own PENDING requisitions
        if (!isPrivileged) {
            if (!req.getRequestedBy().getUsername().equals(username)) {
                throw new RuntimeException("You can only delete your own requisitions.");
            }
            if (!"PENDING".equals(req.getStatus())) {
                throw new RuntimeException("Only PENDING requisitions can be deleted.");
            }
        }

        requisitionRepository.deleteById(id);
        auditService.log("Requisition", id, "DELETE", "Requisition deleted by " + username);
    }

    // ===================== GET BY STATUS =====================
    @PreAuthorize("hasAuthority('VIEW_REQUISITION')")
    public List<Requisition> getRequisitionsByStatus(String status) {
        return requisitionRepository.findByStatus(status);
    }

    // ===================== GET BY ID (all roles, employees see own only)
    // =====================
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_EMPLOYEE')")
    public Requisition getRequisitionById(Long id) {
        Requisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requisition not found with id: " + id));

        boolean isPrivileged = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROCUREMENT_MANAGER")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isPrivileged) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!req.getRequestedBy().getUsername().equals(username)) {
                throw new RuntimeException("Access denied: this requisition does not belong to you.");
            }
        }
        return req;
    }
}
