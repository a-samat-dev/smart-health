package kz.samat.smarthealth.doctorservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.web.multipart.MultipartFile;

public final class PdfUtils {

    /** Every PDF file starts with these bytes, whatever content type the browser sent. */
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private PdfUtils() {
    }

    public static boolean isPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        try (InputStream content = file.getInputStream()) {
            return Arrays.equals(content.readNBytes(PDF_SIGNATURE.length), PDF_SIGNATURE);
        } catch (IOException e) {
            return false;
        }
    }
}
