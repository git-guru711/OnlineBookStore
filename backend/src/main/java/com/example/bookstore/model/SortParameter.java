package com.example.bookstore.model;

import com.example.bookstore.exception.InvalidRequestParameterException;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;

/** Parses a {@code sort=field[,asc|desc]} query parameter against an allow-list of fields. */
public final class SortParameter {

    private static final String PARAMETER = "sort";

    private SortParameter() {}

    public static Sort parse(String value, Set<String> allowedFields) {
        String[] parts = value.split(",", -1);
        String field = parts[0].trim();
        if (parts.length > 2 || !allowedFields.contains(field)) {
            throw new InvalidRequestParameterException(
                    PARAMETER, "must be one of %s, optionally followed by ,asc or ,desc".formatted(allowedFields));
        }
        return Sort.by(direction(parts), field);
    }

    private static Sort.Direction direction(String[] parts) {
        if (parts.length == 1) {
            return Sort.Direction.ASC;
        }
        return switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new InvalidRequestParameterException(PARAMETER, "direction must be asc or desc");
        };
    }
}
