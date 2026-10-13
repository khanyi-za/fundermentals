package com.khanyi.java.fundermentals.model;

import java.util.List;
import java.util.Locale;

/**
 * Something that can be found with a free-text search.
 * Implementers only say WHICH text to search; the matching rule lives here once.
 */
public interface Searchable {
    /**
     * @return the text fields a search looks in, e.g. title, artist, album ( things that can be searched)
     */
    List<String> searchableFields();

    /**
     * Case-insensitive "contains" search across every searchable field.
     * A blank or null query matches everything, like an empty search box.
     */
    default boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        for (String field : searchableFields()) {
            if (containsIgnoreCase(field, needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A static helper: belongs to the interface itself, not to any object.
     * Null-safe, so an item with a missing field (e.g. no album) never throws.
     */
    static boolean containsIgnoreCase(String text, String lowerCaseNeedle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }

}
