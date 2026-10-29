package kz.samat.smarthealth.appointmentservice.util;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.exception.BadRequestException;

/**
 * Cursors for the appointment list. A cursor names the last appointment of a page by its sort value, such as
 * its creation time, and its id; the next page starts after it. Clients treat cursors as opaque strings.
 */
public final class CursorUtils {

    /**
     * @param sort  the sort order the cursor was made for, such as "createdAt,desc"
     * @param value the last appointment's value of the sorted property
     * @param id    the last appointment's id, which orders appointments with the same value
     */
    public record Cursor(String sort, Instant value, UUID id) {
    }

    private static final String SEPARATOR = "|";

    private CursorUtils() {
    }

    public static String encode(Cursor cursor) {
        String raw = cursor.sort() + SEPARATOR + cursor.value() + SEPARATOR + cursor.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String encoded) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
                    .split("\\" + SEPARATOR);
            if (parts.length != 3) {
                throw new BadRequestException("Invalid cursor");
            }
            return new Cursor(parts[0], Instant.parse(parts[1]), UUID.fromString(parts[2]));
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new BadRequestException("Invalid cursor");
        }
    }
}
