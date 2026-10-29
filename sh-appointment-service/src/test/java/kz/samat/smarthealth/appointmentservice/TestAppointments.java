package kz.samat.smarthealth.appointmentservice;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static kz.samat.smarthealth.appointmentservice.controller.AppointmentController.USER_ID_HEADER;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Times in the stub doctors' time zone, and booking through the API. */
public final class TestAppointments {

    public static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");
    public static final int CARDIOLOGIST = 3;
    public static final int PEDIATRICIAN = 18;

    private TestAppointments() {
    }

    /** A working day for the stub doctors that is always in the future. */
    public static LocalDate nextMonday() {
        return LocalDate.now(ALMATY).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /** The moment it is the given time of day in Almaty, such as "09:30". */
    public static Instant at(LocalDate date, String time) {
        return ZonedDateTime.of(date, LocalTime.parse(time), ALMATY).toInstant();
    }

    public static ResultActions book(MockMvc mvc, UUID clientId, UUID doctorId, int specializationId, Instant startTime)
            throws Exception {
        return mvc.perform(post("/api/v1/appointments").header(USER_ID_HEADER, clientId).contentType(APPLICATION_JSON)
                .content("""
                        {"doctorId": "%s", "specializationId": %d, "startTime": "%s", "notes": "Headache for a week"}
                        """.formatted(doctorId, specializationId, startTime)));
    }

    /** Books the slot and returns the appointment's id. */
    public static String booked(MockMvc mvc, UUID clientId, UUID doctorId, int specializationId, Instant startTime)
            throws Exception {
        String appointment = book(mvc, clientId, doctorId, specializationId, startTime)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return field(appointment, "id");
    }

    public static <T> T field(String json, String path) {
        return JsonPath.read(json, "$." + path);
    }
}
