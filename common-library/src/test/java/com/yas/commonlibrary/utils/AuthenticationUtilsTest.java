package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yas.commonlibrary.constants.ApiConstant;
import com.yas.commonlibrary.exception.AccessDeniedException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserIdAndJwt_whenJwtAuthenticationExists_returnSubjectAndTokenValue() {
        Jwt jwt = Jwt.withTokenValue("jwt-token")
            .header("alg", "none")
            .subject("user-123")
            .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertSame(authentication, AuthenticationUtils.getAuthentication());
        assertEquals("user-123", AuthenticationUtils.extractUserId());
        assertEquals("jwt-token", AuthenticationUtils.extractJwt());
    }

    @Test
    void extractUserId_whenAnonymousAuthentication_throwsAccessDeniedException() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);

        assertEquals(ApiConstant.ACCESS_DENIED, exception.getMessage());
    }
}
