package com.techup.course_flow_server.upload.supabase;

import com.techup.course_flow_server.config.SupabaseProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

/**
 * Server-side uploads to Supabase Storage via the REST API (same buckets as the SPA).
 *
 * @see <a href="https://supabase.com/docs/reference/api/v1/upload-an-object">Storage upload</a>
 */
@Component
public class SupabaseStorageClient {

    private final SupabaseProperties supabaseProperties;
    private final RestClient restClient;

    public SupabaseStorageClient(SupabaseProperties supabaseProperties) {
        this.supabaseProperties = supabaseProperties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Upload bytes to a bucket at the given object path (folders separated by {@code /}).
     *
     * @return Public URL for the object (bucket must be public, same as JS client)
     */
    public String upload(
            String bucket,
            String objectPath,
            byte[] data,
            String contentType) {
        String key = requireServiceKey();
        String base = normalizeBaseUrl(supabaseProperties.url());
        String pathEncoded = encodeObjectPath(objectPath);
        String url =
                base
                        + "/storage/v1/object/"
                        + UriUtils.encodePathSegment(bucket, StandardCharsets.UTF_8)
                        + "/"
                        + pathEncoded;

        String ct =
                (contentType != null && !contentType.isBlank())
                        ? contentType
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            restClient
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .header("apikey", key)
                    .header(HttpHeaders.CONTENT_TYPE, ct)
                    .header("x-upsert", "true")
                    .body(data)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            (req, res) -> {
                                throw new SupabaseStorageException(
                                        "Supabase upload failed: "
                                                + res.getStatusCode()
                                                + " "
                                                + res.getStatusText()
                                                + " — "
                                                + readErrorBodySafely(res));
                            })
                    .toBodilessEntity();
        } catch (SupabaseStorageException e) {
            throw e;
        } catch (RestClientException e) {
            throw new SupabaseStorageException(
                    "Supabase upload failed (network or client error): " + e.getMessage(), e);
        }

        return getPublicUrl(bucket, objectPath);
    }

    /** Avoid NPE / IO failures when mapping Storage errors (prevents generic HTTP 500). */
    private static String readErrorBodySafely(ClientHttpResponse res) {
        try {
            InputStream in = res.getBody();
            if (in == null) {
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "(could not read error body: " + e.getMessage() + ")";
        }
    }

    /**
     * Public URL for an object in a public bucket (matches {@code getPublicUrl} in supabase-js).
     */
    public String getPublicUrl(String bucket, String objectPath) {
        String base = normalizeBaseUrl(supabaseProperties.url());
        return base
                + "/storage/v1/object/public/"
                + UriUtils.encodePathSegment(bucket, StandardCharsets.UTF_8)
                + "/"
                + encodeObjectPath(objectPath);
    }

    private String requireServiceKey() {
        String key = supabaseProperties.serviceKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "supabase.serviceKey is not configured (required for server-side Storage uploads). "
                            + "Set environment variable SUPABASE_SERVICE_ROLE_KEY or SUPABASE_SERVICE_KEY, "
                            + "or add supabase.serviceKey to optional application-local.properties "
                            + "using the service_role secret from Supabase Dashboard → Project Settings → API "
                            + "(not the anon key). See application-local.properties.example.");
        }
        return key.trim();
    }

    static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("supabase.url is not configured");
        }
        String t = url.trim();
        if (t.endsWith("/")) {
            return t.substring(0, t.length() - 1);
        }
        return t;
    }

    /** Encode each path segment; keep {@code /} as separator (RFC 3986 path). */
    static String encodeObjectPath(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("objectPath is required");
        }
        return Arrays.stream(objectPath.split("/"))
                .map(s -> UriUtils.encodePathSegment(s, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }
}
