package com.procurement.procurement.security;

import com.procurement.procurement.entity.vendor.VendorAccount;
import com.procurement.procurement.repository.vendor.VendorAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class VendorUserDetailsService implements UserDetailsService {

    @Autowired
    private VendorAccountRepository vendorAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        VendorAccount vendorAccount = vendorAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Vendor account not found: " + email));

        boolean isEnabled = vendorAccount.getStatus().equals("APPROVED");

        return new org.springframework.security.core.userdetails.User(
                vendorAccount.getEmail(),
                vendorAccount.getPassword(),
                isEnabled,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_VENDOR")));
    }
}

