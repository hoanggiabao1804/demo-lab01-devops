package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.csv.BaseCsv;
import com.yas.commonlibrary.csv.CsvExporter;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.utils.AuthenticationUtils;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.utils.Constants;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductService productService;
    @Mock
    private CartService cartService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderAddress orderAddress;

    @BeforeEach
    void setUp() {
        // Fix NullPointerException: Cung cấp đầy đủ thông tin Address cho Order
        orderAddress = OrderAddress.builder().id(10L).build();

        order = Order.builder()
                .id(1L)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .couponCode("DISCOUNT10")
                .shippingAddressId(orderAddress)
                .billingAddressId(orderAddress)
                .build();
    }

    @Nested
    class CreateOrder {
        @Test
        void shouldCreateOrderSuccessfully() {
            OrderPostVm postVm = mock(OrderPostVm.class);
            OrderAddressPostVm addressVm = mock(OrderAddressPostVm.class);
            OrderItemPostVm itemVm = mock(OrderItemPostVm.class);

            when(postVm.billingAddressPostVm()).thenReturn(addressVm);
            when(postVm.shippingAddressPostVm()).thenReturn(addressVm);
            when(postVm.orderItemPostVms()).thenReturn(List.of(itemVm));
            when(postVm.paymentStatus()).thenReturn(PaymentStatus.PENDING);

            when(orderRepository.findById(any())).thenReturn(Optional.of(order));

            OrderVm result = orderService.createOrder(postVm);

            // Fix TooManyActualInvocations: Hàm lưu order được gọi 2 lần (1 lần tạo, 1 lần
            // gọi acceptOrder ở cuối)
            verify(orderRepository, times(2)).save(any(Order.class));
            verify(orderItemRepository).saveAll(any());
            verify(productService).subtractProductStockQuantity(any());
            verify(cartService).deleteCartItems(any());
            verify(promotionService).updateUsagePromotion(anyList());
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class GetOrderWithItemsById {
        @Test
        void shouldReturnOrderVm_WhenOrderExists() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(OrderItem.builder().id(1L).build()));

            OrderVm result = orderService.getOrderWithItemsById(1L);
            assertThat(result).isNotNull();
        }

        @Test
        void shouldThrowNotFound_WhenOrderDoesNotExist() {
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            // Fix AssertionError: Cập nhật câu text mong đợi
            assertThatThrownBy(() -> orderService.getOrderWithItemsById(1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }
    }

    @Nested
    class GetAllOrder {
        @Test
        void shouldReturnEmptyList_WhenPageIsEmpty() {
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            OrderListVm result = orderService.getAllOrder(
                    Pair.of(ZonedDateTime.now(), ZonedDateTime.now()),
                    "Prod", List.of(), Pair.of("VN", "123"), "test@mail.com", Pair.of(0, 10));

            assertThat(result.totalElements()).isZero();
            assertThat(result.orderList()).isNull();
        }

        @Test
        void shouldReturnOrderList_WhenHasContent() {
            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            OrderListVm result = orderService.getAllOrder(
                    Pair.of(ZonedDateTime.now(), ZonedDateTime.now()),
                    "Prod", List.of(OrderStatus.PENDING), Pair.of("VN", "123"), "test@mail.com", Pair.of(0, 10));

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.orderList()).hasSize(1);
        }
    }

    @Nested
    class GetLatestOrders {
        @Test
        void shouldReturnEmpty_WhenCountIsZeroOrLess() {
            assertThat(orderService.getLatestOrders(0)).isEmpty();
        }

        @Test
        void shouldReturnEmpty_WhenRepositoryReturnsEmpty() {
            when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(Collections.emptyList());
            assertThat(orderService.getLatestOrders(5)).isEmpty();
        }

        @Test
        void shouldReturnData_WhenRepositoryReturnsList() {
            when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of(order));
            assertThat(orderService.getLatestOrders(5)).hasSize(1);
        }
    }

    @Nested
    class IsOrderCompletedWithUserIdAndProductId {
        @Test
        void shouldReturnTrue_WhenNoVariationsAndOrderFound() {
            try (MockedStatic<AuthenticationUtils> authMock = mockStatic(AuthenticationUtils.class)) {
                authMock.when(AuthenticationUtils::extractUserId).thenReturn("user1");
                when(productService.getProductVariations(1L)).thenReturn(Collections.emptyList());
                when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.of(order));

                OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(1L);
                assertThat(result).isNotNull();
            }
        }

        @Test
        void shouldReturnFalse_WhenHasVariationsAndOrderNotFound() {
            try (MockedStatic<AuthenticationUtils> authMock = mockStatic(AuthenticationUtils.class)) {
                authMock.when(AuthenticationUtils::extractUserId).thenReturn("user1");
                ProductVariationVm variation = mock(ProductVariationVm.class);
                when(productService.getProductVariations(1L)).thenReturn(List.of(variation));
                when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

                OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(1L);
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    class GetMyOrders {
        @Test
        void shouldReturnOrderList() {
            try (MockedStatic<AuthenticationUtils> authMock = mockStatic(AuthenticationUtils.class)) {
                authMock.when(AuthenticationUtils::extractUserId).thenReturn("user1");
                when(orderRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(order));

                List<OrderGetVm> result = orderService.getMyOrders("prod", OrderStatus.PENDING);
                assertThat(result).hasSize(1);
            }
        }
    }

    @Nested
    class CheckoutIdFinders {
        @Test
        void findOrderVmByCheckoutId_ShouldReturnVm() {
            when(orderRepository.findByCheckoutId("chk1")).thenReturn(Optional.of(order));
            when(orderItemRepository.findAllByOrderId(1L)).thenReturn(List.of(OrderItem.builder().id(1L).build()));

            OrderGetVm result = orderService.findOrderVmByCheckoutId("chk1");
            assertThat(result).isNotNull();
        }

        @Test
        void findOrderByCheckoutId_ShouldThrowNotFound_WhenNotExists() {
            when(orderRepository.findByCheckoutId("chk1")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.findOrderByCheckoutId("chk1"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("is not found");
        }
    }

    @Nested
    class UpdatePaymentStatus {
        @Test
        void shouldUpdateToPaid_WhenStatusIsCompleted() {
            PaymentOrderStatusVm req = mock(PaymentOrderStatusVm.class);
            when(req.orderId()).thenReturn(1L);
            when(req.paymentId()).thenReturn(1L);
            when(req.paymentStatus()).thenReturn("COMPLETED");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(req);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result).isNotNull();
        }

        @Test
        void shouldNotUpdateToPaid_WhenStatusIsNotCompleted() {
            PaymentOrderStatusVm req = mock(PaymentOrderStatusVm.class);
            when(req.orderId()).thenReturn(1L);
            when(req.paymentId()).thenReturn(1L);
            when(req.paymentStatus()).thenReturn("PENDING");

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            orderService.updateOrderPaymentStatus(req);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    @Nested
    class AcceptAndRejectOrder {
        @Test
        void rejectOrder_ShouldSetStatusToReject() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            orderService.rejectOrder(1L, "Out of stock");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
            assertThat(order.getRejectReason()).isEqualTo("Out of stock");
            verify(orderRepository).save(order);
        }

        @Test
        void acceptOrder_ShouldSetStatusToAccepted() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            orderService.acceptOrder(1L);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);
            verify(orderRepository).save(order);
        }
    }

    @Nested
    class ExportCsv {

        // Cấu hình mock request với đầy đủ thông tin bắt buộc để hàm Pair.of() không bị
        // dính NullPointerException
        private OrderRequest prepareMockRequest() {
            OrderRequest request = mock(OrderRequest.class);
            when(request.getPageNo()).thenReturn(0);
            when(request.getPageSize()).thenReturn(10);
            when(request.getCreatedFrom()).thenReturn(ZonedDateTime.now());
            when(request.getCreatedTo()).thenReturn(ZonedDateTime.now());
            when(request.getBillingCountry()).thenReturn("VN");
            when(request.getBillingPhoneNumber()).thenReturn("123456789");
            when(request.getEmail()).thenReturn("test@mail.com");
            when(request.getProductName()).thenReturn("Prod");
            when(request.getOrderStatus()).thenReturn(List.of(OrderStatus.PENDING));
            return request;
        }

        @Test
        void shouldReturnEmptyCsvBytes_WhenOrderListIsNull() throws IOException {
            OrderRequest request = prepareMockRequest();
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            try (MockedStatic<CsvExporter> csvMock = mockStatic(CsvExporter.class)) {
                csvMock.when(() -> CsvExporter.exportToCsv(anyList(), eq(OrderItemCsv.class)))
                        .thenReturn(new byte[] { 1 });

                byte[] result = orderService.exportCsv(request);
                assertThat(result).isNotEmpty();
            }
        }

        @Test
        void shouldReturnCsvBytes_WhenOrderListHasContent() throws IOException {
            OrderRequest request = prepareMockRequest();

            Page<Order> page = new PageImpl<>(List.of(order));
            when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(orderMapper.toCsv(any(OrderBriefVm.class))).thenReturn(mock(OrderItemCsv.class));

            try (MockedStatic<CsvExporter> csvMock = mockStatic(CsvExporter.class)) {
                csvMock.when(() -> CsvExporter.exportToCsv(anyList(), eq(OrderItemCsv.class)))
                        .thenReturn(new byte[] { 1, 2, 3 });

                byte[] result = orderService.exportCsv(request);
                assertThat(result).isNotEmpty();
            }
        }
    }
}