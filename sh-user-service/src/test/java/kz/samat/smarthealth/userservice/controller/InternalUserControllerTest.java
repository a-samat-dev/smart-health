package kz.samat.smarthealth.userservice.controller;

import java.util.UUID;

import kz.samat.smarthealth.userservice.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static kz.samat.smarthealth.userservice.TestUsers.createUser;
import static kz.samat.smarthealth.userservice.TestUsers.field;
import static kz.samat.smarthealth.userservice.TestUsers.uniqueEmail;
import static kz.samat.smarthealth.userservice.controller.UserController.USER_ID_HEADER;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The endpoints other services call directly, without going through the gateway. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class InternalUserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsAnyUserWithoutTheUserIdHeader() throws Exception {
        String userId = field(createUser(mvc, uniqueEmail()), "id");

        mvc.perform(get("/internal/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("Aigerim"))
                .andExpect(jsonPath("$.type").value("CLIENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void makesTheUserADoctor() throws Exception {
        String userId = field(createUser(mvc, uniqueEmail()), "id");

        mvc.perform(put("/internal/v1/users/{id}/type", userId).contentType(APPLICATION_JSON).content("""
                        {"type": "DOCTOR"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DOCTOR"));

        mvc.perform(get("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId))
                .andExpect(jsonPath("$.type").value("DOCTOR"));
    }

    @Test
    void rejectsAnUnknownType() throws Exception {
        String userId = field(createUser(mvc, uniqueEmail()), "id");

        mvc.perform(put("/internal/v1/users/{id}/type", userId).contentType(APPLICATION_JSON).content("""
                        {"type": "NURSE"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersNotFoundForAnUnknownUser() throws Exception {
        UUID unknown = UUID.randomUUID();

        mvc.perform(get("/internal/v1/users/{id}", unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User " + unknown + " not found"));
        mvc.perform(put("/internal/v1/users/{id}/type", unknown).contentType(APPLICATION_JSON).content("""
                        {"type": "DOCTOR"}
                        """))
                .andExpect(status().isNotFound());
    }
}
