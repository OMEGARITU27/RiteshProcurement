package com.procurement.procurement.controller.admin;

import com.procurement.procurement.entity.user.AccountStatus;
import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    public AdminController(UserRepository userRepository, RoleRepository roleRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
    }

    // ===================== Get Pending Users =====================
    @GetMapping("/pending-users")
    public ResponseEntity<List<User>> getPendingUsers() {
        List<User> pendingUsers = userRepository.findAll().stream()
                .filter(user -> user.getAccountStatus() == AccountStatus.PENDING_APPROVAL)
                .toList();
        return ResponseEntity.ok(pendingUsers);
    }

    // ===================== Get All Users =====================
    @GetMapping("/all-users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // ===================== Approve User =====================
    @PutMapping("/users/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        user.setAccountStatus(AccountStatus.APPROVED);
        userRepository.save(user);

        emailService.sendUserApproved(user.getEmail(), user.getUsername());
        return ResponseEntity.ok("User approved successfully");
    }

    // ===================== Reject User =====================
    @PutMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        String reason = request.getOrDefault("reason", "No reason provided");

        User user = userOpt.get();
        user.setAccountStatus(AccountStatus.REJECTED);
        user.setRejectionReason(reason); // ✅ saved for login error message
        userRepository.save(user);

        emailService.sendUserRejected(user.getEmail(), user.getUsername(), reason);
        return ResponseEntity.ok("User rejected successfully");
    }

    // ===================== Suspend User =====================
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);

        emailService.sendUserSuspended(user.getEmail(), user.getUsername());
        return ResponseEntity.ok("User suspended successfully");
    }

    // ===================== Change User Role =====================
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        String roleName = request.getOrDefault("roleName", request.get("role"));
        if (roleName == null || (!roleName.equals("ROLE_EMPLOYEE") && !roleName.equals("ROLE_PROCUREMENT_MANAGER"))) {
            return ResponseEntity.badRequest()
                    .body("Invalid role name. Must be ROLE_EMPLOYEE or ROLE_PROCUREMENT_MANAGER");
        }

        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Role " + roleName + " not found in DB");
        }

        User user = userOpt.get();
        user.getRoles().clear();
        user.addRole(roleOpt.get());
        userRepository.save(user);

        emailService.sendRoleChanged(user.getEmail(), user.getUsername(), roleName);
        return ResponseEntity.ok("User role updated successfully to " + roleName);
    }
}

