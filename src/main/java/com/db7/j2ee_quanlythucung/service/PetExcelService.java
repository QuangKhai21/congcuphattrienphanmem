package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetExcelService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] exportPets(List<Pet> pets, String reportDate) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách thú cưng");

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);
            CellStyle altStyle = createAltRowStyle(workbook);

            int rowIdx = 0;

            // Title row
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("PET MANAGEMENT SYSTEM - BÁO CÁO DANH SÁCH THÚ CƯNG");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            // Info row
            Row infoRow = sheet.createRow(rowIdx++);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("Ngày xuất báo cáo: " + reportDate + " | Tổng số: " + pets.size() + " thú cưng");
            infoCell.setCellStyle(centerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            rowIdx++; // blank row

            // Header row
            String[] headers = { "STT", "Tên thú cưng", "Loài", "Giống", "Giới tính", "Tuổi", "Cân nặng (kg)", "Chủ nuôi", "Trạng thái" };
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int idx = 1;
            for (Pet pet : pets) {
                Row row = sheet.createRow(rowIdx++);
                boolean alt = (idx % 2 == 0);
                CellStyle rowStyle = alt ? altStyle : dataStyle;

                createCell(row, 0, String.valueOf(idx++), rowStyle);
                createCell(row, 1, pet.getName(), rowStyle);
                createCell(row, 2, pet.getCategory() != null ? pet.getCategory().getName() : "-", rowStyle);
                createCell(row, 3, pet.getBreed() != null ? pet.getBreed().getName() : "-", rowStyle);

                String gender = pet.getGender() != null
                        ? (pet.getGender().name().equals("MALE") ? "Đực" : "Cái") : "-";
                createCell(row, 4, gender, rowStyle);

                String age = "-";
                if (pet.getDateOfBirth() != null) {
                    int years = LocalDate.now().getYear() - pet.getDateOfBirth().getYear();
                    age = String.valueOf(years);
                }
                createCell(row, 5, age, rowStyle);

                createCell(row, 6, pet.getWeight() != null ? String.valueOf(pet.getWeight()) : "-", rowStyle);
                createCell(row, 7, pet.getOwner() != null ? pet.getOwner().getFullName() : "-", rowStyle);

                String status = pet.getStatus() != null
                        ? switch (pet.getStatus().name()) {
                            case "ACTIVE" -> "Đang nuôi";
                            case "PASSED" -> "Đã mất";
                            case "TRANSFERRED" -> "Đã chuyển";
                            default -> pet.getStatus().name();
                        } : "-";
                createCell(row, 8, status, rowStyle);
            }

            // Footer row
            rowIdx++;
            Row footerRow = sheet.createRow(rowIdx++);
            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue("Pet Management System - Báo cáo được xuất tự động");
            footerCell.setCellStyle(centerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 8));

            // Column widths
            int[] widths = { 6, 20, 12, 15, 10, 8, 12, 20, 12 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // Freeze header
            sheet.createFreezePane(0, 5);

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
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createAltRowStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }
}