package com.techup.course_flow_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Omise gateway configuration used by the backend payment flow.
 *
 * Only the secret key is required for server-side charge creation.
 * Public key usage stays on the frontend through Omise.js.
 *
 * <p>Webhook signing: set {@code webhookSecret} (Base64, from Omise Dashboard) so
 * {@code POST /api/webhooks/omise} can verify {@code Omise-Signature} headers.
 */
@ConfigurationProperties(prefix = "omise")
public record OmiseProperties(
        String secretKey,
        String apiBaseUrl,
        String currency,
        /** Webhook signing secret จาก Omise Dashboard (รูปแบบ Base64) — คนละค่ากับ secret API key สำหรับ charge */
        String webhookSecret,
        /**
         * ช่วงยอมรับความต่างของ {@code Omise-Signature-Timestamp} กับเวลาเซิร์ฟเวอร์ (วินาที)
         * สำหรับกัน replay; ถ้า null หรือ ≤0 ใช้ค่า default 300 วินาทีใน {@link #effectiveWebhookTimestampToleranceSeconds()}
         */
        Integer webhookTimestampToleranceSeconds
) {

    /** ค่า tolerance ที่ใช้จริงเมื่อ verify webhook */
    public int effectiveWebhookTimestampToleranceSeconds() {
        if (webhookTimestampToleranceSeconds != null && webhookTimestampToleranceSeconds > 0) {
            return webhookTimestampToleranceSeconds;
        }
        return 300;
    }
}
