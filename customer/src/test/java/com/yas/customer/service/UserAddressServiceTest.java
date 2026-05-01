package com.yas.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserAddressService userAddressService;

    @Captor
    private ArgumentCaptor<List<UserAddress>> userAddressListCaptor;

    private final String VALID_USER_ID = "user-123";
    private final String ANONYMOUS_USER = "anonymousUser";

    @BeforeEach
    void setUp() {
        // Có thể setup thêm biến dùng chung tại đây
    }

    @AfterEach
    void tearDown() {
        // Clear SecurityContext sau mỗi test để tránh ảnh hưởng chéo
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String userId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userId);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class GetUserAddressList {
        @Test
        void shouldThrowAccessDeniedException_WhenUserIsAnonymous() {
            mockSecurityContext(ANONYMOUS_USER);

            assertThatThrownBy(() -> userAddressService.getUserAddressList())
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldReturnSortedAddressList_WhenUserIsAuthenticated() {
            mockSecurityContext(VALID_USER_ID);

            // Mock UserAddress từ Database
            UserAddress inactiveAddress = UserAddress.builder().addressId(1L).userId(VALID_USER_ID).isActive(false)
                    .build();
            UserAddress activeAddress = UserAddress.builder().addressId(2L).userId(VALID_USER_ID).isActive(true)
                    .build();
            when(userAddressRepository.findAllByUserId(VALID_USER_ID))
                    .thenReturn(List.of(inactiveAddress, activeAddress));

            // Mock AddressDetailVm từ LocationService
            AddressDetailVm address1 = new AddressDetailVm(1L, "Name1", "Phone1", "Line1", "City1", "Zip1", 1L, "Dist1",
                    1L, "State1", 1L, "Country1");
            AddressDetailVm address2 = new AddressDetailVm(2L, "Name2", "Phone2", "Line2", "City2", "Zip2", 2L, "Dist2",
                    2L, "State2", 2L, "Country2");
            when(locationService.getAddressesByIdList(List.of(1L, 2L))).thenReturn(List.of(address1, address2));

            // Call Service
            List<ActiveAddressVm> result = userAddressService.getUserAddressList();

            // Assert
            assertThat(result).hasSize(2);
            // Verify sorting: active address (id 2) should be first
            assertThat(result.get(0).id()).isEqualTo(2L);
            assertThat(result.get(0).isActive()).isTrue();
            assertThat(result.get(1).id()).isEqualTo(1L);
            assertThat(result.get(1).isActive()).isFalse();
        }
    }

    @Nested
    class GetAddressDefault {
        @Test
        void shouldThrowAccessDeniedException_WhenUserIsAnonymous() {
            mockSecurityContext(ANONYMOUS_USER);

            assertThatThrownBy(() -> userAddressService.getAddressDefault())
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldThrowNotFoundException_WhenDefaultAddressNotFound() {
            mockSecurityContext(VALID_USER_ID);
            when(userAddressRepository.findByUserIdAndIsActiveTrue(VALID_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAddressService.getAddressDefault())
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void shouldReturnDefaultAddress_WhenFound() {
            mockSecurityContext(VALID_USER_ID);
            UserAddress userAddress = UserAddress.builder().addressId(99L).userId(VALID_USER_ID).isActive(true).build();
            when(userAddressRepository.findByUserIdAndIsActiveTrue(VALID_USER_ID)).thenReturn(Optional.of(userAddress));

            AddressDetailVm expectedAddress = new AddressDetailVm(99L, "Name", "Phone", "Line", "City", "Zip", 1L,
                    "Dist", 1L, "State", 1L, "Country");
            when(locationService.getAddressById(99L)).thenReturn(expectedAddress);

            AddressDetailVm result = userAddressService.getAddressDefault();

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(99L);
        }
    }

    @Nested
    class CreateAddress {
        @Test
        void shouldCreateFirstAddressAsActive() {
            mockSecurityContext(VALID_USER_ID);

            // Giả lập user chưa có địa chỉ nào
            when(userAddressRepository.findAllByUserId(VALID_USER_ID)).thenReturn(Collections.emptyList());

            AddressPostVm postVm = new AddressPostVm("Name", "Phone", "Line", "City", "Zip", 1L, 1L, 1L);
            AddressVm savedAddressVm = new AddressVm(10L, "Name", "Phone", "Line", "City", "Zip", 1L, 1L, 1L);

            when(locationService.createAddress(postVm)).thenReturn(savedAddressVm);
            when(userAddressRepository.save(any(UserAddress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserAddressVm result = userAddressService.createAddress(postVm);

            // Kiểm tra địa chỉ đầu tiên tạo ra phải là địa chỉ mặc định (isActive = true)
            assertThat(result.isActive()).isTrue();
            assertThat(result.addressGetVm().id()).isEqualTo(10L);
            verify(userAddressRepository).save(any(UserAddress.class));
        }

        @Test
        void shouldCreateSubsequentAddressAsInactive() {
            mockSecurityContext(VALID_USER_ID);

            // Giả lập user ĐÃ có địa chỉ
            UserAddress existingAddress = UserAddress.builder().addressId(1L).build();
            when(userAddressRepository.findAllByUserId(VALID_USER_ID)).thenReturn(List.of(existingAddress));

            AddressPostVm postVm = new AddressPostVm("Name2", "Phone2", "Line2", "City2", "Zip2", 2L, 2L, 2L);
            AddressVm savedAddressVm = new AddressVm(20L, "Name2", "Phone2", "Line2", "City2", "Zip2", 2L, 2L, 2L);

            when(locationService.createAddress(postVm)).thenReturn(savedAddressVm);
            when(userAddressRepository.save(any(UserAddress.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserAddressVm result = userAddressService.createAddress(postVm);

            // Kiểm tra địa chỉ sau tạo ra không được làm mặc định (isActive = false)
            assertThat(result.isActive()).isFalse();
            assertThat(result.addressGetVm().id()).isEqualTo(20L);
        }
    }

    @Nested
    class DeleteAddress {
        @Test
        void shouldThrowNotFoundException_WhenAddressDoesNotExist() {
            mockSecurityContext(VALID_USER_ID);
            Long addressId = 1L;
            when(userAddressRepository.findOneByUserIdAndAddressId(VALID_USER_ID, addressId)).thenReturn(null);

            assertThatThrownBy(() -> userAddressService.deleteAddress(addressId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void shouldDeleteAddress_WhenFound() {
            mockSecurityContext(VALID_USER_ID);
            Long addressId = 1L;
            UserAddress userAddress = UserAddress.builder().addressId(addressId).userId(VALID_USER_ID).build();
            when(userAddressRepository.findOneByUserIdAndAddressId(VALID_USER_ID, addressId)).thenReturn(userAddress);

            userAddressService.deleteAddress(addressId);

            verify(userAddressRepository).delete(userAddress);
        }
    }

    @Nested
    class ChooseDefaultAddress {
        @Test
        void shouldUpdateAllAddressesToSetCorrectActiveAddress() {
            mockSecurityContext(VALID_USER_ID);
            Long newDefaultAddressId = 2L;

            UserAddress address1 = UserAddress.builder().addressId(1L).userId(VALID_USER_ID).isActive(true).build();
            UserAddress address2 = UserAddress.builder().addressId(2L).userId(VALID_USER_ID).isActive(false).build();
            UserAddress address3 = UserAddress.builder().addressId(3L).userId(VALID_USER_ID).isActive(false).build();

            List<UserAddress> currentList = List.of(address1, address2, address3);
            when(userAddressRepository.findAllByUserId(VALID_USER_ID)).thenReturn(currentList);

            userAddressService.chooseDefaultAddress(newDefaultAddressId);

            verify(userAddressRepository).saveAll(userAddressListCaptor.capture());
            List<UserAddress> savedList = userAddressListCaptor.getValue();

            // Assert states changed correctly
            assertThat(savedList).hasSize(3);
            assertThat(savedList.stream().filter(UserAddress::getIsActive).findFirst().get().getAddressId())
                    .isEqualTo(newDefaultAddressId); // Chỉ có address 2 là true

            // Check specific objects
            assertThat(savedList.get(0).getIsActive()).isFalse(); // Address 1 chuyển thành false
            assertThat(savedList.get(1).getIsActive()).isTrue(); // Address 2 chuyển thành true
            assertThat(savedList.get(2).getIsActive()).isFalse(); // Address 3 giữ nguyên false
        }
    }
}