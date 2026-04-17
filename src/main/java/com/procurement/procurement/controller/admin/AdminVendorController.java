package com.procurement.procurement.controller.admin;

import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.entity.vendor.VendorAccount;
import com.procurement.procurement.repository.vendor.VendorAccountRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/vendor-accounts")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminVendorController {

    private final VendorAccountRepository vendorAccountRepository;
    private final VendorRepository vendorRepository;
    private final EmailService emailService;

    public AdminVendorController(VendorAccountRepository vendorAccountRepository,
                                 VendorRepository vendorRepository,
                                 EmailService emailService) {
        this.vendorAccountRepository = vendorAccountRepository;
        this.vendorRepository = vendorRepository;
        this.emailService = emailService;
    }

    // ===================== Get Pending Vendor Accounts =====================
    @GetMapping("/pending")
    public ResponseEntity<List<VendorAccount>> getPendingVendorAccounts() {
        return ResponseEntity.ok(vendorAccountRepository.findByStatus("PENDING"));
    }

    // ===================== Get All Vendor Accounts =====================
    @GetMapping
    public ResponseEntity<List<VendorAccount>> getAllVendorAccounts() {
        return ResponseEntity.ok(vendorAccountRepository.findAll());
    }

    // ===================== Approve Vendor Account =====================
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveVendorAccount(@PathVariable Long id) {
        Optional<VendorAccount> vendorOpt = vendorAccountRepository.findById(id);
        if (vendorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Vendor account not found");
        }

        VendorAccount account = vendorOpt.get();
        account.setStatus("APPROVED");

        Vendor vendor;
        if (account.getVendor() != null) {
            // Vendor already linked — just approve it
            vendor = account.getVendor();
            vendor.setStatus("APPROVED");
        } else {
            // ✅ Auto-create a Vendor entity from the VendorAccount data
            vendor = new Vendor();
            vendor.setName(account.getEmail().split("@")[0]); // Use email prefix as vendor name
            vendor.setEmail(account.getEmail());
            vendor.setStatus("APPROVED");
        }
        vendorRepository.save(vendor);
        account.setVendor(vendor);
        vendorAccountRepository.save(account);

        String companyName = vendor.getName();
        emailService.sendVendorApproved(account.getEmail(), companyName);
        return ResponseEntity.ok("Vendor account approved successfully");
    }

    // ===================== Reject Vendor Account =====================
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectVendorAccount(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Optional<VendorAccount> vendorOpt = vendorAccountRepository.findById(id);
        if (vendorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Vendor account not found");
        }

        String reason = request.getOrDefault("reason", "No reason provided");

        VendorAccount account = vendorOpt.get();
        account.setStatus("REJECTED");
        vendorAccountRepository.save(account);

        String companyName = account.getVendor() != null ? account.getVendor().getName() : "Vendor";
        emailService.sendVendorRejected(account.getEmail(), companyName, reason);
        return ResponseEntity.ok("Vendor account rejected successfully");
    }
}