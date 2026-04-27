package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKeySpec secretKeySpec;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        return generateToken(user.getEmail(), user.getRole().name());
    }

    public String generateToken(String email, String role) {
        try {
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plusMillis(expirationMs);

            Map<String, Object> header = Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", email);
            payload.put("role", role);
            payload.put("iat", issuedAt.getEpochSecond());
            payload.put("exp", expiresAt.getEpochSecond());

            String headerSegment = encodeJson(header);
            String payloadSegment = encodeJson(payload);
            String signingInput = headerSegment + "." + payloadSegment;
            String signatureSegment = sign(signingInput);
            String token = signingInput + "." + signatureSegment;
            logger.info("JWT generated for {} role={} exp={}", email, role, expiresAt);
            return token;
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de generer le JWT", exception);
        }
    }

    public String extractUsername(String token) {
        return parseToken(token).subject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        JwtClaims claims = parseToken(token);
        return userDetails.getUsername().equalsIgnoreCase(claims.subject()) && !claims.isExpired();
    }

    public JwtClaims parseToken(String token) {
        try {
            String[] segments = token.split("\\.");
            if (segments.length != 3) {
                throw new IllegalArgumentException("JWT mal forme");
            }

            String signingInput = segments[0] + "." + segments[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    segments[2].getBytes(StandardCharsets.UTF_8)
            )) {
                throw new IllegalArgumentException("Signature JWT invalide");
            }

            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(segments[1]),
                    MAP_TYPE
            );

            JwtClaims claims = new JwtClaims(
                    asString(payload.get("sub")),
                    asString(payload.get("role")),
                    Instant.ofEpochSecond(asLong(payload.get("iat"))),
                    Instant.ofEpochSecond(asLong(payload.get("exp")))
            );
            if (claims.isExpired()) {
                throw new IllegalArgumentException("JWT expire");
            }

            logger.info("JWT parsed for {} role={}", claims.subject(), claims.role());
            return claims;
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT invalide", exception);
        }
    }

    private String encodeJson(Map<String, Object> payload) throws Exception {
        return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String asString(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Claim JWT manquant");
        }
        return String.valueOf(value);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(asString(value));
    }

    public record JwtClaims(String subject, String role, Instant issuedAt, Instant expiresAt) {
        public boolean isExpired() {
            return expiresAt.isBefore(Instant.now());
        }
    }
}
