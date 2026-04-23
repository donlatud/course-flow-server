package com.techup.course_flow_server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techup.course_flow_server.config.OmiseProperties;
import org.junit.jupiter.api.Test;

/** ตรวจว่า parse webhook รองรับ event envelope ของ Omise (data = charge object เต็ม) */
class PaymentGatewayServiceWebhookParseTest {

    @Test
    void parseChargeWebhook_readsChargeFromEventDataObject() {
        OmiseProperties props = new OmiseProperties("sk", "https://api.omise.co", "thb", null, null);
        PaymentGatewayService svc = new PaymentGatewayService(props, new ObjectMapper());

        String payload =
                """
                {
                  "object": "event",
                  "key": "charge.create",
                  "data": {
                    "object": "charge",
                    "id": "chrg_test_67alj8i4a74i5lyj9o3",
                    "currency": "THB",
                    "status": "successful",
                    "paid": true,
                    "authorized": true,
                    "failure_code": null,
                    "failure_message": null
                  }
                }
                """;

        PaymentGatewayService.ChargeWebhookEvent ev = svc.parseChargeWebhook(payload);
        assertEquals("charge.create", ev.eventKey());
        assertEquals("chrg_test_67alj8i4a74i5lyj9o3", ev.chargeId());
        assertEquals("THB", ev.currency());
        assertEquals("successful", ev.status());
        assertTrue(ev.paid());
        assertTrue(ev.authorized());
    }

    @Test
    void parseChargeWebhook_readsChargeAtRoot() {
        OmiseProperties props = new OmiseProperties("sk", "https://api.omise.co", "thb", null, null);
        PaymentGatewayService svc = new PaymentGatewayService(props, new ObjectMapper());

        String payload =
                """
                {
                  "object": "charge",
                  "id": "chrg_test_root",
                  "currency": "thb",
                  "status": "pending",
                  "paid": false,
                  "authorized": false
                }
                """;

        PaymentGatewayService.ChargeWebhookEvent ev = svc.parseChargeWebhook(payload);
        assertEquals("chrg_test_root", ev.chargeId());
        assertEquals("thb", ev.currency());
    }
}
