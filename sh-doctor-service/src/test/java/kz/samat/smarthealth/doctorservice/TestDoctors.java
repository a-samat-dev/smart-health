package kz.samat.smarthealth.doctorservice;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static kz.samat.smarthealth.doctorservice.controller.DoctorController.USER_ID_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Registers doctors through the API for tests. */
public final class TestDoctors {

    /** Specialization ids from the V1 migration. */
    public static final int CARDIOLOGIST = 3;
    public static final int DERMATOLOGIST = 5;
    public static final int PEDIATRICIAN = 18;

    private TestDoctors() {
    }

    public static byte[] pdf(String text) {
        return ("%PDF-1.7\n" + text + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);
    }

    public static String uniqueLicenseNumber() {
        return "KZ-" + UUID.randomUUID().toString().substring(0, 13);
    }

    /** A complete profile: a dermatologist and cardiologist working 09:00 to 17:00, Monday to Friday. */
    public static String doctorJson(String licenseNumber, int... specializationIds) {
        return """
                {
                  "licenseNumber": "%s",
                  "specializationIds": %s,
                  "address": {"country": "Kazakhstan", "city": "Almaty", "street": "Abay Avenue", "houseNumber": "52A"},
                  "contactEmail": "clinic@example.com",
                  "contactPhone": "+7 727 123 45 67",
                  "workingHours": {"start": "09:00", "end": "17:00"},
                  "daysOff": ["SUNDAY", "SATURDAY"],
                  "timeZone": "Asia/Almaty"
                }
                """.formatted(licenseNumber, Arrays.toString(specializationIds));
    }

    public static String doctorJson() {
        return doctorJson(uniqueLicenseNumber(), DERMATOLOGIST, CARDIOLOGIST);
    }

    public static ResultActions register(MockMvc mvc, UUID userId, String doctorJson, byte[] licenseFile)
            throws Exception {
        return mvc.perform(multipart("/api/v1/doctors")
                .file(new MockMultipartFile("doctor", "", "application/json", doctorJson.getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("licenseFile", "license.pdf", "application/pdf", licenseFile))
                .header(USER_ID_HEADER, userId));
    }

    /** Registers the user as a doctor with {@link #doctorJson()} and returns the profile. */
    public static String registerDoctor(MockMvc mvc, UUID userId) throws Exception {
        return register(mvc, userId, doctorJson(), pdf("license"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    public static <T> T field(String json, String path) {
        return JsonPath.read(json, "$." + path);
    }
}
