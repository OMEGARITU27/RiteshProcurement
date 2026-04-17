package com.procurement.procurement.service.initializer;

import com.procurement.procurement.entity.user.AccountStatus;
import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ===================== CREATE ROLES =====================

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_ADMIN", "System Administrator")));

        roleRepository.findByName("ROLE_PROCUREMENT_MANAGER")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_PROCUREMENT_MANAGER", "Procurement Manager")));

        roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_EMPLOYEE", "Normal Employee")));

        // ✅ ROLE_VENDOR — for vendor portal auth flow
        roleRepository.findByName("ROLE_VENDOR")
                .orElseGet(() -> roleRepository.save(
                        new Role("ROLE_VENDOR", "Vendor")));

        System.out.println("✅ Roles Initialized");

        // ===================== CREATE OR FIX DEFAULT ADMIN =====================

        userRepository.findByUsername("admin").ifPresentOrElse(
                existingAdmin -> {
                    // ✅ Admin already exists — make sure they have APPROVED status
                    // (handles case where admin was created by old seeder without accountStatus)
                    boolean needsUpdate = false;

                    if (existingAdmin.getAccountStatus() == null ||
                            existingAdmin.getAccountStatus() == AccountStatus.PENDING_APPROVAL) {
                        existingAdmin.setAccountStatus(AccountStatus.APPROVED);
                        needsUpdate = true;
                        System.out.println(
                                "✅ Patched existing admin: accountStatus set to APPROVED");
                    }
                    if (existingAdmin.getFullName() == null
                            || existingAdmin.getFullName().isBlank()) {
                        existingAdmin.setFullName("System Administrator");
                        needsUpdate = true;
                    }
                    // ✅ Always sync to the real admin email (fixes fake admin@company.com)
                    if (!"adminritesh@gmail.com".equals(existingAdmin.getEmail())) {
                        existingAdmin.setEmail("adminritesh@gmail.com");
                        needsUpdate = true;
                        System.out.println(
                                "✅ Patched existing admin: email updated to real address");
                    }
                    if (needsUpdate) {
                        userRepository.save(existingAdmin);
                    }
                },
                () -> {
                    // ✅ Admin doesn't exist — create fresh with all correct fields
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setEmail("adminritesh@gmail.com");
                    admin.setFullName("System Administrator");
                    admin.setAccountStatus(AccountStatus.APPROVED);
                    admin.setEnabled(true);

                    Set<Role> roles = new HashSet<>();
                    roles.add(adminRole);
                    admin.setRoles(roles);

                    userRepository.save(admin);
                    System.out.println(
                            "✅ Default Admin Created — username: admin / password: admin123");
                });
    }
}