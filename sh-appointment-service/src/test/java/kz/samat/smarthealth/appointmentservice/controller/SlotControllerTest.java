package kz.samat.smarthealth.appointmentservice.controller;

import java.time.LocalDate;
import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.ServicesStub;
import kz.samat.smarthealth.appointmentservice.TestcontainersConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static kz.samat.smarthealth.appointmentservice.TestAppointments.CARDIOLOGIST;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.at;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.booked;
import static kz.samat.smarthealth.appointmentservice.TestAppointments.nextMonday;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class SlotControllerTest {

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

    @Test
    void cutsTheWorkingHoursIntoHalfHourSlotsInTheDoctorsTimeZone() throws Exception {
        UUID doctorId = services.addDoctor("Aigerim", "Nurlanova", CARDIOLOGIST, "Cardiologist");
        LocalDate monday = nextMonday();

        slots(doctorId, monday, monday.plusDays(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].startTime").value(at(monday, "09:00").toString()))
                .andExpect(jsonPath("$[0].endTime").value(at(monday, "09:30").toString()))
                .andExpect(jsonPath("$[5].startTime").value(at(monday, "11:30").toString()))
                .andExpect(jsonPath("$[*].available", everyItem(is(true))));
    }

    @Test
    void hasNoSlotsOnDaysOff() throws Exception {
        UUID doctorId = services.addDoctor("Aigerim", "Nurlanova", CARDIOLOGIST, "Cardiologist");
        LocalDate saturday = nextMonday().plusDays(5);

        slots(doctorId, saturday, saturday.plusDays(2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void marksBookedSlotsAsUnavailable() throws Exception {
        UUID doctorId = services.addDoctor("Aigerim", "Nurlanova", CARDIOLOGIST, "Cardiologist");
        LocalDate monday = nextMonday();
        booked(mvc, services.addUser("Dana", "Omarova"), doctorId, CARDIOLOGIST, at(monday, "09:30"));

        slots(doctorId, monday, monday.plusDays(1))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].available").value(false))
                .andExpect(jsonPath("$[2].available").value(true));
    }

    @Test
    void marksSlotsUnavailableWhenTheDoctorHasAnAppointmentAsAPatient() throws Exception {
        UUID doctorId = services.addDoctor("Aigerim", "Nurlanova", CARDIOLOGIST, "Cardiologist");
        UUID otherDoctorId = services.addDoctor("Dana", "Omarova", CARDIOLOGIST, "Cardiologist");
        LocalDate monday = nextMonday();
        booked(mvc, doctorId, otherDoctorId, CARDIOLOGIST, at(monday, "10:00"));

        slots(doctorId, monday, monday.plusDays(1))
                .andExpect(jsonPath("$[2].startTime").value(at(monday, "10:00").toString()))
                .andExpect(jsonPath("$[2].available").value(false));
    }

    @Test
    void rejectsAPeriodLongerThanAMonth() throws Exception {
        UUID doctorId = services.addDoctor("Aigerim", "Nurlanova", CARDIOLOGIST, "Cardiologist");
        LocalDate monday = nextMonday();

        slots(doctorId, monday, monday.plusDays(32))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before to, and at most 31 days earlier"));
    }

    @Test
    void answersNotFoundForAnUnknownDoctor() throws Exception {
        UUID unknown = UUID.randomUUID();

        slots(unknown, nextMonday(), nextMonday().plusDays(1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor " + unknown + " not found"));
    }

    private ResultActions slots(UUID doctorId, LocalDate from, LocalDate to) throws Exception {
        return mvc.perform(get("/api/v1/slots")
                .param("doctorId", doctorId.toString())
                .param("from", at(from, "00:00").toString())
                .param("to", at(to, "00:00").toString()));
    }
}
