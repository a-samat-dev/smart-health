package kz.samat.smarthealth.userservice.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyConfigTest {

    private final JwtKeyConfig config = new JwtKeyConfig();

    @Test
    void readsThePrivateKeyFromAPemFileAndDerivesThePublicKey(@TempDir Path dir) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair original = generator.generateKeyPair();
        Path pemFile = dir.resolve("jwt-private.pem");
        Files.writeString(pemFile, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(original.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n");

        KeyPair loaded = config.jwtKeyPair(properties("file:" + pemFile), new DefaultResourceLoader());

        assertThat(loaded.getPrivate().getEncoded()).isEqualTo(original.getPrivate().getEncoded());
        assertThat(loaded.getPublic().getEncoded()).isEqualTo(original.getPublic().getEncoded());
    }

    @Test
    void generatesAKeyWhenNoneIsConfigured() throws Exception {
        KeyPair generated = config.jwtKeyPair(properties(""), new DefaultResourceLoader());

        assertThat(generated.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    private static JwtProperties properties(String privateKeyLocation) {
        return new JwtProperties(privateKeyLocation, "sh-user-service", Duration.ofHours(1), Duration.ofDays(7));
    }
}
