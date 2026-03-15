package com.shopee.ecommerce.util;

import java.text.Normalizer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utility for generating URL-friendly slugs.
 *
 * Examples:
 *   "Áo Thun Nam Cao Cấp"  → "ao-thun-nam-cao-cap"
 *   "iPhone 15 Pro Max!!!" → "iphone-15-pro-max"
 */
public class SlugUtils {

    private static final Pattern NON_ASCII      = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern DIACRITICS     = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE     = Pattern.compile("\\s+");
    private static final Pattern MULTI_DASH     = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING_DASH = Pattern.compile("^-|-$");

    private SlugUtils() { /* utility class */ }

    /**
     * Convert any string (including Vietnamese) to a URL slug.
     *
     * @param input raw text, e.g. "Áo Thun Nam"
     * @return slug, e.g. "ao-thun-nam"
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";

        // 1. Unicode NFD decomposition → separate base letters from diacritics
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 2. Strip combining diacritical marks (removes accents / tone marks)
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");

        // 3. Replace remaining non-ASCII with empty (handles đ → d already via NFD for most,
        //    but 'đ' needs explicit handling)
        String ascii = withoutDiacritics
                .replace("đ", "d")
                .replace("Đ", "d");
        ascii = NON_ASCII.matcher(ascii).replaceAll("");

        // 4. Lowercase
        String lower = ascii.toLowerCase();

        // 5. Remove characters that are not alphanumeric, space, or dash
        String cleaned = NOT_ALPHANUMERIC.matcher(lower).replaceAll("");

        // 6. Collapse whitespace → single dash
        String dashed = WHITESPACE.matcher(cleaned.trim()).replaceAll("-");

        // 7. Collapse multiple consecutive dashes
        String collapsed = MULTI_DASH.matcher(dashed).replaceAll("-");

        // 8. Strip leading/trailing dashes
        return LEADING_TRAILING_DASH.matcher(collapsed).replaceAll("");
    }

    /**
     * Generate a unique slug by appending a numeric suffix if needed.
     *
     * <pre>
     * Usage:
     *   String slug = SlugUtils.generateUnique(
     *       SlugUtils.toSlug(product.getName()),
     *       productRepository::existsBySlug
     *   );
     * </pre>
     *
     * @param base      base slug, e.g. "ao-thun-nam"
     * @param existsFn  predicate that returns true if the slug is already taken
     * @return unique slug, e.g. "ao-thun-nam" or "ao-thun-nam-2"
     */
    public static String generateUnique(String base, Predicate<String> existsFn) {
        if (!existsFn.test(base)) {
            return base;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = base + "-" + suffix;
            suffix++;
        } while (existsFn.test(candidate));
        return candidate;
    }
}