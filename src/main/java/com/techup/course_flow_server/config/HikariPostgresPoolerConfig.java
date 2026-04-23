package com.techup.course_flow_server.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supabase transaction pooler (PgBouncer, port 6543) is incompatible with PostgreSQL JDBC
 * server-side prepared statements — Hibernate then fails with
 * {@code prepared statement "S_n" already exists} on commit (500 on {@code /learning}, {@code /me}, etc.).
 * <p>
 * Spring Boot 4 no longer exposes {@code HikariConfigCustomizer} in the old package; we apply the same
 * driver flags on the {@link HikariDataSource} bean before it initializes the pool.
 */
@Configuration(proxyBeanMethods = false)
public class HikariPostgresPoolerConfig {

    @Bean
    public static BeanPostProcessor hikariPostgresPoolerBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof HikariDataSource hds)) {
                    return bean;
                }
                String jdbcUrl = hds.getJdbcUrl();
                if (jdbcUrl != null && jdbcUrl.contains("postgresql")) {
                    hds.addDataSourceProperty("prepareThreshold", "0");
                    hds.addDataSourceProperty("preferQueryMode", "simple");
                }
                return bean;
            }
        };
    }
}
