package com.procurement.procurement.service;

import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${procurement.admin.email:admin@company.com}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    // ===================== Core HTML send =====================

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(adminEmail, "ProcureFlow System");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Returns admin email(s) to notify.
     * Uses static config to avoid Hibernate LazyLoading / StackOverflow.
     */
    private List<String> getAdminEmails() {
        return List.of(adminEmail);
    }

    private String primaryAdminEmail() {
        return adminEmail;
    }

    private String baseStyle() {
        return "font-family: Arial, sans-serif; background: #f4f6f8; padding: 40px 0;";
    }

    private String wrapHtml(String title, String body) {
        return "<div style=\"" + baseStyle() + "\">"
                + "<div style=\"max-width:600px;margin:0 auto;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);\">"
                + "<div style=\"background:linear-gradient(135deg,#6c63ff,#5246d5);padding:32px;text-align:center;\">"
                + "<h1 style=\"color:#fff;margin:0;font-size:22px;\">" + title + "</h1>"
                + "</div>"
                + "<div style=\"padding:32px;color:#333;line-height:1.7;\">"
                + body
                + "</div>"
                + "<div style=\"background:#f4f6f8;padding:16px;text-align:center;font-size:12px;color:#999;\">"
                + "Smart Procurement System &mdash; This is an automated message."
                + "</div>"
                + "</div>"
                + "</div>";
    }

    // ===================== User Notification Emails =====================

    /** Notify admin(s) that a new user has registered and is pending approval */
    public void sendAdminNewUserNotification(String username, String fullName, String email, String role) {
        String roleDisplay = role.replace("ROLE_", "").replace("_", " ");
        String body = "<p>A new user has registered and is <strong>pending your approval</strong>.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;\">"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">Username</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + username + "</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">Full Name</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + fullName + "</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">Email</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + email + "</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">Role Requested</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + roleDisplay + "</td></tr>"
                + "</table>"
                + "<p style=\"margin-top:20px;\">Please log in to the Admin Panel to approve or reject this registration.</p>";
        String html = wrapHtml("New " + roleDisplay + " Registration Pending", body);
        for (String admin : getAdminEmails()) {
            sendHtmlEmail(admin, "New " + roleDisplay + " Registration Pending: " + username, html);
        }
    }

    /** Notify user that their account has been approved */
    public void sendUserApproved(String toEmail, String username) {
        String body = "<p>Hello <strong>" + username + "</strong>,</p>"
                + "<p>Great news! Your account has been <span style=\"color:#27ae60;font-weight:bold;\">approved</span>.</p>"
                + "<p>You can now log in to the Smart Procurement System and start working.</p>"
                + "<a href=\"http://localhost:5173/login\" style=\"display:inline-block;margin-top:20px;padding:12px 28px;background:#6c63ff;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;\">Login Now</a>";
        sendHtmlEmail(toEmail, "Your Account Has Been Approved", wrapHtml("Account Approved! ✅", body));
    }

    /** Notify user that their account has been rejected */
    public void sendUserRejected(String toEmail, String username, String reason) {
        String body = "<p>Hello <strong>" + username + "</strong>,</p>"
                + "<p>Unfortunately, your registration request has been <span style=\"color:#e74c3c;font-weight:bold;\">rejected</span>.</p>"
                + "<p><strong>Reason:</strong> " + reason + "</p>"
                + "<p>If you believe this is an error, please contact your system administrator.</p>";
        sendHtmlEmail(toEmail, "Your Registration Was Rejected", wrapHtml("Registration Rejected", body));
    }

    /** Notify user that their account has been suspended */
    public void sendUserSuspended(String toEmail, String username) {
        String body = "<p>Hello <strong>" + username + "</strong>,</p>"
                + "<p>Your account has been <span style=\"color:#e67e22;font-weight:bold;\">suspended</span>.</p>"
                + "<p>Please contact your system administrator for further information.</p>";
        sendHtmlEmail(toEmail, "Your Account Has Been Suspended", wrapHtml("Account Suspended", body));
    }

    /** Notify user that their role has been changed */
    public void sendRoleChanged(String toEmail, String username, String newRole) {
        String roleDisplay = newRole.replace("ROLE_", "").replace("_", " ");
        String body = "<p>Hello <strong>" + username + "</strong>,</p>"
                + "<p>Your role in the Smart Procurement System has been updated to: <strong>" + roleDisplay
                + "</strong>.</p>"
                + "<p>Your new permissions will take effect on your next login.</p>";
        sendHtmlEmail(toEmail, "Your Role Has Been Updated", wrapHtml("Role Updated: " + roleDisplay, body));
    }

    // Keep legacy name for backward compatibility
    public void sendRoleUpgraded(String toEmail, String newRole) {
        sendRoleChanged(toEmail, "", newRole);
    }

    /** Notify requisition creator about status change */
    public void sendRequisitionStatusUpdate(String toEmail, String reqNumber, String status, String reason) {
        String color = status.equals("APPROVED") ? "#27ae60" : "#e74c3c";
        String body = "<p>Your requisition <strong>" + reqNumber + "</strong> has been updated.</p>"
                + "<p>New Status: <span style=\"color:" + color + ";font-weight:bold;\">" + status + "</span></p>"
                + (reason != null && !reason.isBlank() ? "<p><strong>Reason:</strong> " + reason + "</p>" : "")
                + "<p>Log in to the system to view full details.</p>";
        sendHtmlEmail(toEmail, "Requisition " + reqNumber + " — " + status,
                wrapHtml("Requisition Status Updated", body));
    }

    // Legacy methods for backward compatibility
    public void sendRequisitionApproved(String toEmail, String requisitionNumber) {
        sendRequisitionStatusUpdate(toEmail, requisitionNumber, "APPROVED", null);
    }

    public void sendRequisitionRejected(String toEmail, String requisitionNumber, String reason) {
        sendRequisitionStatusUpdate(toEmail, requisitionNumber, "REJECTED", reason);
    }

    // ===================== Vendor Notification Emails =====================

    /** Notify vendor about a new PO assigned to them */
    public void sendVendorNewPO(String toEmail, String poNumber, String vendorName) {
        String body = "<p>Hello <strong>" + vendorName + "</strong>,</p>"
                + "<p>A new Purchase Order has been assigned to you.</p>"
                + "<p><strong>PO Number:</strong> " + poNumber + "</p>"
                + "<p>Please log in to the Vendor Portal to view the details and update the order status.</p>";
        sendHtmlEmail(toEmail, "New Purchase Order: " + poNumber, wrapHtml("New Purchase Order Assigned", body));
    }

    // Legacy overload — keeps existing PurchaseOrderController call working
    public void sendVendorNewPO(String toEmail, String poNumber) {
        sendVendorNewPO(toEmail, poNumber, "Vendor");
    }

    /** Notify admin(s) that a new vendor has registered */
    public void sendAdminNewVendorNotification(String companyName, String email, String gst) {
        String body = "<p>A new vendor has registered and is <strong>pending your approval</strong>.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;\">"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">Company Name</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + companyName + "</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">Email</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + email + "</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #eee;font-weight:bold;\">GST Number</td><td style=\"padding:8px;border:1px solid #eee;\">"
                + gst + "</td></tr>"
                + "</table>"
                + "<p style=\"margin-top:20px;\">Please log in to the Admin Panel to approve or reject this vendor registration.</p>";
        String html = wrapHtml("New Vendor Registration Pending", body);
        for (String admin : getAdminEmails()) {
            sendHtmlEmail(admin, "New Vendor Registration: " + companyName, html);
        }
    }

    /** Notify vendor that their account has been approved */
    public void sendVendorApproved(String toEmail, String companyName) {
        String body = "<p>Hello <strong>" + companyName + "</strong>,</p>"
                + "<p>Your vendor account has been <span style=\"color:#27ae60;font-weight:bold;\">approved</span>!</p>"
                + "<p>You can now login to the Vendor Portal using your registered email address.</p>"
                + "<a href=\"http://localhost:5173/vendor-login\" style=\"display:inline-block;margin-top:20px;padding:12px 28px;background:#6c63ff;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;\">Login to Vendor Portal</a>";
        sendHtmlEmail(toEmail, "Your Vendor Account Is Approved", wrapHtml("Vendor Account Approved! ✅", body));
    }

    // Legacy method name
    public void sendVendorAccountApproved(String vendorEmail) {
        sendVendorApproved(vendorEmail, "Vendor");
    }

    /** Notify vendor that their account has been rejected */
    public void sendVendorRejected(String toEmail, String companyName, String reason) {
        String body = "<p>Hello <strong>" + companyName + "</strong>,</p>"
                + "<p>Unfortunately, your vendor registration has been <span style=\"color:#e74c3c;font-weight:bold;\">rejected</span>.</p>"
                + "<p><strong>Reason:</strong> " + reason + "</p>"
                + "<p>If you believe this is an error, please contact the procurement team.</p>";
        sendHtmlEmail(toEmail, "Vendor Registration Rejected", wrapHtml("Vendor Registration Rejected", body));
    }

    /** Notify admin and manager when a PO is marked as DELIVERED */
    public void sendPODelivered(String adminEmail, String managerEmail, String poNumber, String vendorName) {
        String body = "<p>Purchase Order <strong>" + poNumber
                + "</strong> has been marked as <span style=\"color:#27ae60;font-weight:bold;\">DELIVERED</span>.</p>"
                + "<p><strong>Vendor:</strong> " + vendorName + "</p>"
                + "<p>Please log in to verify the delivery and process any required actions.</p>";
        String html = wrapHtml("PO Delivered: " + poNumber, body);
        if (adminEmail != null && !adminEmail.isBlank())
            sendHtmlEmail(adminEmail, "PO Delivered: " + poNumber, html);
        if (managerEmail != null && !managerEmail.isBlank())
            sendHtmlEmail(managerEmail, "PO Delivered: " + poNumber, html);
    }

    /** Legacy: notify all admins+managers when PO is delivered */
    public void sendPODeliveredToAdmins(String poNumber) {
        List<String> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().map(Role::getName)
                        .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_PROCUREMENT_MANAGER")))
                .map(User::getEmail)
                .toList();
        String body = "<p>Purchase Order <strong>" + poNumber
                + "</strong> has been marked as <span style=\"color:#27ae60;font-weight:bold;\">DELIVERED</span> by the vendor.</p>"
                + "<p>Please log in to verify and process accordingly.</p>";
        String html = wrapHtml("PO Delivered: " + poNumber, body);
        for (String email : recipients) {
            sendHtmlEmail(email, "PO Delivered: " + poNumber, html);
        }
    }
}

