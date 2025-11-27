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

        var voLine = new OrderLine(UUID.randomUUID().toString(), 2, Money.zero(), 5L);
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
    @DisplayName("GivenOrderLineVO_WhenToPayloadOrderLine_ThenMapsCorrectly")
    void GivenOrderLineVO_WhenToPayloadOrderLine_ThenMapsCorrectly() {
        // given
        var uuid = UUID.randomUUID();
        var voLine = new OrderLine(uuid.toString(), 3, Money.zero(), 7L);

        // when
        var payload = underTest.toPayloadOrderLine(voLine);

        // then
        assertThat(payload.menuItemId()).isEqualTo(uuid);
        assertThat(payload.quantity()).isEqualTo(3);
        assertThat(payload.version()).isEqualTo(7L);
    }

    @Test
    @DisplayName("GivenOrder_WhenToDetailsResponse_ThenMapsFieldsAndLines")
    void GivenOrder_WhenToDetailsResponse_ThenMapsFieldsAndLines() {
        // given
        var id = OrderId.generate();
        var lineUuid = UUID.randomUUID().toString();
        var lines = List.of(new OrderLine(lineUuid, 1, Money.zero(), 2L));
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
        assertThat(mappedLine.version()).isEqualTo(2L);
    }

}
