package kz.samat.smarthealth.userservice.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class JwtKeyConfig {

    private static final int GENERATED_KEY_SIZE = 2048;

    /** Signs access tokens. Its public half is published at /.well-known/jwks.json for the gateway. */
    @Bean
    public KeyPair jwtKeyPair(JwtProperties properties, ResourceLoader resourceLoader)
            throws GeneralSecurityException, IOException {
        if (!StringUtils.hasText(properties.privateKeyLocation())) {
            log.warn("jwt.private-key-location is not set, so a temporary signing key was generated. Access tokens"
                    + " from before a restart stop working once the gateway fetches the new key, and several"
                    + " instances cannot share the key.");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(GENERATED_KEY_SIZE);
            return generator.generateKeyPair();
        }
        return readPkcs8(resourceLoader.getResource(properties.privateKeyLocation()));
    }

    /** Reads a "BEGIN PRIVATE KEY" PEM file, as written by {@code openssl genpkey}, and derives the public key. */
    private static KeyPair readPkcs8(Resource pemFile) throws GeneralSecurityException, IOException {
        String base64 = pemFile.getContentAsString(StandardCharsets.US_ASCII)
                .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        return new KeyPair(
                keyFactory.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent())),
                privateKey);
    }
}
