package com.procurement.procurement.repository.vendor;

import com.procurement.procurement.entity.vendor.VendorAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorAccountRepository extends JpaRepository<VendorAccount, Long> {
    Optional<VendorAccount> findByEmail(String email);

    List<VendorAccount> findByStatus(String status);
}

