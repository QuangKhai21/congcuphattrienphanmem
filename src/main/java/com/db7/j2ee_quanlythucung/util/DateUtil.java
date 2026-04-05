package com.db7.j2ee_quanlythucung.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Utility bean expose date/time helpers for Thymeleaf templates.
 * Instead of T(java.time.X).method(), use ${@dateUtil.method()}.
 */
@Component("dateUtil")
public class DateUtil {

    public LocalDate today() {
        return LocalDate.now();
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public String formatDate(LocalDate date, String pattern) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public int yearsBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) return 0;
        return Period.between(from, to).getYears();
    }

    public long daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }

    public boolean isBefore(LocalDate a, LocalDate b) {
        if (a == null || b == null) return false;
        return a.isBefore(b);
    }

    public boolean isAfter(LocalDate a, LocalDate b) {
        if (a == null || b == null) return false;
        return a.isAfter(b);
    }

    public boolean isToday(LocalDate date) {
        if (date == null) return false;
        return date.equals(LocalDate.now());
    }

    public boolean isBeforeNow(LocalDate date) {
        if (date == null) return false;
        return date.isBefore(LocalDate.now());
    }

    public boolean isBeforeNowPlusDays(LocalDate date, int days) {
        if (date == null) return false;
        return date.isBefore(LocalDate.now().plusDays(days));
    }

    public LocalDate plusDays(int days) {
        return LocalDate.now().plusDays(days);
    }

    public String todayPattern(String pattern) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    public double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
