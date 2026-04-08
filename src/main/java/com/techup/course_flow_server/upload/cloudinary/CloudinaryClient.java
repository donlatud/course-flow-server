package com.techup.course_flow_server.upload.cloudinary;

import com.techup.course_flow_server.config.CloudinaryProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Server-side video upload to Cloudinary (signed upload, same folders as the SPA).
 *
 * @see <a href="https://cloudinary.com/documentation/upload_images#generating_authentication_signatures">Signed upload</a>
 */
@Component
public class CloudinaryClient {

    private final CloudinaryProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Pattern SECURE_URL_PATTERN =
            Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_MESSAGE_PATTERN =
            Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    public CloudinaryClient(CloudinaryProperties props) {
        this.props = props;
    }

    /**
     * Upload raw video bytes; returns {@code secure_url} from the JSON response.
     */
    public String uploadVideo(byte[] data, String folder, String filename, String contentType) {
        requireConfig();

        String cloud = props.cloudName().trim();
        String apiKey = props.apiKey().trim();
        String apiSecret = props.apiSecret().trim();

        long timestamp = Instant.now().getEpochSecond();
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("folder", folder.trim());
        signParams.put("timestamp", Long.toString(timestamp));
        String signature = apiSignRequest(signParams, apiSecret);

        String url = "https://api.cloudinary.com/v1_1/" + cloud + "/video/upload";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(
                "file",
                new ByteArrayResource(data) {
                    @Override
                    public String getFilename() {
                        return filename != null && !filename.isBlank() ? filename : "video.mp4";
                    }
                });
        body.add("api_key", apiKey);
        body.add("timestamp", Long.toString(timestamp));
        body.add("signature", signature);
        body.add("folder", folder.trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new CloudinaryUploadException(
                        "Cloudinary upload failed: HTTP " + response.getStatusCode());
            }
            String bodyStr = response.getBody();
            if (bodyStr.contains("\"error\"")) {
                Matcher errMatcher = ERROR_MESSAGE_PATTERN.matcher(bodyStr);
                String msg = errMatcher.find() ? errMatcher.group(1) : bodyStr;
                throw new CloudinaryUploadException("Cloudinary: " + msg);
            }
            Matcher secureUrlMatcher = SECURE_URL_PATTERN.matcher(bodyStr);
            if (!secureUrlMatcher.find()) {
                throw new CloudinaryUploadException("Cloudinary response missing secure_url");
            }
            return secureUrlMatcher.group(1);
        } catch (CloudinaryUploadException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CloudinaryUploadException("Cloudinary upload failed: " + ex.getMessage(), ex);
        }
    }

    /** Cloudinary {@code api_sign_request}: sorted params, join key=value with &, append secret, SHA1 hex. */
    static String apiSignRequest(Map<String, String> paramsToSign, String apiSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(paramsToSign);
        String stringToSign =
                sorted.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
        return sha1Hex(stringToSign + apiSecret);
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private void requireConfig() {
        if (props.cloudName() == null || props.cloudName().isBlank()) {
            throw new IllegalStateException("cloudinary.cloudName is not configured");
        }
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("cloudinary.apiKey is not configured");
        }
        if (props.apiSecret() == null || props.apiSecret().isBlank()) {
            throw new IllegalStateException("cloudinary.apiSecret is not configured");
        }
    }
}
