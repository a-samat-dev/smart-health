package kz.samat.smarthealth.doctorservice.controller;

import kz.samat.smarthealth.doctorservice.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class SpecializationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsTheSpecializationsFromTheMigrationByName() throws Exception {
        mvc.perform(get("/api/v1/specializations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Allergist"))
                .andExpect(jsonPath("$[2].name").value("Cardiologist"))
                .andExpect(jsonPath("$[23].name").value("Urologist"));
    }
}
