package com.techup.course_flow_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techup.course_flow_server.config.OmiseProperties;
import java.math.BigDecimal;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal Omise client responsible for creating charges and parsing webhook
 * payloads. Keeping the gateway interaction in one place makes the rest of the
 * payment flow easier to test and swap later.
 */
@Service
public class PaymentGatewayService {

    private static final String DEFAULT_API_BASE_URL = "https://api.omise.co";
    private static final String DEFAULT_CURRENCY = "thb";

    private final OmiseProperties omiseProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public PaymentGatewayService(
            OmiseProperties omiseProperties,
            ObjectMapper objectMapper) {
        this.omiseProperties = omiseProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public ChargeResult createCardCharge(
            BigDecimal amount,
            String token,
            String description,
            String returnUri) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Payment token is required");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("amount", String.valueOf(toMinorUnits(amount)));
        body.add("currency", getCurrency());
        body.add("card", token.trim());
        body.add("description", description);
        body.add("return_uri", returnUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(getSecretKey(), "");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    getApiBaseUrl() + "/charges",
                    request,
                    String.class);
            return parseChargeResponse(response.getBody());
        } catch (RestClientResponseException ex) {
            throw new IllegalArgumentException(buildChargeFailureMessage(ex), ex);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Failed to create card charge: unable to reach Omise", ex);
        }
    }

    public ChargeWebhookEvent parseChargeWebhook(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode charge = resolveChargeNodeFromWebhook(root);

            String eventKey = textValue(root, "key");
            String chargeId = textValue(charge, "id");
            String status = textValue(charge, "status");
            boolean paid = booleanValue(charge, "paid");
            boolean authorized = booleanValue(charge, "authorized");
            String currency = textValue(charge, "currency");
            String failureCode = textValue(charge, "failure_code");
            String failureMessage = textValue(charge, "failure_message");

            if (chargeId == null || chargeId.isBlank()) {
                throw new IllegalArgumentException("Webhook payload does not contain a charge id");
            }

            return new ChargeWebhookEvent(
                    eventKey,
                    chargeId,
                    status,
                    paid,
                    authorized,
                    currency,
                    failureCode,
                    failureMessage);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Webhook payload is invalid", ex);
        }
    }

    /**
     * Omise ส่ง webhook เป็น event: {@code { "object":"event", "key":"charge.create", "data": { ...charge } }}.
     * ใน {@code data} ฟิลด์ {@code object} เป็นสตริง {@code "charge"} (type discriminator) — charge จริงคือทั้ง object {@code data}
     * ไม่ใช่ node ย่อยใต้ {@code data.object}. โค้ดเดิมที่ใช้ {@code data.path("object")} จึงได้แค่ string แล้วหา {@code id} ไม่เจอ → 400
     */
    private JsonNode resolveChargeNodeFromWebhook(JsonNode root) {
        if ("charge".equals(textValue(root, "object"))) {
            return root;
        }
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull() || !data.isObject()) {
            return data;
        }
        if ("charge".equals(textValue(data, "object"))) {
            return data;
        }
        JsonNode nested = data.path("object");
        if (nested.isObject() && nested.has("id")) {
            return nested;
        }
        return data;
    }

    private ChargeResult parseChargeResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            long amountMinorUnits = root.path("amount").asLong();
            return new ChargeResult(
                    textValue(root, "id"),
                    textValue(root, "status"),
                    booleanValue(root, "paid"),
                    booleanValue(root, "authorized"),
                    textValue(root, "currency"),
                    textValue(root, "authorize_uri"),
                    textValue(root, "return_uri"),
                    root.path("failure_code").asText(null),
                    root.path("failure_message").asText(null),
                    BigDecimal.valueOf(amountMinorUnits, 2));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Charge response is invalid");
        }
    }

    private String getApiBaseUrl() {
        String configured = omiseProperties.apiBaseUrl();
        return configured == null || configured.isBlank() ? DEFAULT_API_BASE_URL : configured.trim();
    }

    private String getCurrency() {
        String configured = omiseProperties.currency();
        return configured == null || configured.isBlank() ? DEFAULT_CURRENCY : configured.trim().toLowerCase();
    }

    private String getSecretKey() {
        String secretKey = omiseProperties.secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Omise secret key is not configured");
        }
        return secretKey.trim();
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String buildChargeFailureMessage(RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                String message = textValue(root, "message");
                if (message == null || message.isBlank()) {
                    message = textValue(root, "code");
                }
                if (message != null && !message.isBlank()) {
                    return "Failed to create card charge: " + message;
                }
            } catch (Exception ignored) {
                // Fall through to status-based message when the response is not JSON.
            }
        }
        return "Failed to create card charge: Omise returned HTTP " + ex.getStatusCode().value();
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }

    private boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        return !child.isMissingNode() && !child.isNull() && child.asBoolean();
    }

    public record ChargeResult(
            String chargeId,
            String status,
            boolean paid,
            boolean authorized,
            String currency,
            String authorizeUri,
            String returnUri,
            String failureCode,
            String failureMessage,
            BigDecimal amount
    ) {
    }

    public record ChargeWebhookEvent(
            String eventKey,
            String chargeId,
            String status,
            boolean paid,
            boolean authorized,
            String currency,
            String failureCode,
            String failureMessage
    ) {
    }
}
