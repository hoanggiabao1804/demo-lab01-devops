package com.yas.storefrontbff.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        ReactiveClientRegistrationRepository repository = Mockito.mock(ReactiveClientRegistrationRepository.class);
        securityConfig = new SecurityConfig(repository);
    }

    @Test
    void shouldMapRolesFromOidcUserAuthority() {
        OidcUserInfo userInfo = Mockito.mock(OidcUserInfo.class);
        Mockito.when(userInfo.hasClaim("realm_access")).thenReturn(true);
        Mockito.when(userInfo.getClaimAsMap("realm_access")).thenReturn(Map.of(
                "roles", List.of("ADMIN", "USER")));

        OidcUserAuthority authority = Mockito.mock(OidcUserAuthority.class);
        Mockito.when(authority.getUserInfo()).thenReturn(userInfo);

        var mapped = securityConfig.userAuthoritiesMapperForKeycloak()
                .mapAuthorities(Set.of(authority));

        assertEquals(2, mapped.size());
        assertTrue(mapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(mapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void shouldMapRolesFromOAuth2UserAuthority() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("MANAGER"));
        OAuth2UserAuthority authority = new OAuth2UserAuthority(
                Map.of("realm_access", realmAccess));

        var mapped = securityConfig.userAuthoritiesMapperForKeycloak()
                .mapAuthorities(Set.of(authority));

        assertEquals(1, mapped.size());
        assertTrue(mapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    void shouldGenerateAuthoritiesFromClaimList() {
        Collection<GrantedAuthority> mapped = securityConfig.generateAuthoritiesFromClaim(List.of("ADMIN", "GUEST"));

        assertEquals(2, mapped.size());
        assertTrue(mapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(mapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));
    }
}
