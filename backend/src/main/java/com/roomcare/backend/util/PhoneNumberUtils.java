package com.roomcare.backend.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa số điện thoại di động Việt Nam sang E.164 (+84...) và hỗ trợ đối chiếu dữ liệu cũ (0...).
 */
public final class PhoneNumberUtils {

    private static final Pattern VN_LOCAL_PATTERN = Pattern.compile("^0([35789]\\d{8})$");
    private static final Pattern VN_COUNTRY_PATTERN = Pattern.compile("^(?:\\+?84)([35789]\\d{8})$");

    private PhoneNumberUtils() {
    }

    public static boolean isValidVietnamMobile(String raw) {
        return extractCoreNineDigits(raw) != null;
    }

    public static String normalizeToE164(String raw) {
        String core = extractCoreNineDigits(raw);
        if (core == null) {
            throw new IllegalArgumentException(
                    "Số điện thoại Việt Nam không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)"
            );
        }
        return "+84" + core;
    }

    public static String toLocalVietnamFormat(String raw) {
        String core = extractCoreNineDigits(raw);
        if (core == null) {
            throw new IllegalArgumentException(
                    "Số điện thoại Việt Nam không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)"
            );
        }
        return "0" + core;
    }

    /**
     * Trả về cả dạng E.164 (+84...) và dạng nội địa (0...) để tra cứu đăng nhập / chống trùng
     * tương thích với tài khoản cũ.
     */
    public static List<String> lookupVariants(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String sanitized = sanitize(raw);
        Set<String> variants = new LinkedHashSet<>();
        String core = extractCoreNineDigits(sanitized);
        if (core != null) {
            variants.add("+84" + core);
            variants.add("0" + core);
        } else {
            variants.add(sanitized);
        }
        return List.copyOf(variants);
    }

    private static String extractCoreNineDigits(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String sanitized = sanitize(raw);
        Matcher localMatcher = VN_LOCAL_PATTERN.matcher(sanitized);
        if (localMatcher.matches()) {
            return localMatcher.group(1);
        }
        Matcher intlMatcher = VN_COUNTRY_PATTERN.matcher(sanitized);
        if (intlMatcher.matches()) {
            return intlMatcher.group(1);
        }
        return null;
    }

    private static String sanitize(String raw) {
        return raw.trim().replaceAll("[\\s.\\-()]", "");
    }
}
