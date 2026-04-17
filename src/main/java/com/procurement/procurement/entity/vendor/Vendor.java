package com.procurement.procurement.entity.vendor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String contactNumber;
    private String address;
    private String gstNumber;
    private String status;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "vendor" }) // ← STOPS VendorDocument → Vendor → documents loop
    private List<VendorDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "vendor" }) // ← STOPS VendorRating → Vendor → ratings loop
    private List<VendorRating> ratings = new ArrayList<>();

    public Vendor() {
    }

    public Vendor(String name, String email, String contactNumber, String address,
                  String status, List<VendorDocument> documents, List<VendorRating> ratings) {
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.address = address;
        this.status = status;
        this.documents = documents;
        this.ratings = ratings;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public List<VendorDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<VendorDocument> documents) {
        this.documents = documents;
    }

    public List<VendorRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<VendorRating> ratings) {
        this.ratings = ratings;
    }

    public Object getPhone() {
        return contactNumber;
    }

    public void setPhone(Object phone) {
        this.contactNumber = (String) phone;
    }
}
