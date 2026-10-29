package kz.samat.smarthealth.appointmentservice.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.ServicesStub;
import kz.samat.smarthealth.appointmentservice.TestcontainersConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static kz.samat.smarthealth.appointmentservice.TestAppointments.CARDIOLOGIST;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.PEDIATRICIAN;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.at;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.book;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.booked;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.field;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.nextMonday;
import static kz.samat.smarthealth.appointmentservice.controller.AppointmentController.USER_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AppointmentControllerTest {

    private static final ServicesStub services = ServicesStub.start();

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("services.user-service", services::uri);
        registry.add("services.doctor-service", services::uri);
    }

    @AfterAll
    static void stopStub() {
        services.stop();
    }

    @AfterEach
    void resetStub() {
        services.setFailing(false);
    }

    private UUID newDoctor() {
        return services.addDoctor("Aigerim", "Nurlanova", CARDIOLOGIST, "Cardiologist");
    }

    private UUID newClient() {
        return services.addUser("Dana", "Omarova");
    }

    @Nested
    class Booking {

        @Test
        void booksAFreeSlot() throws Exception {
            UUID doctorId = newDoctor();
            UUID clientId = newClient();
            LocalDate monday = nextMonday();

            book(mvc, clientId, doctorId, CARDIOLOGIST, at(monday, "10:30"))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.doctorId").value(doctorId.toString()))
                    .andExpect(jsonPath("$.doctorName").value("Aigerim Nurlanova"))
                    .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                    .andExpect(jsonPath("$.clientName").value("Dana Omarova"))
                    .andExpect(jsonPath("$.specializationName").value("Cardiologist"))
                    .andExpect(jsonPath("$.startTime").value(at(monday, "10:30").toString()))
                    .andExpect(jsonPath("$.endTime").value(at(monday, "11:00").toString()))
                    .andExpect(jsonPath("$.notes").value("Headache for a week"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        void rejectsTimesThatAreNotSlots() throws Exception {
            UUID doctorId = newDoctor();
            LocalDate monday = nextMonday();

            book(mvc, newClient(), doctorId, CARDIOLOGIST, at(monday, "09:10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("The doctor has no slot starting at " + at(monday, "09:10")));
            book(mvc, newClient(), doctorId, CARDIOLOGIST, at(monday, "11:45")).andExpect(status().isBadRequest());
            book(mvc, newClient(), doctorId, CARDIOLOGIST, at(monday, "12:00")).andExpect(status().isBadRequest());
            book(mvc, newClient(), doctorId, CARDIOLOGIST, at(monday.plusDays(5), "09:00")).andExpect(status().isBadRequest());
        }

        @Test
        void rejectsTimesInThePast() throws Exception {
            book(mvc, newClient(), newDoctor(), CARDIOLOGIST, at(nextMonday().minusWeeks(2), "09:00"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("The appointment must start in the future"));
        }

        @Test
        void rejectsASlotThatIsTaken() throws Exception {
            UUID doctorId = newDoctor();
            booked(mvc, newClient(), doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"));

            book(mvc, newClient(), doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("This slot is already taken"));
        }

        @Test
        void rejectsAPatientWhoHasAnotherAppointmentThen() throws Exception {
            UUID clientId = newClient();
            booked(mvc, clientId, newDoctor(), CARDIOLOGIST, at(nextMonday(), "09:00"));

            book(mvc, clientId, newDoctor(), CARDIOLOGIST, at(nextMonday(), "09:00"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("The patient already has an appointment at this time"));
        }

        @Test
        void rejectsASpecializationTheDoctorDoesNotOffer() throws Exception {
            book(mvc, newClient(), newDoctor(), PEDIATRICIAN, at(nextMonday(), "09:00"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("The doctor does not offer specialization " + PEDIATRICIAN));
        }

        @Test
        void rejectsAnAppointmentWithYourself() throws Exception {
            UUID doctorId = newDoctor();

            book(mvc, doctorId, doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("You cannot book an appointment with yourself"));
        }

        @Test
        void answersServiceUnavailableWhenTheDoctorServiceIsDown() throws Exception {
            UUID doctorId = newDoctor();
            services.setFailing(true);

            book(mvc, newClient(), doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Doctor service is unavailable, please try again later"));
        }
    }

    @Nested
    class Listing {

        @Test
        void pagesThroughTheAppointmentsNewestBookingFirst() throws Exception {
            UUID doctorId = newDoctor();
            UUID clientId = newClient();
            List<String> bookedIds = new ArrayList<>();
            for (String time : List.of("09:00", "09:30", "10:00", "10:30", "11:00")) {
                bookedIds.add(booked(mvc, clientId, doctorId, CARDIOLOGIST, at(nextMonday(), time)));
            }

            List<String> listedIds = new ArrayList<>();
            String cursor = null;
            List<Integer> pageSizes = new ArrayList<>();
            do {
                var request = get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("size", "2");
                if (cursor != null) {
                    request.param("cursor", cursor);
                }
                String page = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                List<String> ids = field(page, "items[*].id");
                pageSizes.add(ids.size());
                listedIds.addAll(ids);
                cursor = field(page, "nextCursor");
            } while (cursor != null);

            assertThat(pageSizes).containsExactly(2, 2, 1);
            assertThat(listedIds).containsExactlyElementsOf(bookedIds.reversed());
        }

        @Test
        void sortsByStartTime() throws Exception {
            UUID doctorId = newDoctor();
            UUID clientId = newClient();
            String late = booked(mvc, clientId, doctorId, CARDIOLOGIST, at(nextMonday(), "11:00"));
            String early = booked(mvc, clientId, doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"));

            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("sort", "startTime,asc"))
                    .andExpect(jsonPath("$.items[*].id", contains(early, late)));
            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("sort", "startTime,desc"))
                    .andExpect(jsonPath("$.items[*].id", contains(late, early)));
        }

        @Test
        void searchesBySpecialization() throws Exception {
            UUID clientId = newClient();
            String cardiology = booked(mvc, clientId, newDoctor(), CARDIOLOGIST, at(nextMonday(), "09:00"));
            UUID pediatrician = services.addDoctor("Aliya", "Bekova", PEDIATRICIAN, "Pediatrician");
            booked(mvc, clientId, pediatrician, PEDIATRICIAN, at(nextMonday(), "10:00"));

            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("specialization", "CARDIO"))
                    .andExpect(jsonPath("$.items[*].id", contains(cardiology)));
            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("specialization", "%"))
                    .andExpect(jsonPath("$.items", hasSize(0)));
        }

        @Test
        void filtersByStartTime() throws Exception {
            UUID doctorId = newDoctor();
            UUID clientId = newClient();
            LocalDate monday = nextMonday();
            booked(mvc, clientId, doctorId, CARDIOLOGIST, at(monday, "09:00"));
            String tuesday = booked(mvc, clientId, doctorId, CARDIOLOGIST, at(monday.plusDays(1), "09:00"));
            booked(mvc, clientId, doctorId, CARDIOLOGIST, at(monday.plusDays(2), "09:00"));

            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId)
                            .param("from", at(monday.plusDays(1), "00:00").toString())
                            .param("to", at(monday.plusDays(2), "00:00").toString()))
                    .andExpect(jsonPath("$.items[*].id", contains(tuesday)))
                    .andExpect(jsonPath("$.nextCursor").value(nullValue()));
        }

        @Test
        void showsDoctorsTheAppointmentsBookedWithThem() throws Exception {
            UUID doctorId = newDoctor();
            String appointmentId = booked(mvc, newClient(), doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"));

            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, doctorId))
                    .andExpect(jsonPath("$.items[*].id", contains(appointmentId)))
                    .andExpect(jsonPath("$.items[0].clientName").value("Dana Omarova"));
            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, newClient()))
                    .andExpect(jsonPath("$.items", hasSize(0)));
        }

        @Test
        void rejectsInvalidParameters() throws Exception {
            UUID clientId = newClient();
            UUID doctorId = newDoctor();
            booked(mvc, clientId, doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"));
            booked(mvc, clientId, doctorId, CARDIOLOGIST, at(nextMonday(), "09:30"));
            String cursor = field(mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("size", "1"))
                    .andReturn().getResponse().getContentAsString(), "nextCursor");

            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId)
                            .param("sort", "doctorName,asc").param("size", "0").param("from", "yesterday"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details", contains(
                            "from: invalid value",
                            "size: must be greater than or equal to 1",
                            "sort: must be createdAt or startTime, then asc or desc, such as startTime,asc")));
            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId).param("cursor", "not-a-cursor"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid cursor"));
            mvc.perform(get("/api/v1/appointments").header(USER_ID_HEADER, clientId)
                            .param("cursor", cursor).param("sort", "startTime,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("The cursor belongs to another sort order"));
        }
    }

    @Nested
    class Managing {

        @Test
        void reschedulesToAnotherFreeSlot() throws Exception {
            UUID clientId = newClient();
            String id = booked(mvc, clientId, newDoctor(), CARDIOLOGIST, at(nextMonday(), "09:00"));

            mvc.perform(put("/api/v1/appointments/{id}", id).header(USER_ID_HEADER, clientId).contentType(APPLICATION_JSON)
                            .content("""
                                    {"startTime": "%s", "notes": "Moved"}
                                    """.formatted(at(nextMonday(), "11:30"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startTime").value(at(nextMonday(), "11:30").toString()))
                    .andExpect(jsonPath("$.endTime").value(at(nextMonday(), "12:00").toString()))
                    .andExpect(jsonPath("$.notes").value("Moved"));
        }

        @Test
        void cancelsAndFreesTheSlot() throws Exception {
            UUID doctorId = newDoctor();
            UUID clientId = newClient();
            String id = booked(mvc, clientId, doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"));

            mvc.perform(delete("/api/v1/appointments/{id}", id).header(USER_ID_HEADER, doctorId))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/v1/appointments/{id}", id).header(USER_ID_HEADER, clientId))
                    .andExpect(status().isNotFound());
            booked(mvc, newClient(), doctorId, CARDIOLOGIST, at(nextMonday(), "09:00"));
        }

        @Test
        void isOnlyForThePatientAndTheDoctor() throws Exception {
            UUID clientId = newClient();
            String id = booked(mvc, clientId, newDoctor(), CARDIOLOGIST, at(nextMonday(), "09:00"));
            UUID stranger = newClient();

            mvc.perform(get("/api/v1/appointments/{id}", id).header(USER_ID_HEADER, stranger))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You can only access your own appointments"));
            mvc.perform(delete("/api/v1/appointments/{id}", id).header(USER_ID_HEADER, stranger))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/appointments/{id}", id).header(USER_ID_HEADER, clientId))
                    .andExpect(status().isOk());
        }

        @Test
        void rejectsRequestsWithoutTheUserIdHeader() throws Exception {
            mvc.perform(get("/api/v1/appointments"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));
        }
    }
}
