package com.yas.backofficebff.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(mock(ReactiveClientRegistrationRepository.class));

    @Test
    void should_map_roles_to_granted_authorities() {
        Collection<String> roles = List.of("ADMIN", "MANAGER");

        Collection<GrantedAuthority> authorities = securityConfig.generateAuthoritiesFromClaim(roles);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER");
    }

    @Test
    void should_map_oauth2_user_authority_claims_to_roles() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("ADMIN", "USER"));
        Map<String, Object> attributes = Map.of("realm_access", realmAccess);
        OAuth2UserAuthority oauth2UserAuthority = new OAuth2UserAuthority(attributes);

        GrantedAuthoritiesMapper mapper = securityConfig.userAuthoritiesMapperForKeycloak();
        Collection<? extends GrantedAuthority> mappedAuthorities = mapper.mapAuthorities(Set.of(oauth2UserAuthority));

        assertThat(mappedAuthorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void should_return_empty_authorities_when_claim_missing() {
        Map<String, Object> attributes = Map.of("preferred_username", "john.doe");
        OAuth2UserAuthority oauth2UserAuthority = new OAuth2UserAuthority(attributes);

        GrantedAuthoritiesMapper mapper = securityConfig.userAuthoritiesMapperForKeycloak();
        Collection<? extends GrantedAuthority> mappedAuthorities = mapper.mapAuthorities(Set.of(oauth2UserAuthority));

        assertThat(mappedAuthorities).isEmpty();
    }
}
