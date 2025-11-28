package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.Money;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import com.polsl.engineering.project.rms.order.vo.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    OrderMapper underTest = Mappers.getMapper(OrderMapper.class);

    @Test
    @DisplayName("GivenPlaceDeliveryRequest_WhenToCommand_ThenMapsFields")
    void GivenPlaceDeliveryRequest_WhenToCommand_ThenMapsFields() {
        // given
        var customer = new CustomerInfo("John", "Doe", "123456789");
        var address = new Address("Main St", "1", null, "City", "12-345");
        var request = new OrderPayloads.PlaceDeliveryOrderRequest(customer, address, DeliveryMode.ASAP, null, List.of());

        var voLine = new OrderLine(UUID.randomUUID().toString(), 2, Money.zero(), "name");
        var voLines = List.of(voLine);

        // when
        var result = underTest.toCommand(request, voLines);

        // then
        assertThat(result.customerInfo()).isEqualTo(customer);
        assertThat(result.address()).isEqualTo(address);
        assertThat(result.deliveryMode()).isEqualTo(DeliveryMode.ASAP);
        assertThat(result.scheduledFor()).isNull();
        assertThat(result.orderLines()).isEqualTo(voLines);
    }

    @Test
    @DisplayName("GivenPlacePickUpRequest_WhenToCommand_ThenMapsFields")
    void GivenPlacePickUpRequest_WhenToCommand_ThenMapsFields() {
        // given
        var customer = new CustomerInfo("Bob", "Marley", "000000000");
        var request = new OrderPayloads.PlacePickUpOrderRequest(customer, DeliveryMode.SCHEDULED, LocalTime.of(18, 30), List.of());

        var voLine = new OrderLine(UUID.randomUUID().toString(), 1, Money.zero(), "item");
        var voLines = List.of(voLine);

        // when
        var result = underTest.toCommand(request, voLines);

        // then
        assertThat(result.customerInfo()).isEqualTo(customer);
        assertThat(result.deliveryMode()).isEqualTo(DeliveryMode.SCHEDULED);
        assertThat(result.scheduledFor()).isEqualTo(LocalTime.of(18, 30));
        assertThat(result.orderLines()).isEqualTo(voLines);
    }

    @Test
    @DisplayName("GivenApproveRequest_WhenToCommand_ThenMapsEstimatedTime")
    void GivenApproveRequest_WhenToCommand_ThenMapsEstimatedTime() {
        // given
        var req = new OrderPayloads.ApproveOrderRequest(12);

        // when
        var cmd = underTest.toCommand(req);

        // then
        assertThat(cmd).isNotNull();
        assertThat(cmd.estimatedPreparationMinutes()).isEqualTo(12);
    }

    @Test
    @DisplayName("GivenCancelRequest_WhenToCommand_ThenMapsReason")
    void GivenCancelRequest_WhenToCommand_ThenMapsReason() {
        // given
        var req = new OrderPayloads.CancelOrderRequest("No stock");

        // when
        var cmd = underTest.toCommand(req);

        // then
        assertThat(cmd).isNotNull();
        assertThat(cmd.reason()).isEqualTo("No stock");
    }

    @Test
    @DisplayName("GivenOrder_WhenToResponse_ThenMapsId")
    void GivenOrder_WhenToResponse_ThenMapsId() {
        // given
        var id = OrderId.generate();
        var order = Order.reconstruct(
                id,
                OrderType.PICKUP,
                DeliveryMode.ASAP,
                OrderStatus.PENDING_APPROVAL,
                List.of(),
                null,
                new CustomerInfo("C","D","1"),
                null,
                null,
                null,
                Instant.now(), // placedAt
                null, // approvedAt (LocalDateTime)
                null, // deliveryStartedAt (LocalDateTime)
                Instant.now(), // updatedAt
                0L
        );

        // when
        var resp = underTest.toResponse(order);

        // then
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isEqualTo(id.value());
    }

    @Test
    @DisplayName("GivenOrder_WhenToDetailsResponse_ThenMapsFieldsAndLines")
    void GivenOrder_WhenToDetailsResponse_ThenMapsFieldsAndLines() {
        // given
        var id = OrderId.generate();
        var lineUuid = UUID.randomUUID().toString();
        var lines = List.of(new OrderLine(lineUuid, 1, Money.zero(), "name"));
        var placedAt = Instant.now();
        var approvedAt = LocalDateTime.now().minusMinutes(5);

        var order = Order.reconstruct(
                id,
                OrderType.DELIVERY,
                DeliveryMode.ASAP,
                OrderStatus.APPROVED,
                lines,
                new Address("Str", "10", "1", "Town", "00-001"),
                new CustomerInfo("Jane", "Smith", "987654321"),
                LocalTime.of(12, 0),
                15,
                null,
                placedAt,
                approvedAt,
                null,
                Instant.now(),
                0L
        );

        // when
        var details = underTest.toDetailsResponse(order);

        // then
        assertThat(details.id()).isEqualTo(id.value());
        assertThat(details.status()).isEqualTo(order.getStatus().name());
        assertThat(details.customerInfo()).isEqualTo(order.getCustomerInfo());
        assertThat(details.address()).isEqualTo(order.getDeliveryAddress());
        assertThat(details.deliveryMode()).isEqualTo(order.getDeliveryMode());
        assertThat(details.scheduledFor()).isEqualTo(order.getScheduledFor());
        assertThat(details.estimatedPreparationTimeMinutes()).isEqualTo(order.getEstimatedPreparationMinutes());
        assertThat(details.approvedAt()).isEqualTo(order.getApprovedAt());
        assertThat(details.deliveryStartedAt()).isEqualTo(order.getDeliveryStartedAt());

        // lines mapping: payload menuItemId should be UUID parsed from original string
        assertThat(details.orderLines()).hasSize(1);
        var mappedLine = details.orderLines().getFirst();
        assertThat(mappedLine.menuItemId()).isEqualTo(UUID.fromString(lineUuid));
        assertThat(mappedLine.quantity()).isEqualTo(1);
        assertThat(mappedLine.menuItemName()).isEqualTo("name");
        assertThat(mappedLine.unitPrice()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("GivenOrder_WhenToInitialData_ThenMapsFieldsAndLines")
    void GivenOrder_WhenToInitialData_ThenMapsFieldsAndLines() {
        // given
        var id = OrderId.generate();
        var lineUuid = UUID.randomUUID().toString();
        var lines = List.of(new OrderLine(lineUuid, 2, Money.zero(), "item"));
        var placedAt = Instant.now();
        var approvedAt = LocalDateTime.now().minusMinutes(8);
        var deliveryStartedAt = LocalDateTime.now().minusMinutes(3);

        var order = Order.reconstruct(
                id,
                OrderType.DELIVERY,
                DeliveryMode.SCHEDULED,
                OrderStatus.READY_FOR_DRIVER,
                lines,
                new Address("A","1",null,"City","11-111"),
                new CustomerInfo("Zoe","Lee","555"),
                LocalTime.of(20,0),
                25,
                null,
                placedAt,
                approvedAt,
                deliveryStartedAt,
                Instant.now(),
                2L
        );

        // when
        var evt = underTest.toInitialData(order);

        // then
        assertThat(evt).isNotNull();
        assertThat(evt.id()).isEqualTo(id.value());
        assertThat(evt.status()).isEqualTo(order.getStatus().name());
        assertThat(evt.customerInfo()).isEqualTo(order.getCustomerInfo());
        assertThat(evt.address()).isEqualTo(order.getDeliveryAddress());
        assertThat(evt.deliveryMode()).isEqualTo(order.getDeliveryMode());
        assertThat(evt.scheduledFor()).isEqualTo(order.getScheduledFor());
        assertThat(evt.placedAt()).isEqualTo(order.getPlacedAt());
        assertThat(evt.estimatedPreparationTimeMinutes()).isEqualTo(order.getEstimatedPreparationMinutes());
        assertThat(evt.approvedAt()).isEqualTo(order.getApprovedAt());
        assertThat(evt.deliveryStartedAt()).isEqualTo(order.getDeliveryStartedAt());

        assertThat(evt.orderLines()).hasSize(1);
        var mapped = evt.orderLines().getFirst();
        assertThat(mapped.menuItemId()).isEqualTo(UUID.fromString(lineUuid));
        assertThat(mapped.quantity()).isEqualTo(2);
        assertThat(mapped.menuItemName()).isEqualTo("item");
    }

    @Test
    @DisplayName("GivenOrder_WhenToCustomerViewResponse_ThenMapsAllFields")
    void GivenOrder_WhenToCustomerViewResponse_ThenMapsAllFields() {
        // given
        var id = OrderId.generate();
        var lines = List.<OrderLine>of();
        var placedAt = Instant.now();
        var approvedAt = LocalDateTime.now().minusMinutes(2);
        var deliveryStartedAt = LocalDateTime.now().minusMinutes(1);

        var order = Order.reconstruct(
                id,
                OrderType.DELIVERY,
                DeliveryMode.ASAP,
                OrderStatus.APPROVED,
                lines,
                null,
                new CustomerInfo("Alice", "Brown", "111222333"),
                LocalTime.of(13, 0),
                20,
                null,
                placedAt,
                approvedAt,
                deliveryStartedAt,
                Instant.now(),
                1L
        );

        // when
        var view = underTest.toCustomerViewResponse(order);

        // then
        assertThat(view).isNotNull();
        assertThat(view.id()).isEqualTo(id.value());
        // OrderStatus.APPROVED is mapped to OrderCustomerVisibleStatus.CONFIRMED
        assertThat(view.status()).isEqualTo("CONFIRMED");
        assertThat(view.orderType()).isEqualTo(OrderType.DELIVERY);
        assertThat(view.estimatedPreparationMinutes()).isEqualTo(20);
        assertThat(view.cancellationReason()).isNull();
        assertThat(view.approvedAt()).isEqualTo(approvedAt);
        assertThat(view.deliveryStartedAt()).isEqualTo(deliveryStartedAt);
    }

    @Test
    @DisplayName("GivenOrderLineVO_WhenToPayloadOrderLine_ThenMapsCorrectly")
    void GivenOrderLineVO_WhenToPayloadOrderLine_ThenMapsCorrectly() {
        // given
        var uuid = UUID.randomUUID();
        var voLine = new OrderLine(uuid.toString(), 3, Money.zero(), "name");

        // when
        var payload = underTest.toPayloadOrderLine(voLine);

        // then
        assertThat(payload).isNotNull();
        assertThat(payload.menuItemId()).isEqualTo(uuid);
        assertThat(payload.quantity()).isEqualTo(3);
        assertThat(payload.menuItemName()).isEqualTo("name");
        assertThat(payload.unitPrice()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("GivenNullOrEmptyLines_WhenMapLines_ThenHandlesGracefully")
    void GivenNullOrEmptyLines_WhenMapLines_ThenHandlesGracefully() {
        // when
        var resultNull = underTest.mapLines(null);
        var resultEmpty = underTest.mapLines(List.of());

        // then
        assertThat(resultNull).isNotNull().isEmpty();
        assertThat(resultEmpty).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("GivenMultipleLines_WhenMapLines_ThenMapsAll")
    void GivenMultipleLines_WhenMapLines_ThenMapsAll() {
        // given
        var uuid1 = UUID.randomUUID().toString();
        var uuid2 = UUID.randomUUID().toString();
        var lines = List.of(
                new OrderLine(uuid1, 1, Money.zero(), "first"),
                new OrderLine(uuid2, 2, Money.zero(), "second")
        );

        // when
        var mapped = underTest.mapLines(lines);

        // then
        assertThat(mapped).hasSize(2);
        assertThat(mapped.get(0).menuItemId()).isEqualTo(UUID.fromString(uuid1));
        assertThat(mapped.get(1).menuItemId()).isEqualTo(UUID.fromString(uuid2));
    }

}
