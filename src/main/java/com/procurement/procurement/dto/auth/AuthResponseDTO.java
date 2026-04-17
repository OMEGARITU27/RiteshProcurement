package com.procurement.procurement.dto.auth;

import java.util.List;

public class AuthResponseDTO {

    private Long id;
    private String token;
    private String type;
    private String username;
    private List<String> roles;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, String type) {
        this.token = token;
        this.type = type;
    }

    // ===================== Getters & Setters =====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}