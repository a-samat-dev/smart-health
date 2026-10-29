package kz.samat.smarthealth.doctorservice.controller;

import java.io.InputStream;
import java.util.UUID;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import kz.samat.smarthealth.doctorservice.TestcontainersConfig;
import kz.samat.smarthealth.doctorservice.UserServiceStub;
import kz.samat.smarthealth.doctorservice.UserServiceStub.ReceivedRequest;
import kz.samat.smarthealth.doctorservice.repository.DoctorRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static kz.samat.smarthealth.doctorservice.TestDoctors.CARDIOLOGIST;
import static kz.samat.smarthealth.doctorservice.TestDoctors.DERMATOLOGIST;
import static kz.samat.smarthealth.doctorservice.TestDoctors.PEDIATRICIAN;
import static kz.samat.smarthealth.doctorservice.TestDoctors.doctorJson;
import static kz.samat.smarthealth.doctorservice.TestDoctors.field;
import static kz.samat.smarthealth.doctorservice.TestDoctors.pdf;
import static kz.samat.smarthealth.doctorservice.TestDoctors.register;
import static kz.samat.smarthealth.doctorservice.TestDoctors.registerDoctor;
import static kz.samat.smarthealth.doctorservice.TestDoctors.uniqueLicenseNumber;
import static kz.samat.smarthealth.doctorservice.controller.DoctorController.USER_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class DoctorControllerTest {

    private static final UserServiceStub userService = UserServiceStub.start();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MinioClient minioClient;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("services.user-service", userService::uri);
    }

    @AfterAll
    static void stopStub() {
        userService.stop();
    }

    @AfterEach
    void resetStub() {
        userService.setFailing(false);
        userService.setFailingTypeUpdates(false);
    }

    @Nested
    class Registration {

        @Test
        void createsTheProfileAndMakesTheUserADoctor() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");

            register(mvc, userId, doctorJson("KZ-0042", DERMATOLOGIST, CARDIOLOGIST), pdf("license"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/doctors/" + userId))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.firstName").value("Aigerim"))
                    .andExpect(jsonPath("$.lastName").value("Nurlanova"))
                    .andExpect(jsonPath("$.licenseNumber").value("KZ-0042"))
                    .andExpect(jsonPath("$.specializations[0].name").value("Cardiologist"))
                    .andExpect(jsonPath("$.specializations[1].name").value("Dermatologist"))
                    .andExpect(jsonPath("$.address.city").value("Almaty"))
                    .andExpect(jsonPath("$.address.houseNumber").value("52A"))
                    .andExpect(jsonPath("$.contactPhone").value("+7 727 123 45 67"))
                    .andExpect(jsonPath("$.workingHours.start").value("09:00"))
                    .andExpect(jsonPath("$.workingHours.end").value("17:00"))
                    .andExpect(jsonPath("$.daysOff[0]").value("SATURDAY"))
                    .andExpect(jsonPath("$.daysOff[1]").value("SUNDAY"))
                    .andExpect(jsonPath("$.timeZone").value("Asia/Almaty"))
                    .andExpect(jsonPath("$.licenseFileUrl").doesNotExist());

            assertThat(userService.requestsFor(userId))
                    .contains(new ReceivedRequest("PUT", "/internal/v1/users/" + userId + "/type"));
        }

        @Test
        void storesTheLicenseFileInMinioAndItsLinkInTheDatabase() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");

            register(mvc, userId, doctorJson(), pdf("the license")).andExpect(status().isCreated());

            String link = doctorRepository.findById(userId).orElseThrow().getLicenseFileUrl();
            assertThat(link).startsWith("http").endsWith("/doctor-licenses/" + userId + "/license.pdf");
            try (InputStream stored = minioClient.getObject(GetObjectArgs.builder()
                    .bucket("doctor-licenses").object(userId + "/license.pdf").build())) {
                assertThat(stored.readAllBytes()).isEqualTo(pdf("the license"));
            }
        }

        @Test
        void rejectsALicenseFileThatIsNotAPdf() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");

            register(mvc, userId, doctorJson(), "just text".getBytes())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("The license file must be a PDF"));
            assertThat(doctorRepository.existsById(userId)).isFalse();
        }

        @Test
        void rejectsAnInvalidProfile() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");

            register(mvc, userId, """
                    {"licenseNumber": "", "specializationIds": [], "address": {"country": "Kazakhstan", "city": "",
                     "street": "Abay Avenue", "houseNumber": "52A"}, "contactEmail": "clinic@example.com",
                     "contactPhone": "call me", "workingHours": {"start": "09:00", "end": "17:00"}, "daysOff": [],
                     "timeZone": "Asia/Almaty"}
                    """, pdf("license"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details", containsInAnyOrder(
                            "address.city: must not be blank",
                            "contactPhone: must be a phone number",
                            "licenseNumber: must not be blank",
                            "specializationIds: must not be empty")));
        }

        @Test
        void rejectsUnknownSpecializationsTimeZonesAndWorkingHours() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");

            register(mvc, userId, doctorJson(uniqueLicenseNumber(), CARDIOLOGIST, 99), pdf("license"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Unknown specialization ids: [99]"));
            register(mvc, userId, doctorJson().replace("Asia/Almaty", "Mars/Olympus"), pdf("license"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Unknown time zone Mars/Olympus"));
            register(mvc, userId, doctorJson().replace("\"17:00\"", "\"08:00\""), pdf("license"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Working hours must end after they start"));
        }

        @Test
        void rejectsASecondRegistration() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            registerDoctor(mvc, userId);

            register(mvc, userId, doctorJson(), pdf("license"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("You are already registered as a doctor"));
        }

        @Test
        void rejectsALicenseNumberAnotherDoctorHas() throws Exception {
            String licenseNumber = uniqueLicenseNumber();
            register(mvc, userService.addUser("Aigerim", "Nurlanova"), doctorJson(licenseNumber, CARDIOLOGIST), pdf("license"))
                    .andExpect(status().isCreated());

            register(mvc, userService.addUser("Dana", "Omarova"), doctorJson(licenseNumber, CARDIOLOGIST), pdf("license"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Another doctor is registered with license number " + licenseNumber));
        }

        @Test
        void answersNotFoundForAUserSignedUpNowhere() throws Exception {
            UUID unknown = UUID.randomUUID();

            register(mvc, unknown, doctorJson(), pdf("license"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User " + unknown + " not found"));
        }

        @Test
        void retriesAndAnswersServiceUnavailableWhenTheUserServiceIsDown() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            userService.setFailing(true);

            register(mvc, userId, doctorJson(), pdf("license"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("User service is unavailable, please try again later"));

            // The first attempt and three retries.
            assertThat(userService.requestsFor(userId)).hasSize(4);
            assertThat(doctorRepository.existsById(userId)).isFalse();
        }

        @Test
        void rollsTheProfileBackWhenTheUserCannotBeMadeADoctor() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            userService.setFailingTypeUpdates(true);

            register(mvc, userId, doctorJson(), pdf("license")).andExpect(status().isServiceUnavailable());

            assertThat(doctorRepository.existsById(userId)).isFalse();
        }

        @Test
        void rejectsRequestsWithoutTheUserIdHeader() throws Exception {
            mvc.perform(multipart("/api/v1/doctors")
                            .file(new MockMultipartFile("doctor", "", "application/json", doctorJson().getBytes()))
                            .file(new MockMultipartFile("licenseFile", "license.pdf", "application/pdf", pdf("license"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Profiles {

        @Test
        void canBeReadByAnyone() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            registerDoctor(mvc, userId);

            mvc.perform(get("/api/v1/doctors/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Aigerim"));
            mvc.perform(get("/api/v1/doctors/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void canBeListedBySpecialization() throws Exception {
            UUID pediatrician = userService.addUser("Aigerim", "Nurlanova");
            register(mvc, pediatrician, doctorJson(uniqueLicenseNumber(), PEDIATRICIAN), pdf("license"))
                    .andExpect(status().isCreated());
            UUID cardiologist = userService.addUser("Dana", "Omarova");
            register(mvc, cardiologist, doctorJson(uniqueLicenseNumber(), CARDIOLOGIST), pdf("license"))
                    .andExpect(status().isCreated());

            mvc.perform(get("/api/v1/doctors").param("specializationId", String.valueOf(PEDIATRICIAN)).param("size", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[*].userId", hasItem(pediatrician.toString())))
                    .andExpect(jsonPath("$.items[*].userId", not(hasItem(cardiologist.toString()))))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(100));
            mvc.perform(get("/api/v1/doctors").param("size", "500"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value("size: must be less than or equal to 100"));
        }

        @Test
        void canBeReplacedByTheirDoctor() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            registerDoctor(mvc, userId);

            mvc.perform(put("/api/v1/doctors/{id}", userId).header(USER_ID_HEADER, userId).contentType(APPLICATION_JSON)
                            .content(doctorJson("KZ-" + userId, PEDIATRICIAN)
                                    .replace("\"09:00\"", "\"10:00\"")
                                    .replace("[\"SUNDAY\", \"SATURDAY\"]", "[\"WEDNESDAY\"]")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.licenseNumber").value("KZ-" + userId))
                    .andExpect(jsonPath("$.specializations[*].name", containsInAnyOrder("Pediatrician")))
                    .andExpect(jsonPath("$.workingHours.start").value("10:00"));

            mvc.perform(get("/api/v1/doctors/{id}", userId))
                    .andExpect(jsonPath("$.daysOff", containsInAnyOrder("WEDNESDAY")))
                    .andExpect(jsonPath("$.specializations[*].id", containsInAnyOrder(PEDIATRICIAN)));
        }

        @Test
        void cannotBeChangedByAnotherUser() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            registerDoctor(mvc, userId);

            mvc.perform(put("/api/v1/doctors/{id}", userId).header(USER_ID_HEADER, UUID.randomUUID())
                            .contentType(APPLICATION_JSON).content(doctorJson()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You can only access your own doctor profile"));
        }
    }

    @Nested
    class LicenseFiles {

        @Test
        void canBeDownloadedOnlyByTheirDoctor() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            register(mvc, userId, doctorJson(), pdf("my license")).andExpect(status().isCreated());

            mvc.perform(get("/api/v1/doctors/{id}/license-file", userId).header(USER_ID_HEADER, userId))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(content().bytes(pdf("my license")));
            mvc.perform(get("/api/v1/doctors/{id}/license-file", userId).header(USER_ID_HEADER, UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void canBeReplaced() throws Exception {
            UUID userId = userService.addUser("Aigerim", "Nurlanova");
            String registeredAt = field(registerDoctor(mvc, userId), "updatedAt");

            mvc.perform(multipart(HttpMethod.PUT, "/api/v1/doctors/{id}/license-file", userId)
                            .file(new MockMultipartFile("licenseFile", "new.pdf", "application/pdf", pdf("renewed")))
                            .header(USER_ID_HEADER, userId))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/v1/doctors/{id}/license-file", userId).header(USER_ID_HEADER, userId))
                    .andExpect(content().bytes(pdf("renewed")));
            mvc.perform(get("/api/v1/doctors/{id}", userId))
                    .andExpect(jsonPath("$.updatedAt", not(registeredAt)));
        }
    }
}
