package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.MedicalRecord;
import com.db7.j2ee_quanlythucung.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class MedicalRecordExcelService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportMedicalRecords(Pet pet, java.util.List<MedicalRecord> records, String reportDate) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Báo cáo bệnh án");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle altStyle = createAltRowStyle(workbook);

            int rowIdx = 0;

            // Title
            Row titleRow = sheet.createRow(rowIdx++);
            Cell tc = titleRow.createCell(0);
            tc.setCellValue("PET MANAGEMENT SYSTEM - BÁO CÁO BỆNH ÁN");
            tc.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // Pet info
            Row infoRow = sheet.createRow(rowIdx++);
            Cell ic = infoRow.createCell(0);
            ic.setCellValue("Thú cưng: " + (pet != null ? pet.getName() : "N/A")
                    + " | Chủ: " + (pet != null && pet.getOwner() != null ? pet.getOwner().getFullName() : "N/A")
                    + " | Tổng: " + records.size() + " hồ sơ | Xuất: " + reportDate);
            ic.setCellStyle(centerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            rowIdx++;

            // Header
            String[] headers = { "STT", "Ngày khám", "Chẩn đoán", "Triệu chứng", "Điều trị", "Đơn thuốc", "Cân nặng" };
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int idx = 1;
            for (MedicalRecord rec : records) {
                Row row = sheet.createRow(rowIdx++);
                CellStyle rs = (idx % 2 == 0) ? altStyle : dataStyle;
                createCell(row, 0, String.valueOf(idx++), rs);
                createCell(row, 1, rec.getRecordDate() != null ? rec.getRecordDate().format(DF) : "-", rs);
                createCell(row, 2, rec.getDiagnosis() != null ? rec.getDiagnosis() : "-", rs);
                createCell(row, 3, rec.getSymptoms() != null ? rec.getSymptoms() : "-", rs);
                createCell(row, 4, rec.getTreatment() != null ? rec.getTreatment() : "-", rs);
                createCell(row, 5, rec.getPrescription() != null ? rec.getPrescription() : "-", rs);
                createCell(row, 6, rec.getWeight() != null ? String.valueOf(rec.getWeight()) : "-", rs);
            }

            if (records.isEmpty()) {
                Row emptyRow = sheet.createRow(rowIdx++);
                Cell ec = emptyRow.createCell(0);
                ec.setCellValue("Không có hồ sơ bệnh án");
                ec.setCellStyle(centerStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 6));
            }

            rowIdx++;
            Row footerRow = sheet.createRow(rowIdx);
            Cell fc = footerRow.createCell(0);
            fc.setCellValue("Pet Management System - Báo cáo được xuất tự động");
            fc.setCellStyle(centerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 6));

            int[] widths = { 6, 16, 25, 25, 25, 20, 10 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 4);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.DARK_RED.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderTop(BorderStyle.MEDIUM);
        s.setBorderBottom(BorderStyle.MEDIUM);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setWrapText(true);
        return s;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setItalic(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createAltRowStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setWrapText(true);
        return s;
    }
}