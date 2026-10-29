package kz.samat.smarthealth.userservice.service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.RsaPublicJwk;
import kz.samat.smarthealth.userservice.config.JwtProperties;
import kz.samat.smarthealth.userservice.model.entity.User;
import org.springframework.stereotype.Service;

/**
 * Issues access tokens signed with RS256. Verifiers, such as sh-api-gateway, get the public key from
 * {@link #publicKeys()}, so they never hold anything that can sign a token.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final PrivateKey signingKey;
    private final RsaPublicJwk publicJwk;

    public JwtService(JwtProperties properties, KeyPair jwtKeyPair) {
        this.properties = properties;
        this.signingKey = jwtKeyPair.getPrivate();
        this.publicJwk = Jwks.builder()
                .key((RSAPublicKey) jwtKeyPair.getPublic())
                .algorithm(Jwts.SIG.RS256.getId())
                .publicKeyUse("sig")
                .idFromThumbprint()
                .build();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                // Tells verifiers which published key to check the signature with.
                .header().keyId(publicJwk.getId()).and()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(signingKey, Jwts.SIG.RS256)
                .compact();
    }

    /** The public key access tokens can be verified with, as a JSON Web Key Set. */
    public JwkSet publicKeys() {
        return Jwks.set().add(publicJwk).build();
    }
}
