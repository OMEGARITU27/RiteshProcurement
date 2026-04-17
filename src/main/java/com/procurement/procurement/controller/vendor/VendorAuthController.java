package com.procurement.procurement.controller.vendor;


import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.entity.vendor.VendorAccount;
import com.procurement.procurement.repository.vendor.VendorAccountRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.security.JwtTokenProvider;
import com.procurement.procurement.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor-auth")
public class VendorAuthController {

    private final VendorAccountRepository vendorAccountRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public VendorAuthController(VendorAccountRepository vendorAccountRepository,
                                VendorRepository vendorRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                JwtTokenProvider jwtTokenProvider,
                                EmailService emailService) {
        this.vendorAccountRepository = vendorAccountRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    // ===================== REGISTER =====================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        if (vendorAccountRepository.findByEmail(email).isPresent() || userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered in the system.");
        }

        String companyName = request.getOrDefault("companyName", "Unknown");
        String gstNumber = request.getOrDefault("gstNumber", "");

        // Create the abstract Vendor profile
        Vendor vendor = new Vendor();
        vendor.setName(companyName);
        vendor.setEmail(email);
        vendor.setContactNumber(request.getOrDefault("contactNumber", ""));
        vendor.setAddress(request.getOrDefault("address", ""));
        vendor.setGstNumber(gstNumber);
        vendor.setStatus("PENDING");
        Vendor savedVendor = vendorRepository.save(vendor);

        // Create the authenticating Account
        VendorAccount account = new VendorAccount();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(request.get("password")));
        account.setVendor(savedVendor);
        account.setStatus("PENDING");
        vendorAccountRepository.save(account);

        // ✅ Notify admin about new vendor registration
        emailService.sendAdminNewVendorNotification(companyName, email, gstNumber);

        return ResponseEntity.ok(Map.of("message", "Vendor registration submitted. Awaiting admin approval."));
    }

    // ===================== LOGIN =====================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        VendorAccount account = vendorAccountRepository.findByEmail(email).orElse(null);

        if (account == null || !passwordEncoder.matches(password, account.getPassword())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        if (account.getStatus().equals("PENDING")) {
            return ResponseEntity.status(403).body("Your vendor account is pending admin approval.");
        } else if (account.getStatus().equals("REJECTED")) {
            return ResponseEntity.status(403).body("Your vendor account has been rejected.");
        } else if (account.getStatus().equals("SUSPENDED")) {
            return ResponseEntity.status(403).body("Your vendor account has been suspended. Contact admin.");
        }

        Long vendorId = account.getVendor() != null ? account.getVendor().getId() : null;
        String token = jwtTokenProvider.generateVendorToken(account.getEmail(), vendorId);

        // ✅ Roles always as List<String> — never as objects
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("email", account.getEmail());
        response.put("vendorId", vendorId);
        response.put("roles", List.of("ROLE_VENDOR"));

        return ResponseEntity.ok(response);
    }
}
