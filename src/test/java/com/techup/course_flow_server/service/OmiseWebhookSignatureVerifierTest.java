package com.techup.course_flow_server.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.techup.course_flow_server.config.OmiseProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

/** ทดสอบการคำนวณลายเซ็นตาม Omise และพฤติกรรมเมื่อตั้ง/ไม่ตั้ง webhook secret */
class OmiseWebhookSignatureVerifierTest {

    @Test
    void signaturesMatch_acceptsValidHexSignature() throws Exception {
        byte[] key = "unit-test-key".getBytes(StandardCharsets.UTF_8);
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String body = "{\"object\":\"event\"}";
        String signedPayload = ts + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        String hexSig = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

        assertTrue(OmiseWebhookSignatureVerifier.signaturesMatch(
                body.getBytes(StandardCharsets.UTF_8), ts, hexSig, key));
    }

    @Test
    void signaturesMatch_rejectsWrongSignature() throws Exception {
        byte[] key = "unit-test-key".getBytes(StandardCharsets.UTF_8);
        String ts = "1700000000";
        String body = "{}";
        assertFalse(OmiseWebhookSignatureVerifier.signaturesMatch(
                body.getBytes(StandardCharsets.UTF_8), ts, "ab".repeat(32), key));
    }

    @Test
    void signaturesMatch_acceptsOneOfCommaSeparatedSignatures() throws Exception {
        byte[] key = "unit-test-key".getBytes(StandardCharsets.UTF_8);
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String body = "{\"dual\":true}";
        String signedPayload = ts + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        String good = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        String bad = "00".repeat(32);

        assertTrue(OmiseWebhookSignatureVerifier.signaturesMatch(
                body.getBytes(StandardCharsets.UTF_8), ts, bad + "," + good, key));
    }

    @Test
    void verify_skipsWhenWebhookSecretNotConfigured() {
        OmiseProperties props = new OmiseProperties("sk_test", "https://api.omise.co", "thb", null, null);
        OmiseWebhookSignatureVerifier verifier = new OmiseWebhookSignatureVerifier(props);
        HttpServletRequest req = mock(HttpServletRequest.class);
        assertTrue(verifier.verify(req, "{}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void verify_rejectsWhenSecretConfiguredButHeadersMissing() {
        String b64 = Base64.getEncoder().encodeToString("key".getBytes(StandardCharsets.UTF_8));
        OmiseProperties props = new OmiseProperties("sk_test", "https://api.omise.co", "thb", b64, 300);
        OmiseWebhookSignatureVerifier verifier = new OmiseWebhookSignatureVerifier(props);
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Omise-Signature")).thenReturn(null);
        when(req.getHeader("Omise-Signature-Timestamp")).thenReturn(null);
        assertFalse(verifier.verify(req, "{}".getBytes(StandardCharsets.UTF_8)));
    }
}
