package com.techup.course_flow_server.config;

import com.techup.course_flow_server.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Maps each validated Supabase JWT to authorities from our {@code users} table so
 * {@code /api/admin/**} can require {@code ROLE_ADMIN} without trusting the token alone.
 */
@Component
public class JwtUserRoleAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public JwtUserRoleAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            userRepository.findById(userId).ifPresent(user -> authorities.add(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        } catch (IllegalArgumentException ignored) {
            // invalid sub — no application roles
        }
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
