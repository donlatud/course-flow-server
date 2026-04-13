package com.techup.course_flow_server.util;

import com.techup.course_flow_server.entity.Material;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Cloudinary "player" embed links ({@code player.cloudinary.com/embed/...}) are HTML players, not
 * byte streams for {@code <video src>}. Rewrites them to a {@code res.cloudinary.com/.../video/upload/...mp4}
 * delivery URL when query contains {@code cloud_name} and {@code public_id}.
 */
public final class VideoDeliveryUrlNormalizer {

    private VideoDeliveryUrlNormalizer() {}

    public static String normalizeForVideoPlayer(String fileUrl, Material.FileType fileType) {
        if (fileUrl == null || fileUrl.isBlank() || fileType != Material.FileType.VIDEO) {
            return fileUrl;
        }
        String trimmed = fileUrl.trim();
        if (!trimmed.contains("player.cloudinary.com")) {
            return fileUrl;
        }
        return cloudinaryPlayerEmbedToDeliveryMp4(trimmed);
    }

    static String cloudinaryPlayerEmbedToDeliveryMp4(String embedUrl) {
        try {
            URI u = URI.create(embedUrl);
            String host = u.getHost();
            if (host == null || !host.endsWith("player.cloudinary.com")) {
                return embedUrl;
            }
            String rawQuery = u.getRawQuery();
            if (rawQuery == null || rawQuery.isBlank()) {
                return embedUrl;
            }
            String cloudName = null;
            String publicId = null;
            for (String part : rawQuery.split("&")) {
                int eq = part.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = urlDecode(part.substring(0, eq));
                String val = urlDecode(part.substring(eq + 1));
                if ("cloud_name".equals(key)) {
                    cloudName = val;
                } else if ("public_id".equals(key)) {
                    publicId = val;
                }
            }
            if (cloudName == null || cloudName.isBlank() || publicId == null || publicId.isBlank()) {
                return embedUrl;
            }
            String suffix = publicId.endsWith(".mp4") ? "" : ".mp4";
            return "https://res.cloudinary.com/" + cloudName + "/video/upload/" + publicId + suffix;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return embedUrl;
        }
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
