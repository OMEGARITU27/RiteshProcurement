package com.procurement.procurement.controller.report;

import com.procurement.procurement.dto.report.ReportRequestDTO;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.service.report.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final RequisitionRepository requisitionRepository;

    public ReportController(ReportService reportService, RequisitionRepository requisitionRepository) {
        this.reportService = reportService;
        this.requisitionRepository = requisitionRepository;
    }

    // ===================== Generate Vendor Report (PDF/Excel)
    // =====================
    @PreAuthorize("hasAnyAuthority('ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @PostMapping("/vendor")
    public ResponseEntity<byte[]> generateVendorReport(@RequestBody ReportRequestDTO request,
                                                       @RequestParam(defaultValue = "pdf") String format) {

        byte[] data = reportService.generateVendorReport(request, format);

        String mimeType = format.equalsIgnoreCase("excel")
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";

        String fileName = "vendor_report." + (format.equalsIgnoreCase("excel") ? "xlsx" : "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(mimeType))
                .body(data);
    }

    // ===================== Generate GRN (Goods Receipt Note) for a Requisition
    // =====================
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_PROCUREMENT_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/grn/{requisitionId}")
    public ResponseEntity<?> generateGRN(@PathVariable Long requisitionId) {
        Requisition req = requisitionRepository.findById(requisitionId).orElse(null);
        if (req == null) {
            return ResponseEntity.badRequest().body("Requisition not found");
        }
        byte[] pdf = reportService.generateGRN(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=GRN-" + req.getRequisitionNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

