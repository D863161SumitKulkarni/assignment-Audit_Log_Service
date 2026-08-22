import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

// Demo-only helper: mints a long-lived HS256 JWT signed with the shared demo secret.
// Never use this pattern to issue tokens in production - real tokens must come from an OIDC provider.
class GenerateDemoToken {

    public static void main(String[] args) throws Exception {
        String secret = args.length > 0 ? args[0] : "demo-only-shared-secret-key-please-rotate-32bytes-min";
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = Instant.parse("2099-01-01T00:00:00Z").getEpochSecond();

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"sub\":\"demo-admin\",\"iss\":\"http://localhost:8081/realms/audit\"," +
                        "\"aud\":\"audit-log-service\",\"roles\":[\"ADMIN\",\"AUDITOR\"]," +
                        "\"iat\":%d,\"exp\":%d}",
                issuedAt, expiresAt);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String headerPart = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String payloadPart = encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerPart + "." + payloadPart;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signaturePart = encoder.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));

        System.out.println(signingInput + "." + signaturePart);
    }
}
