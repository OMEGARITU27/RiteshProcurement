package com.procurement.procurement.controller.auth;

import com.procurement.procurement.dto.auth.LoginRequestDTO;
import com.procurement.procurement.dto.auth.AuthResponseDTO;
import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.entity.user.AccountStatus;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.repository.vendor.VendorAccountRepository;
import com.procurement.procurement.security.JwtTokenProvider;
import com.procurement.procurement.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final VendorAccountRepository vendorAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          VendorAccountRepository vendorAccountRepository,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository,
                          EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.vendorAccountRepository = vendorAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
    }

    // ===================== LOGIN =====================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {

        // ✅ Step 1: Find user
        User userCheck = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

        if (userCheck == null) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        // ✅ Step 2: Check account status BEFORE verifying password
        if (userCheck.getAccountStatus() == AccountStatus.PENDING_APPROVAL) {
            return ResponseEntity.status(403).body("Your account is pending admin approval.");
        } else if (userCheck.getAccountStatus() == AccountStatus.REJECTED) {
            String reason = userCheck.getRejectionReason() != null ? userCheck.getRejectionReason()
                    : "No reason provided";
            return ResponseEntity.status(403)
                    .body("Your registration was rejected. Reason: " + reason);
        } else if (userCheck.getAccountStatus() == AccountStatus.SUSPENDED) {
            return ResponseEntity.status(403)
                    .body("Your account has been suspended. Contact admin.");
        }

        // ✅ Step 3: Verify password directly — avoids recursive Spring Security chain
        if (!passwordEncoder.matches(loginRequest.getPassword(), userCheck.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        // ✅ Step 4: Generate JWT token
        String token = jwtTokenProvider.generateToken(userCheck.getUsername());

        // ✅ Step 5: Build roles list (roles are EAGER-loaded with the user)
        List<String> roleNames = userCheck.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setId(userCheck.getId());
        response.setToken(token);
        response.setType("Bearer");
        response.setUsername(userCheck.getUsername());
        response.setRoles(roleNames);

        return ResponseEntity.ok(response);
    }

    // ===================== REGISTER =====================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequestDTO request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists.");
        }

        if (request.getEmail() != null) {
            if (userRepository.findByEmail(request.getEmail()).isPresent() ||
                    vendorAccountRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already registered in the system.");
            }
        }

        // ✅ Only ROLE_EMPLOYEE and ROLE_PROCUREMENT_MANAGER can self-register
        String requestedRole = request.getRole();
        if (requestedRole == null || requestedRole.isBlank()) {
            requestedRole = "ROLE_EMPLOYEE";
        }
        if (!requestedRole.equals("ROLE_EMPLOYEE") && !requestedRole.equals("ROLE_PROCUREMENT_MANAGER")) {
            return ResponseEntity.badRequest()
                    .body("Invalid role. Only ROLE_EMPLOYEE or ROLE_PROCUREMENT_MANAGER allowed.");
        }

        // ✅ Final copy required for use inside lambda
        final String resolvedRole = requestedRole;
        Role role = roleRepository.findByName(resolvedRole)
                .orElseThrow(() -> new RuntimeException(resolvedRole + " not found in DB."));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail() != null ? request.getEmail()
                : request.getUsername() + "@procurement.com");
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setEnabled(true);

        // ✅ Employees are auto-approved — they get immediate access
        // Procurement Managers need admin approval (higher privilege role)
        if (resolvedRole.equals("ROLE_EMPLOYEE")) {
            user.setAccountStatus(AccountStatus.APPROVED);
        } else {
            user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        }

        user.addRole(role);
        userRepository.save(user);

        // Notify admin only when a Manager registers (not for every employee)
        if (resolvedRole.equals("ROLE_PROCUREMENT_MANAGER")) {
            emailService.sendAdminNewUserNotification(
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    resolvedRole);
            return ResponseEntity.ok(Map.of("message", "Registration submitted. Awaiting admin approval."));
        }

        // Employee: registered and ready to use
        return ResponseEntity.ok(Map.of("message", "Account created successfully! You can now log in."));
    }
}