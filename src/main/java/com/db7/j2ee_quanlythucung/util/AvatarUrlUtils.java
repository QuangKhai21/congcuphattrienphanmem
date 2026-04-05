package com.db7.j2ee_quanlythucung.util;

/**
 * Chuẩn hoá URL avatar để trình duyệt luôn tải đúng (tránh relative path gắn vào /vet-qa/...).
 */
public final class AvatarUrlUtils {

    private AvatarUrlUtils() {
    }

    public static String normalize(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim();
        if (u.isEmpty()) {
            return null;
        }
        String lower = u.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://") || u.startsWith("//")) {
            return u;
        }
        u = u.replace('\\', '/');
        // Đường dẫn kiểu D:/project/uploads/... hoặc file:///.../uploads/...
        if (u.length() > 2 && u.charAt(1) == ':' && u.charAt(2) == '/') {
            int idx = u.indexOf("/uploads/");
            if (idx >= 0) {
                return u.substring(idx);
            }
            return null;
        }
        if (lower.startsWith("file:")) {
            int idx = u.indexOf("/uploads/");
            if (idx >= 0) {
                return u.substring(idx);
            }
            return null;
        }
        if (!u.startsWith("/")) {
            u = "/" + u;
        }
        // Chỉ chấp nhận đường dẫn nội bộ dưới /uploads/ — tránh chuỗi rác ("Avatar", "/foo", v.v.) vẫn tạo <img> vỡ
        lower = u.toLowerCase();
        if (!lower.startsWith("/uploads/")) {
            return null;
        }
        return u;
    }

    /** Chữ cái đầu tiên (bỏ qua số/ký tự đặc biệt) — tránh hiển thị "2" với tên kiểu 2045_Đinh... */
    public static String nameInitial(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        for (int i = 0; i < fullName.length(); i++) {
            char c = fullName.charAt(i);
            if (Character.isLetter(c)) {
                return String.valueOf(Character.toUpperCase(c));
            }
        }
        return "?";
    }
}
