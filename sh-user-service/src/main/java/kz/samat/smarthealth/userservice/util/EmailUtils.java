package kz.samat.smarthealth.userservice.util;

import java.util.Locale;

public final class EmailUtils {

    private EmailUtils() {
    }

    /** Emails are stored in lower case, so signing in does not depend on how the address was typed. */
    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
