package com.yas.storefrontbff.viewmodel;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewModelTest {

    @Test
    void shouldCreateAuthenticatedUserVm() {
        AuthenticatedUserVm vm = new AuthenticatedUserVm("alice");

        assertEquals("alice", vm.username());
    }

    @Test
    void shouldCreateAuthenticationInfoVm() {
        AuthenticatedUserVm authenticatedUser = new AuthenticatedUserVm("bob");
        AuthenticationInfoVm vm = new AuthenticationInfoVm(true, authenticatedUser);

        assertAll(
                () -> assertEquals(true, vm.isAuthenticated()),
                () -> assertEquals(authenticatedUser, vm.authenticatedUser()));
    }

    @Test
    void shouldCreateCartDetailVm() {
        CartDetailVm vm = new CartDetailVm(1L, 2L, 5);

        assertAll(
                () -> assertEquals(1L, vm.id()),
                () -> assertEquals(2L, vm.productId()),
                () -> assertEquals(5, vm.quantity()));
    }

    @Test
    void shouldCreateCartGetDetailVm() {
        CartDetailVm detail = new CartDetailVm(1L, 2L, 3);
        CartGetDetailVm vm = new CartGetDetailVm(10L, "customer", List.of(detail));

        assertAll(
                () -> assertEquals(10L, vm.id()),
                () -> assertEquals("customer", vm.customerId()),
                () -> assertEquals(1, vm.cartDetails().size()),
                () -> assertEquals(detail, vm.cartDetails().get(0)));
    }

    @Test
    void shouldCreateGuestUserVm() {
        GuestUserVm vm = new GuestUserVm("guest123", "guest@example.com", "secret");

        assertAll(
                () -> assertEquals("guest123", vm.userId()),
                () -> assertEquals("guest@example.com", vm.email()),
                () -> assertEquals("secret", vm.password()));
    }

    @Test
    void shouldCreateTokenResponseVm() {
        TokenResponseVm vm = new TokenResponseVm("access", "refresh");

        assertAll(
                () -> assertEquals("access", vm.accessToken()),
                () -> assertEquals("refresh", vm.refreshToken()));
    }

    @Test
    void shouldConvertCartDetailToCartItemVm() {
        CartDetailVm detail = new CartDetailVm(12L, 34L, 2);
        CartItemVm item = CartItemVm.fromCartDetailVm(detail);

        assertAll(
                () -> assertEquals(34L, item.productId()),
                () -> assertEquals(2, item.quantity()));
    }
}
