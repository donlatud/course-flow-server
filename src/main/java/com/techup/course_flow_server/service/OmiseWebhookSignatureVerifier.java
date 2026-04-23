package com.techup.course_flow_server.service;

import com.techup.course_flow_server.config.OmiseProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifies Omise webhook payloads using HMAC-SHA256 per
 * <a href="https://www.omise.co/api-webhooks">Omise Webhooks</a>.
 *
 * <p>สรุปขั้นตอนตามเอกสาร Omise: (1) ต่อสตริง {@code <timestamp>.<raw body UTF-8>}
 * (2) HMAC-SHA256 ด้วย key = Base64-decode ของ webhook secret จาก Dashboard
 * (3) เปรียบเทียบผลเป็น hex กับค่าใน header {@code Omise-Signature} แบบ constant-time
 */
@Component
public class OmiseWebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(OmiseWebhookSignatureVerifier.class);

    /** ลายเซ็น HMAC เป็น hex; ช่วงหมุน secret อาจมีหลายค่าคั่นด้วย comma */
    private static final String HEADER_SIGNATURE = "Omise-Signature";

    /** Unix epoch วินาที — เป็นส่วนหนึ่งของสตริงที่นำไป sign */
    private static final String HEADER_TIMESTAMP = "Omise-Signature-Timestamp";

    private final OmiseProperties omiseProperties;

    public OmiseWebhookSignatureVerifier(OmiseProperties omiseProperties) {
        this.omiseProperties = omiseProperties;
    }

    /**
     * @return {@code true} ถ้าให้ประมวลผล webhook ต่อได้ — ลายเซ็นถูกต้อง หรือยังไม่ได้ตั้ง secret (โหมด dev)
     */
    public boolean verify(HttpServletRequest request, byte[] rawBody) {
        String configured = omiseProperties.webhookSecret();

        // ไม่ตั้ง secret = ยัง verify ไม่ได้ — ยอมรับ request แต่เตือน (ห้ามใช้แบบนี้บน production)
        if (configured == null || configured.isBlank()) {
            log.warn(
                    "Omise webhook accepted without signature verification; set omise.webhook-secret "
                            + "(Dashboard Webhooks) before production");
            return true;
        }

        String signatureHeader = request.getHeader(HEADER_SIGNATURE);
        String timestampHeader = request.getHeader(HEADER_TIMESTAMP);
        // ตั้ง secret แล้วต้องมีทั้งสอง header ไม่งั้นถือว่าไม่ใช่ Omise หรือ request ไม่สมบูรณ์
        if (signatureHeader == null || signatureHeader.isBlank()
                || timestampHeader == null || timestampHeader.isBlank()) {
            return false;
        }

        String ts = timestampHeader.trim();
        // จำกัดช่วงเวลาเทียบกับเวลาเซิร์ฟเวอร์ — ลดความเสี่ยง replay (Omise แนะนำเป็น optional)
        if (!isTimestampWithinTolerance(ts, omiseProperties.effectiveWebhookTimestampToleranceSeconds())) {
            return false;
        }

        byte[] key;
        try {
            // ค่าใน Dashboard เป็น Base64 ของ raw HMAC key ต้อง decode ก่อนนำไป init Mac
            key = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException ex) {
            log.error("omise.webhook-secret is not valid Base64");
            return false;
        }

        return signaturesMatch(rawBody, ts, signatureHeader.trim(), key);
    }

    /**
     * เปรียบเทียบลายเซ็นจาก Omise กับ HMAC ที่คำนวณเอง — package-private เพื่อให้ unit test เรียกได้
     */
    static boolean signaturesMatch(byte[] rawBody, String timestampHeader, String signatureHeader, byte[] decodedSecret) {
        // body ต้องตรง byte-for-byte กับที่ Omise sign — ใช้ UTF-8 เหมือนตัวอย่างในเอกสาร Omise
        String bodyUtf8 = new String(rawBody, StandardCharsets.UTF_8);
        String signedPayload = timestampHeader + "." + bodyUtf8;
        byte[] expected;
        try {
            expected = hmacSha256(decodedSecret, signedPayload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            return false;
        }

        // ช่วงหมุน webhook secret ~24 ชม. Omise ส่ง signature สองค่าใน header เดียว — ยอมรับถ้าอันใดอันหนึ่งตรง
        String[] parts = signatureHeader.split(",");
        HexFormat hex = HexFormat.of();
        for (String part : parts) {
            String sig = part.trim();
            if (sig.isEmpty()) {
                continue;
            }
            byte[] provided;
            try {
                provided = hex.parseHex(sig);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            // constant-time compare กันถูกเดาลายเซ็นทีละตัวอักษรจากเวลาตอบ
            if (provided.length == expected.length && MessageDigest.isEqual(expected, provided)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] hmacSha256(byte[] secret, byte[] message)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(message);
    }

    /** true ถ้า timestamp (วินาที) อยู่ใกล้เวลาปัจจุบันไม่เกิน toleranceSeconds */
    private static boolean isTimestampWithinTolerance(String timestampHeader, int toleranceSeconds) {
        try {
            long ts = Long.parseLong(timestampHeader.trim());
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - ts) <= toleranceSeconds;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
