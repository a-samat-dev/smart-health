package kz.samat.smarthealth.userservice.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import kz.samat.smarthealth.userservice.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static kz.samat.smarthealth.userservice.TestUsers.field;
import static kz.samat.smarthealth.userservice.TestUsers.signedInUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks the published keys the way a verifier such as the gateway uses them. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class JwksControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void publishesTheKeyThatVerifiesAccessTokens() throws Exception {
        String accessToken = field(signedInUser(mvc), "accessToken");
        JwkSet keys = Jwks.setParser().build().parse(publishedKeys());

        // Picks the published key named by the token's "kid" header, as the gateway does.
        String subject = Jwts.parser()
                .keyLocator(header -> keys.getKeys().stream()
                        .filter(key -> key.getId().equals(((ProtectedHeader) header).getKeyId()))
                        .map(Jwk::toKey)
                        .findFirst()
                        .orElseThrow())
                .build()
                .parseSignedClaims(accessToken).getPayload().getSubject();
        assertThat(subject).isNotBlank();
    }

    @Test
    void publishesOnlyThePublicPartOfTheKey() throws Exception {
        mvc.perform(get(JwksController.JWKS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist());
    }

    private String publishedKeys() throws Exception {
        return mvc.perform(get(JwksController.JWKS_PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
