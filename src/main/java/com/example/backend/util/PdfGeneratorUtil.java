package com.example.backend.util;

import com.example.backend.dto.SocietComplianceOverviewDTO;
import com.example.backend.dto.SocietComplianceOverviewDTO.DocTypeBreakdownDTO;
import com.example.backend.dto.SocietComplianceOverviewDTO.ExerciceBreakdownDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

public class PdfGeneratorUtil {

    public static byte[] generateComplianceReport(SocietComplianceOverviewDTO overview) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font smallBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Title
            Paragraph title = new Paragraph("Compliance Overview Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Basic Info
            document.add(new Paragraph("Societé: " + overview.getRaisonSociale(), headerFont));
            document.add(new Paragraph("ICE: " + overview.getIce(), normalFont));
            document.add(new Paragraph("Adresse: " + (overview.getAdresse() != null ? overview.getAdresse() : "N/A"), normalFont));
            
            if (overview.getAccountant() != null) {
                document.add(new Paragraph("Accountant: " + overview.getAccountant().getAccountantName(), normalFont));
            }
            document.add(new Paragraph("Status: " + overview.getComplianceStatus(), headerFont));
            document.add(new Paragraph("Global Compliance: " + String.format("%.2f", overview.getCompliancePercentage()) + "%", headerFont));

            document.add(new Paragraph("\n"));

            // Summary Table
            PdfPTable summaryTable = new PdfPTable(6);
            summaryTable.setWidthPercentage(100);
            addTableHeader(summaryTable, new String[]{"Total", "Pending", "Approved", "Rejected", "Deleted", "Legal Basis"}, smallBoldFont);
            addRows(summaryTable, new String[]{
                    String.valueOf(overview.getTotalDocuments()),
                    String.valueOf(overview.getPendingDocuments()),
                    String.valueOf(overview.getApprovedDocuments()),
                    String.valueOf(overview.getRejectedDocuments()),
                    String.valueOf(overview.getDeletedDocuments()),
                    String.valueOf(overview.getTotalDocuments() - overview.getDeletedDocuments())
            }, normalFont);
            document.add(summaryTable);

            document.add(new Paragraph("\n"));

            // Breakdown Tables
            document.add(new Paragraph("Fiscal Year Breakdown :", headerFont));
            document.add(new Paragraph("\n"));

            PdfPTable exerciceTable = new PdfPTable(4);
            exerciceTable.setWidthPercentage(100);
            addTableHeader(exerciceTable, new String[]{"Exercice", "Total", "Approved", "Compliance (%)"}, smallBoldFont);
            for (ExerciceBreakdownDTO breakdown : overview.getExerciceBreakdowns()) {
                addRows(exerciceTable, new String[]{
                        String.valueOf(breakdown.getExerciceComptable()),
                        String.valueOf(breakdown.getTotal()),
                        String.valueOf(breakdown.getApproved()),
                        String.format("%.2f %%", breakdown.getCompliancePercentage())
                }, normalFont);
            }
            document.add(exerciceTable);

            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Document Type Breakdown :", headerFont));
            document.add(new Paragraph("\n"));

            PdfPTable docTypeTable = new PdfPTable(4);
            docTypeTable.setWidthPercentage(100);
            addTableHeader(docTypeTable, new String[]{"Type", "Total", "Approved", "Compliance (%)"}, smallBoldFont);
            for (DocTypeBreakdownDTO breakdown : overview.getDocTypeBreakdowns()) {
                addRows(docTypeTable, new String[]{
                        breakdown.getTypeDocument(),
                        String.valueOf(breakdown.getTotal()),
                        String.valueOf(breakdown.getApproved()),
                        String.format("%.2f %%", breakdown.getCompliancePercentage())
                }, normalFont);
            }
            document.add(docTypeTable);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return out.toByteArray();
    }

    private static void addTableHeader(PdfPTable table, String[] columnNames, Font font) {
        Stream.of(columnNames)
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle, font));
                    table.addCell(header);
                });
    }

    private static void addRows(PdfPTable table, String[] cellValues, Font font) {
        for (String cellValue : cellValues) {
            table.addCell(new Phrase(cellValue, font));
        }
    }
}
