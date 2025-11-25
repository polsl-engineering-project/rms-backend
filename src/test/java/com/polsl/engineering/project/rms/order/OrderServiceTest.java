package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.general.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.general.result.Result;
import com.polsl.engineering.project.rms.menu.MenuApi;
import com.polsl.engineering.project.rms.menu.dto.MenuItemSnapshotForOrder;
import com.polsl.engineering.project.rms.order.cmd.ApproveOrderCommand;
import com.polsl.engineering.project.rms.order.cmd.PlacePickUpOrderCommand;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.event.OrderEvent;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository jdbcRepository;

    @Mock
    MenuApi menuApi;

    @Mock
    OrderMapper mapper;

    @Mock
    Clock clock;

    @Mock
    OrderOutboxService outboxService;

    @InjectMocks
    OrderService underTest;

    @Test
    @DisplayName("Given valid pickup request_When placePickUpOrder_Then saves order and returns response")
    void GivenValidPickupRequest_WhenPlacePickUpOrder_ThenSavesAndReturnsResponse() {
        //given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new OrderPayloads.OrderLine(payloadLineId, 2, 1L);
        var customer = new CustomerInfo("John", "Doe", "123");
        var request = new OrderPayloads.PlacePickUpOrderRequest(customer, DeliveryMode.ASAP, null, List.of(payloadLine));

        var snapshot = new MenuItemSnapshotForOrder(payloadLineId, new BigDecimal("12.50"), "Dish", 1L);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of(payloadLineId, snapshot));

        // mapper.toCommand should return a valid command based on provided domain lines
        when(mapper.toCommand(any(OrderPayloads.PlacePickUpOrderRequest.class), anyList()))
                .thenAnswer(invocation -> new PlacePickUpOrderCommand(
                        invocation.getArgument(0, OrderPayloads.PlacePickUpOrderRequest.class).customerInfo(),
                        invocation.getArgument(0, OrderPayloads.PlacePickUpOrderRequest.class).deliveryMode(),
                        invocation.getArgument(0, OrderPayloads.PlacePickUpOrderRequest.class).scheduledFor(),
                        invocation.getArgument(1, List.class)
                ));

        var expectedUuid = UUID.randomUUID();
        when(mapper.toResponse(any(Order.class))).thenReturn(new OrderPayloads.OrderPlacedResponse(expectedUuid));

        //when
        var response = underTest.placePickUpOrder(request);

        //then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(expectedUuid);
        verify(jdbcRepository).saveNewOrder(any(Order.class));
        verify(mapper).toResponse(any(Order.class));
        // outbox should receive one persisted event (order placement)
        verify(outboxService).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given missing menu item_When placePickUpOrder_Then throws MenuItemNotFoundException")
    void GivenMissingMenuItem_WhenPlacePickUpOrder_ThenThrowsMenuItemNotFoundException() {
        //given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new OrderPayloads.OrderLine(payloadLineId, 1, 0L);
        var customer = new CustomerInfo("John", "Doe", "123");
        var request = new OrderPayloads.PlacePickUpOrderRequest(customer, DeliveryMode.ASAP, null, List.of(payloadLine));

        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of());

        //when / then
        assertThatThrownBy(() -> underTest.placePickUpOrder(request))
                .isInstanceOf(com.polsl.engineering.project.rms.order.exception.MenuItemNotFoundException.class)
                .hasMessageContaining(payloadLineId.toString());

        verify(jdbcRepository, never()).saveNewOrder(any());
        verify(outboxService, never()).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given version mismatch_When placeDeliveryOrder_Then throws MenuItemVersionMismatchException")
    void GivenVersionMismatch_WhenPlaceDeliveryOrder_ThenThrowsMenuItemVersionMismatchException() {
        //given
        var payloadLineId = UUID.randomUUID();
        var payloadLine = new OrderPayloads.OrderLine(payloadLineId, 1, 5L); // request says version 5
        var customer = new CustomerInfo("John", "Doe", "123");
        var address = new com.polsl.engineering.project.rms.order.vo.Address("Street", "1", null, "City", "00-001");
        var request = new OrderPayloads.PlaceDeliveryOrderRequest(customer, address, DeliveryMode.ASAP, null, List.of(payloadLine));

        // menu snapshot has different version
        var snapshot = new MenuItemSnapshotForOrder(payloadLineId, new BigDecimal("5.00"), "Dish", 1L);
        when(menuApi.getSnapshotsForOrderByIds(anyList())).thenReturn(Map.of(payloadLineId, snapshot));

        //when / then
        assertThatThrownBy(() -> underTest.placeDeliveryOrder(request))
                .isInstanceOf(com.polsl.engineering.project.rms.order.exception.MenuItemVersionMismatchException.class);

        verify(jdbcRepository, never()).saveNewOrder(any());
        verify(outboxService, never()).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given removable lines_When changeOrderLines_Then updates repository with lines")
    void GivenRemovableLines_WhenChangeOrderLines_ThenUpdatesRepositoryWithLines() {
        //given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        var orderMock = mock(Order.class);
        when(jdbcRepository.findById(any())).thenReturn(Optional.of(orderMock));
        when(orderMock.changeOrderLines(any(), any(Clock.class))).thenReturn(Result.ok(null));
        // stub emitted events and id for outbox verification
        var eventMock = mock(OrderEvent.class);
        when(orderMock.pullEvents()).thenReturn(List.of(eventMock));
        when(orderMock.getId()).thenReturn(OrderId.generate());

        var removeLine = new OrderPayloads.RemoveLine(UUID.randomUUID(), 1);
        var request = new OrderPayloads.ChangeOrderLinesRequest(List.of(), List.of(removeLine), 10);

        //when
        underTest.changeOrderLines(orderId, request);

        //then
        verify(jdbcRepository).updateWithLines(orderMock);
        verify(outboxService).persistEvent(any(), any());
    }

    // New tests for completeOrder
    @Test
    @DisplayName("Given existing order_When completeOrder_Then updates repository and persists events")
    void GivenExistingOrder_WhenCompleteOrder_ThenUpdatesRepoAndPersistsEvents() {
        // given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        var orderMock = mock(Order.class);
        when(jdbcRepository.findById(any())).thenReturn(Optional.of(orderMock));
        when(orderMock.complete(any(UserPrincipal.class), any(Clock.class))).thenReturn(Result.ok(null));
        var eventMock = mock(OrderEvent.class);
        when(orderMock.pullEvents()).thenReturn(List.of(eventMock));
        when(orderMock.getId()).thenReturn(OrderId.generate());

        var user = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // when
        underTest.completeOrder(orderId, user);

        // then
        verify(jdbcRepository).updateWithoutLines(orderMock);
        verify(outboxService).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given order not found_When completeOrder_Then throws ResourceNotFoundException")
    void GivenOrderNotFound_WhenCompleteOrder_ThenThrowsResourceNotFoundException() {
        // given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        when(jdbcRepository.findById(any())).thenReturn(Optional.empty());

        var user = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // when / then
        assertThatThrownBy(() -> underTest.completeOrder(orderId, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(orderId);

        verify(jdbcRepository, never()).updateWithoutLines(any());
        verify(outboxService, never()).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given failing aggregate_When completeOrder_Then throws InvalidOrderActionException")
    void GivenFailingAggregate_WhenCompleteOrder_ThenThrowsInvalidOrderActionException() {
        // given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        var orderMock = mock(Order.class);
        when(jdbcRepository.findById(any())).thenReturn(Optional.of(orderMock));
        when(orderMock.complete(any(UserPrincipal.class), any(Clock.class))).thenReturn(Result.failure("not allowed"));

        var user = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // when / then
        assertThatThrownBy(() -> underTest.completeOrder(orderId, user))
                .isInstanceOf(com.polsl.engineering.project.rms.order.exception.InvalidOrderActionException.class);

        verify(jdbcRepository, never()).updateWithoutLines(orderMock);
        verify(outboxService, never()).persistEvent(any(), any());
    }

    // New tests for approve
    @Test
    @DisplayName("Given existing order_When approve_Then updates repository and persists events")
    void GivenExistingOrder_WhenApprove_ThenUpdatesRepoAndPersistsEvents() {
        // given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        var orderMock = mock(Order.class);
        when(jdbcRepository.findById(any())).thenReturn(Optional.of(orderMock));
        when(mapper.toCommand(any(OrderPayloads.ApproveOrderRequest.class))).thenReturn(new ApproveOrderCommand(15));
        when(orderMock.approve(any(ApproveOrderCommand.class), any(Clock.class))).thenReturn(Result.ok(null));
        var eventMock = mock(OrderEvent.class);
        when(orderMock.pullEvents()).thenReturn(List.of(eventMock));
        when(orderMock.getId()).thenReturn(OrderId.generate());

        var request = new OrderPayloads.ApproveOrderRequest(15);

        // when
        underTest.approve(orderId, request);

        // then
        verify(jdbcRepository).updateWithoutLines(orderMock);
        verify(outboxService).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given order not found_When approve_Then throws ResourceNotFoundException")
    void GivenOrderNotFound_WhenApprove_ThenThrowsResourceNotFoundException() {
        // given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        when(jdbcRepository.findById(any())).thenReturn(Optional.empty());

        var request = new OrderPayloads.ApproveOrderRequest(10);

        // when / then
        assertThatThrownBy(() -> underTest.approve(orderId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(orderId);

        verify(jdbcRepository, never()).updateWithoutLines(any());
        verify(outboxService, never()).persistEvent(any(), any());
    }

    @Test
    @DisplayName("Given failing aggregate_When approve_Then throws InvalidOrderActionException")
    void GivenFailingAggregate_WhenApprove_ThenThrowsInvalidOrderActionException() {
        // given
        var id = UUID.randomUUID();
        var orderId = id.toString();
        var orderMock = mock(Order.class);
        when(jdbcRepository.findById(any())).thenReturn(Optional.of(orderMock));
        when(mapper.toCommand(any(OrderPayloads.ApproveOrderRequest.class))).thenReturn(new ApproveOrderCommand(null));
        when(orderMock.approve(any(ApproveOrderCommand.class), any(Clock.class))).thenReturn(Result.failure("not allowed"));

        var request = new OrderPayloads.ApproveOrderRequest(null);

        // when / then
        assertThatThrownBy(() -> underTest.approve(orderId, request))
                .isInstanceOf(com.polsl.engineering.project.rms.order.exception.InvalidOrderActionException.class);

        verify(jdbcRepository, never()).updateWithoutLines(orderMock);
        verify(outboxService, never()).persistEvent(any(), any());
    }

}
