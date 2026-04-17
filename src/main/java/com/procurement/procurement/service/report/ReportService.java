package com.procurement.procurement.service.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.procurement.procurement.dto.report.ReportRequestDTO;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public ReportService(VendorRepository vendorRepository, PurchaseOrderRepository purchaseOrderRepository) {
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public byte[] generateVendorReport(ReportRequestDTO request, String format) {
        try {
            // ── Build filtered vendor list ──
            List<Vendor> vendors = vendorRepository.findAll();
            if (request.getVendorId() != null) {
                vendors = vendors.stream()
                        .filter(v -> v.getId().equals(request.getVendorId()))
                        .collect(Collectors.toList());
            }

            // ── Build filtered PO list ──
            List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
            if (request.getVendorId() != null) {
                pos = pos.stream()
                        .filter(p -> p.getVendor() != null && p.getVendor().getId().equals(request.getVendorId()))
                        .collect(Collectors.toList());
            }
            if (request.getPoNumber() != null && !request.getPoNumber().isBlank()) {
                String q = request.getPoNumber().toLowerCase();
                pos = pos.stream()
                        .filter(p -> p.getPoNumber() != null && p.getPoNumber().toLowerCase().contains(q))
                        .collect(Collectors.toList());
            }
            if (request.getStartDate() != null) {
                pos = pos.stream()
                        .filter(p -> p.getCreatedAt() != null
                                && !p.getCreatedAt().toLocalDate().isBefore(request.getStartDate()))
                        .collect(Collectors.toList());
            }
            if (request.getEndDate() != null) {
                pos = pos.stream()
                        .filter(p -> p.getCreatedAt() != null
                                && !p.getCreatedAt().toLocalDate().isAfter(request.getEndDate()))
                        .collect(Collectors.toList());
            }

            if ("excel".equalsIgnoreCase(format)) {
                return generateExcel(vendors, pos, request);
            }
            return generatePdf(vendors, pos, request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Report generation failed: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════ PDF ═══════════════════════
    private byte[] generatePdf(List<Vendor> vendors, List<PurchaseOrder> pos, ReportRequestDTO request)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate()); // landscape
        PdfWriter.getInstance(doc, out);
        doc.open();

        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
        com.itextpdf.text.Font subFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, BaseColor.GRAY);
        com.itextpdf.text.Font sectionFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);

        // ── Title ──
        Paragraph title = new Paragraph("Smart Procurement System — Vendor Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph generated = new Paragraph("Generated on: " + java.time.LocalDateTime.now().format(DATE_FMT)
                + "  |  Vendors: " + vendors.size() + "  |  POs: " + pos.size(), subFont);
        generated.setAlignment(Element.ALIGN_CENTER);
        generated.setSpacingAfter(10);
        doc.add(generated);

        // ── Applied Filters ──
        if (request.getVendorId() != null || request.getPoNumber() != null
                || request.getStartDate() != null || request.getEndDate() != null) {
            StringBuilder filters = new StringBuilder("Filters: ");
            if (request.getVendorId() != null)
                filters.append("Vendor ID=").append(request.getVendorId()).append("  ");
            if (request.getPoNumber() != null && !request.getPoNumber().isBlank())
                filters.append("PO Number contains '").append(request.getPoNumber()).append("'  ");
            if (request.getStartDate() != null)
                filters.append("From: ").append(request.getStartDate()).append("  ");
            if (request.getEndDate() != null)
                filters.append("To: ").append(request.getEndDate());
            Paragraph fp = new Paragraph(filters.toString(), subFont);
            fp.setSpacingAfter(12);
            doc.add(fp);
        }

        // ──────────────────── VENDORS TABLE ────────────────────
        Paragraph vsection = new Paragraph("Vendors", sectionFont);
        vsection.setSpacingAfter(6);
        doc.add(vsection);

        PdfPTable vtable = new PdfPTable(5);
        vtable.setWidthPercentage(100);
        vtable.setWidths(new float[] { 1f, 3f, 3f, 2f, 2f });
        vtable.setSpacingAfter(16);
        String[] vHeaders = { "ID", "Name", "Email", "Contact", "Status" };
        for (String h : vHeaders)
            addHeader(vtable, h);

        if (vendors.isEmpty()) {
            PdfPCell noData = new PdfPCell(new Phrase("No vendors found", subFont));
            noData.setColspan(5);
            noData.setHorizontalAlignment(Element.ALIGN_CENTER);
            noData.setPadding(8);
            vtable.addCell(noData);
        } else {
            for (Vendor v : vendors) {
                addCell(vtable, String.valueOf(v.getId()));
                addCell(vtable, nvl(v.getName()));
                addCell(vtable, nvl(v.getEmail()));
                addCell(vtable, nvl(v.getContactNumber()));
                addStatusCell(vtable, nvl(v.getStatus()));
            }
        }
        doc.add(vtable);

        // ──────────────────── PURCHASE ORDERS TABLE ────────────────────
        Paragraph posection = new Paragraph("Purchase Orders", sectionFont);
        posection.setSpacingAfter(6);
        doc.add(posection);

        PdfPTable ptable = new PdfPTable(6);
        ptable.setWidthPercentage(100);
        ptable.setWidths(new float[] { 2f, 3f, 2f, 2f, 2f, 2f });
        String[] pHeaders = { "PO Number", "Vendor", "Status", "Total (₹)", "Created", "Updated" };
        for (String h : pHeaders)
            addHeader(ptable, h);

        if (pos.isEmpty()) {
            PdfPCell noData = new PdfPCell(new Phrase("No purchase orders found", subFont));
            noData.setColspan(6);
            noData.setHorizontalAlignment(Element.ALIGN_CENTER);
            noData.setPadding(8);
            ptable.addCell(noData);
        } else {
            for (PurchaseOrder p : pos) {
                double total = p.getItems() == null ? 0
                        : p.getItems().stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
                addCell(ptable, nvl(p.getPoNumber()));
                addCell(ptable, p.getVendor() != null ? nvl(p.getVendor().getName()) : "—");
                addStatusCell(ptable, nvl(p.getStatus()));
                addCell(ptable, String.format("%.2f", total));
                addCell(ptable, p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "—");
                addCell(ptable, p.getUpdatedAt() != null ? p.getUpdatedAt().format(DATE_FMT) : "—");
            }
        }
        doc.add(ptable);

        // ── Footer ──
        com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, BaseColor.GRAY);
        Paragraph footer = new Paragraph("Generated by Smart Procurement System — Confidential", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    private void addHeader(PdfPTable table, String text) {
        com.itextpdf.text.Font f = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(new BaseColor(63, 81, 181));
        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value) {
        com.itextpdf.text.Font f = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
        PdfPCell cell = new PdfPCell(new Phrase(value, f));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addStatusCell(PdfPTable table, String status) {
        BaseColor bg = status.equals("ACTIVE") || status.equals("APPROVED") || status.equals("DELIVERED")
                ? new BaseColor(39, 174, 96)
                : status.equals("PENDING") ? new BaseColor(243, 156, 18)
                : new BaseColor(192, 57, 43);
        com.itextpdf.text.Font f = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(status, f));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }

    // ═══════════════════════ EXCEL ═══════════════════════
    private byte[] generateExcel(List<Vendor> vendors, List<PurchaseOrder> pos, ReportRequestDTO request)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Workbook wb = new XSSFWorkbook();

        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // ── Sheet 1: Vendors ──
        Sheet vendorSheet = wb.createSheet("Vendors");
        String[] vHeaders = { "ID", "Vendor Name", "Email", "Contact Number", "Status" };
        Row vhRow = vendorSheet.createRow(0);
        for (int i = 0; i < vHeaders.length; i++) {
            Cell c = vhRow.createCell(i);
            c.setCellValue(vHeaders[i]);
            c.setCellStyle(headerStyle);
        }
        int r = 1;
        for (Vendor v : vendors) {
            Row row = vendorSheet.createRow(r++);
            row.createCell(0).setCellValue(v.getId());
            row.createCell(1).setCellValue(nvl(v.getName()));
            row.createCell(2).setCellValue(nvl(v.getEmail()));
            row.createCell(3).setCellValue(nvl(v.getContactNumber()));
            row.createCell(4).setCellValue(nvl(v.getStatus()));
        }
        for (int i = 0; i < vHeaders.length; i++)
            vendorSheet.autoSizeColumn(i);

        // ── Sheet 2: Purchase Orders ──
        Sheet poSheet = wb.createSheet("Purchase Orders");
        String[] pHeaders = { "PO Number", "Vendor Name", "Status", "Total Amount (INR)", "Created On",
                "Last Updated" };
        Row phRow = poSheet.createRow(0);
        for (int i = 0; i < pHeaders.length; i++) {
            Cell c = phRow.createCell(i);
            c.setCellValue(pHeaders[i]);
            c.setCellStyle(headerStyle);
        }
        int pr = 1;
        for (PurchaseOrder p : pos) {
            double total = p.getItems() == null ? 0
                    : p.getItems().stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum();
            Row row = poSheet.createRow(pr++);
            row.createCell(0).setCellValue(nvl(p.getPoNumber()));
            row.createCell(1).setCellValue(p.getVendor() != null ? nvl(p.getVendor().getName()) : "—");
            row.createCell(2).setCellValue(nvl(p.getStatus()));
            row.createCell(3).setCellValue(total);
            row.createCell(4).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "—");
            row.createCell(5).setCellValue(p.getUpdatedAt() != null ? p.getUpdatedAt().format(DATE_FMT) : "—");
        }
        for (int i = 0; i < pHeaders.length; i++)
            poSheet.autoSizeColumn(i);

        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    // ═══════════════════════ GRN PDF ═══════════════════════
    public byte[] generateGRN(com.procurement.procurement.entity.procurement.Requisition req) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
            com.itextpdf.text.Font labelFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
            com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, BaseColor.GRAY);

            // Header
            Paragraph co = new Paragraph("SMART PROCUREMENT SYSTEM", new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD,
                    new BaseColor(63, 81, 181)));
            co.setAlignment(Element.ALIGN_CENTER);
            doc.add(co);
            Paragraph title = new Paragraph("GOODS RECEIPT NOTE (GRN)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            // Info block
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setSpacingBefore(12);
            info.setSpacingAfter(12);
            addInfoRow(info, "GRN Number:", "GRN-" + nvl(req.getRequisitionNumber()), labelFont, normalFont);
            addInfoRow(info, "Requisition #:", nvl(req.getRequisitionNumber()), labelFont, normalFont);
            addInfoRow(info, "Requested By:",
                    req.getRequestedBy() != null ? nvl(req.getRequestedBy().getUsername()) : "—", labelFont,
                    normalFont);
            addInfoRow(info, "Date Created:", req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_FMT) : "—",
                    labelFont, normalFont);
            addInfoRow(info, "Date Received:", req.getUpdatedAt() != null ? req.getUpdatedAt().format(DATE_FMT) : "—",
                    labelFont, normalFont);
            addInfoRow(info, "Status:", nvl(req.getStatus()), labelFont, normalFont);
            doc.add(info);

            // Items table
            Paragraph itemsTitle = new Paragraph("Received Items", labelFont);
            itemsTitle.setSpacingAfter(6);
            doc.add(itemsTitle);
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 1.5f, 2f, 2f, 2f });
            table.setSpacingAfter(20);
            for (String h : new String[] { "Item Name", "Qty", "Unit Price (INR)", "Subtotal (INR)", "Receipt" }) {
                addHeader(table, h);
            }
            double grandTotal = 0;
            if (req.getItems() != null) {
                for (com.procurement.procurement.entity.procurement.RequisitionItem item : req.getItems()) {
                    double sub = item.getQuantity() * item.getUnitPrice();
                    grandTotal += sub;
                    addCell(table, nvl(item.getItemName()));
                    addCell(table, String.valueOf(item.getQuantity()));
                    addCell(table, String.format("%.2f", item.getUnitPrice()));
                    addCell(table, String.format("%.2f", sub));
                    addCell(table, "Received");
                }
            }
            doc.add(table);
            Paragraph ttl = new Paragraph("Total Value: Rs. " + String.format("%.2f", grandTotal),
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12,
                            com.itextpdf.text.Font.BOLD, new BaseColor(39, 174, 96)));
            ttl.setAlignment(Element.ALIGN_RIGHT);
            ttl.setSpacingAfter(30);
            doc.add(ttl);

            // Signature section
            PdfPTable sig = new PdfPTable(2);
            sig.setWidthPercentage(100);
            sig.setSpacingBefore(20);
            addSignatureCell(sig, "Received By (Employee)", normalFont);
            addSignatureCell(sig, "Verified By (Manager)", normalFont);
            doc.add(sig);

            doc.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph(
                    "This Goods Receipt Note confirms delivery of the above items. — Smart Procurement System",
                    footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("GRN generation failed: " + e.getMessage(), e);
        }
    }

    private void addInfoRow(PdfPTable table, String label, String value,
                            com.itextpdf.text.Font lf, com.itextpdf.text.Font vf) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, lf));
        lCell.setBorder(Rectangle.NO_BORDER);
        lCell.setPaddingBottom(4);
        PdfPCell vCell = new PdfPCell(new Phrase(value, vf));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setPaddingBottom(4);
        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addSignatureCell(PdfPTable table, String label, com.itextpdf.text.Font f) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.addElement(new Phrase(label + "\n\n\nSignature: ___________________________\n\nDate: _______________", f));
        table.addCell(cell);
    }
}

