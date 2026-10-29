package kz.samat.smarthealth.userservice.controller;

import kz.samat.smarthealth.userservice.TestcontainersConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static kz.samat.smarthealth.userservice.TestUsers.PASSWORD;
import static kz.samat.smarthealth.userservice.TestUsers.createSession;
import static kz.samat.smarthealth.userservice.TestUsers.createUser;
import static kz.samat.smarthealth.userservice.TestUsers.field;
import static kz.samat.smarthealth.userservice.TestUsers.newUserJson;
import static kz.samat.smarthealth.userservice.TestUsers.uniqueEmail;
import static kz.samat.smarthealth.userservice.controller.UserController.USER_ID_HEADER;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Nested
    class SignUp {

        @Test
        void createsTheUser() throws Exception {
            String email = uniqueEmail();

            mvc.perform(post("/api/v1/users").contentType(APPLICATION_JSON).content(newUserJson(email.toUpperCase(), PASSWORD)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", startsWith("/api/v1/users/")))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.firstName").value("Aigerim"))
                    .andExpect(jsonPath("$.type").value("CLIENT"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        void rejectsAnEmailThatIsTaken() throws Exception {
            String email = uniqueEmail();
            createUser(mvc, email);

            mvc.perform(post("/api/v1/users").contentType(APPLICATION_JSON).content(newUserJson(email.toUpperCase(), PASSWORD)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("An account with email " + email + " already exists"));
        }

        @Test
        void rejectsInvalidInput() throws Exception {
            mvc.perform(post("/api/v1/users").contentType(APPLICATION_JSON).content(newUserJson("not-an-email", "short")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.details", containsInAnyOrder(
                            "email: must be a well-formed email address",
                            "password: size must be between 8 and 72")));
        }
    }

    @Nested
    class OwnAccount {

        @Test
        void returnsTheProfile() throws Exception {
            String email = uniqueEmail();
            String userId = field(createUser(mvc, email), "id");

            mvc.perform(get("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.lastName").value("Nurlanova"))
                    .andExpect(jsonPath("$.phoneNumber").value(nullValue()))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        void updatesTheProfile() throws Exception {
            String email = uniqueEmail();
            String userId = field(createUser(mvc, email), "id");

            mvc.perform(put("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId).contentType(APPLICATION_JSON).content("""
                            {"firstName": "Aidana", "lastName": "Sarsenova", "phoneNumber": "+7 701 123 45 67", "dateOfBirth": "1995-04-12"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Aidana"))
                    .andExpect(jsonPath("$.dateOfBirth").value("1995-04-12"));

            mvc.perform(get("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId))
                    .andExpect(jsonPath("$.firstName").value("Aidana"))
                    .andExpect(jsonPath("$.lastName").value("Sarsenova"))
                    .andExpect(jsonPath("$.phoneNumber").value("+7 701 123 45 67"))
                    .andExpect(jsonPath("$.dateOfBirth").value("1995-04-12"))
                    .andExpect(jsonPath("$.email").value(email));
        }

        @Test
        void rejectsAnInvalidProfileUpdate() throws Exception {
            String userId = field(createUser(mvc, uniqueEmail()), "id");

            mvc.perform(put("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId).contentType(APPLICATION_JSON).content("""
                            {"firstName": "", "lastName": "Sarsenova", "phoneNumber": "call me", "dateOfBirth": "2999-01-01"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details", containsInAnyOrder(
                            "dateOfBirth: must be a past date",
                            "firstName: must not be blank",
                            "phoneNumber: must be a phone number")));
        }

        @Test
        void deletesTheAccountAndItsSessions() throws Exception {
            String email = uniqueEmail();
            String userId = field(createUser(mvc, email), "id");
            String session = createSession(mvc, email);

            mvc.perform(delete("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId))
                    .andExpect(status().isNoContent());

            mvc.perform(get("/api/v1/users/{id}", userId).header(USER_ID_HEADER, userId))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/api/v1/sessions/{id}/tokens", field(session, "id")).contentType(APPLICATION_JSON).content("""
                            {"refreshToken": "%s"}
                            """.formatted(field(session, "refreshToken"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void rejectsRequestsWithoutTheUserIdHeader() throws Exception {
            String userId = field(createUser(mvc, uniqueEmail()), "id");

            mvc.perform(get("/api/v1/users/{id}", userId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));
        }
    }

    @Nested
    class OtherUsersAccount {

        @Test
        void cannotBeRead() throws Exception {
            String userId = field(createUser(mvc, uniqueEmail()), "id");
            String otherUserId = field(createUser(mvc, uniqueEmail()), "id");

            mvc.perform(get("/api/v1/users/{id}", otherUserId).header(USER_ID_HEADER, userId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You can only access your own account"));
        }

        @Test
        void cannotBeChangedOrDeleted() throws Exception {
            String userId = field(createUser(mvc, uniqueEmail()), "id");
            String otherUserId = field(createUser(mvc, uniqueEmail()), "id");

            mvc.perform(put("/api/v1/users/{id}", otherUserId).header(USER_ID_HEADER, userId).contentType(APPLICATION_JSON).content("""
                            {"firstName": "Aidana", "lastName": "Sarsenova"}
                            """))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/users/{id}", otherUserId).header(USER_ID_HEADER, userId))
                    .andExpect(status().isForbidden());

            mvc.perform(get("/api/v1/users/{id}", otherUserId).header(USER_ID_HEADER, otherUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Aigerim"));
        }
    }
}
