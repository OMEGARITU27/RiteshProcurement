package com.procurement.procurement.security;

import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional  // ✅ THIS IS THE KEY FIX — keeps session open while roles load
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));

        // ✅ Force load roles while transaction is still open
        Set<GrantedAuthority> authorities = mapRolesAndPermissions(user);

        System.out.println("DEBUG - User: " + user.getUsername() +
                " | Roles loaded: " + authorities.toString()); // temp debug

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isAccountNonLocked(),
                authorities
        );
    }

    private Set<GrantedAuthority> mapRolesAndPermissions(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            System.out.println("DEBUG - WARNING: No roles found for user: " + user.getUsername());
            return authorities;
        }

        user.getRoles().forEach(role -> {
            System.out.println("DEBUG - Adding role: " + role.getName()); // temp debug
            authorities.add(new SimpleGrantedAuthority(role.getName()));

            if (role.getPermissions() != null) {
                role.getPermissions().forEach(permission -> {
                    System.out.println("DEBUG - Adding permission: " + permission.getName());
                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                });
            }
        });

        return authorities;
    }
}