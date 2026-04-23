package com.techup.course_flow_server.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * PgBouncer in transaction mode (e.g. Supabase pooler port 6543) breaks PostgreSQL JDBC when the
 * driver uses server-side named prepared statements. Symptoms include {@code prepared statement "S_n"
 * already exists}, {@code bind message supplies N parameters, but prepared statement "S_n" requires 0},
 * then broken rollbacks and 500s on {@code /learning}, {@code /me}, etc.
 * <p>
 * This runs before any DataSource exists and <strong>merges</strong> {@code prepareThreshold=0} and
 * {@code preferQueryMode=simple} into the JDBC URL (overwriting any prior values for those keys) so
 * env-only {@code SPRING_DATASOURCE_URL} without these flags still works.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PgPoolerJdbcUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROP = "spring.datasource.url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty(PROP);
        if (url == null || url.isBlank() || !isPostgresJdbcUrl(url)) {
            return;
        }
        if (!likelyNeedsPgPoolerWorkaround(url)) {
            return;
        }

        String merged = mergePoolerQueryParams(url.trim());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(PROP, merged);
        MutablePropertySources sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource("pgPoolerJdbcUrlFix", map));
    }

    static String mergePoolerQueryParams(String jdbcUrl) {
        int q = jdbcUrl.indexOf('?');
        String base = q >= 0 ? jdbcUrl.substring(0, q) : jdbcUrl;
        Map<String, String> params = new LinkedHashMap<>();
        if (q >= 0 && q + 1 < jdbcUrl.length()) {
            for (String part : jdbcUrl.substring(q + 1).split("&")) {
                if (part.isEmpty()) continue;
                int eq = part.indexOf('=');
                if (eq <= 0) continue;
                String key = decode(part.substring(0, eq));
                if ("prepareThreshold".equalsIgnoreCase(key) || "preferQueryMode".equalsIgnoreCase(key)) {
                    continue;
                }
                params.put(key, decode(part.substring(eq + 1)));
            }
        }
        params.put("prepareThreshold", "0");
        params.put("preferQueryMode", "simple");
        String tail =
                params.entrySet().stream()
                        .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                        .collect(Collectors.joining("&"));
        return base + "?" + tail;
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static boolean isPostgresJdbcUrl(String url) {
        String u = url.toLowerCase();
        return u.startsWith("jdbc:postgresql:") || u.startsWith("jdbc:postgres:");
    }

    private static boolean likelyNeedsPgPoolerWorkaround(String url) {
        String u = url.toLowerCase();
        return u.contains("pooler.supabase.com")
                || u.contains(":6543/")
                || u.contains(":6543?");
    }
}
