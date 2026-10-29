package kz.samat.smarthealth.apigateway;

import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Builds access tokens the way sh-user-service does, with any claims a test needs. */
public final class TestTokens {

    /** Stands in for sh-user-service's signing key. {@link UpstreamStub} publishes its public half. */
    public static final RSAKey SIGNING_KEY = generateKey("test-key");

    private TestTokens() {
    }

    public static String accessToken(String issuer, String subject, Instant expiresAt) {
        return accessToken(SIGNING_KEY, issuer, subject, expiresAt);
    }

    public static String accessToken(RSAKey key, String issuer, String subject, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }

    /** The JSON Web Key Set sh-user-service would publish for {@link #SIGNING_KEY}. */
    public static String publishedKeys() {
        return new JWKSet(SIGNING_KEY.toPublicJWK()).toString();
    }

    public static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
