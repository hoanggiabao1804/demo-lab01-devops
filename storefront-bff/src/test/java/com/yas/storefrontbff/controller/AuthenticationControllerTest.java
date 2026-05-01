package com.yas.storefrontbff.controller;

import com.yas.storefrontbff.viewmodel.AuthenticatedUserVm;
import com.yas.storefrontbff.viewmodel.AuthenticationInfoVm;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationControllerTest {

    private final AuthenticationController controller = new AuthenticationController();

    @Test
    void shouldReturnUnauthenticatedWhenPrincipalIsNull() {
        AuthenticationInfoVm result = controller.user(null).getBody();

        assertFalse(result.isAuthenticated());
        assertNull(result.authenticatedUser());
    }

    @Test
    void shouldReturnAuthenticatedWhenPrincipalHasPreferredUsername() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("preferred_username")).thenReturn("alice");

        AuthenticationInfoVm result = controller.user(principal).getBody();

        assertTrue(result.isAuthenticated());
        assertEquals(new AuthenticatedUserVm("alice"), result.authenticatedUser());
    }
}
