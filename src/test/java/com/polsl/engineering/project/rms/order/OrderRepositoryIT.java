package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderType;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({OrderRepository.class, OrderDbMapper.class, OrderQueryParamsBuilder.class})
@DisplayName("Integration tests for OrderRepository")
class OrderRepositoryIT extends ContainersEnvironment {

    @Autowired
    OrderRepository underTest;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Flyway migrations are applied once when the testcontainer is started (see ContainersEnvironment).
        // Here we only populate test data required by the individual test class.
        var populator = new ResourceDatabasePopulator(new ClassPathResource("init/sql/order.sql"));
        populator.execute(dataSource);
    }

    @Test
    @DisplayName("Given existing order id_When findById_Then returns order with expected fields and lines")
    void GivenExistingOrderId_WhenFindById_ThenReturnOrder() {
        // given
        var orderId = OrderId.from("00000000-0000-0000-0000-000000000001");

        // when
        var optOrder = underTest.findById(orderId);

        // then
        assertThat(optOrder).isPresent();
        var order = optOrder.get();

        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getType()).isEqualTo(OrderType.PICKUP);
        assertThat(order.getDeliveryMode()).isEqualTo(DeliveryMode.SCHEDULED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // customer info
        var customer = order.getCustomerInfo();
        assertThat(customer).isNotNull();
        assertThat(customer.firstName()).isEqualTo("John");
        assertThat(customer.lastName()).isEqualTo("Doe");
        assertThat(customer.phoneNumber()).isEqualTo("123456789");

        // scheduling and estimates
        assertThat(order.getScheduledFor()).isEqualTo(LocalTime.parse("13:00:00"));
        assertThat(order.getEstimatedPreparationMinutes()).isEqualTo(15);

        // lines
        var lines = order.getLines();
        assertThat(lines).hasSize(2);
        assertThat(lines).extracting(OrderLine::menuItemId).containsExactlyInAnyOrder("pizza", "cake");

        var pizzaLine = lines.stream().filter(l -> l.menuItemId().equals("pizza")).findFirst().orElseThrow();
        assertThat(pizzaLine.unitPrice().amount()).isEqualByComparingTo(new BigDecimal("30.00"));

        var cakeLine = lines.stream().filter(l -> l.menuItemId().equals("cake")).findFirst().orElseThrow();
        assertThat(cakeLine.unitPrice().amount()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    @DisplayName("Given non-existing order id_When findById_Then returns empty optional")
    void GivenNonExistingOrderId_WhenFindById_ThenReturnEmptyOptional() {
        // given
        var orderId = OrderId.from("00000000-0000-0000-0000-000000000099");

        // when
        var optOrder = underTest.findById(orderId);

        // then
        assertThat(optOrder).isEmpty();
    }

    @Test
    @DisplayName("Given new order_When saveNewOrder_Then row is persisted in orders and order_lines")
    void GivenNewOrder_WhenSaveNewOrder_ThenPersistedInDb() {
        // given
        var lines = List.of(new OrderLine("soup", 2, new Money(new BigDecimal("5.00")), 1L));
        var customer = new CustomerInfo("Alice", "Smith", "111222333");
        var cmd = new com.polsl.engineering.project.rms.order.cmd.PlacePickUpOrderCommand(customer, DeliveryMode.ASAP, null, lines);

        var result = Order.placePickUpOrder(cmd, Clock.systemUTC());
        assertThat(result.isSuccess()).isTrue();
        var order = result.getValue();

        // when
        underTest.saveNewOrder(order);

        // then
        var orderType = jdbcTemplate.queryForObject("SELECT order_type FROM orders WHERE id = ?", String.class, order.getId().value());
        assertThat(orderType).isEqualTo("PICKUP");

        var firstName = jdbcTemplate.queryForObject("SELECT customer_first_name FROM orders WHERE id = ?", String.class, order.getId().value());
        assertThat(firstName).isEqualTo("Alice");

        List<Map<String, Object>> linesFromDb = jdbcTemplate.queryForList("SELECT menu_item_id, quantity, unit_price FROM order_lines WHERE order_id = ?", order.getId().value());
        assertThat(linesFromDb).hasSize(1);
        Map<String, Object> row = linesFromDb.getFirst();
        assertThat(row).containsEntry("menu_item_id", "soup");
        // unit_price returns BigDecimal from JDBC
        assertThat(row.get("unit_price")).isNotNull();
        assertThat((BigDecimal) row.get("unit_price")).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(row.get("quantity")).isNotNull();
        assertThat(((Number) row.get("quantity")).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given existing order_When updateWithoutLines_Then updates fields and increments version without touching lines")
    void GivenExistingOrder_WhenUpdateWithoutLines_ThenUpdatesFieldsAndIncrementsVersion() {
        // given
        var orderId = OrderId.from("00000000-0000-0000-0000-000000000001");
        var optOrder = underTest.findById(orderId);
        assertThat(optOrder).isPresent();
        var order = optOrder.get();
        var originalVersion = order.getVersion();
        assertThat(originalVersion).isZero();

        // when
        // build a new Order instance with updated status using only getters (no behavior methods)
        var updatedOrder = Order.reconstruct(
                order.getId(),
                order.getType(),
                order.getDeliveryMode(),
                OrderStatus.APPROVED_BY_FRONT_DESK,
                order.getLines(),
                order.getDeliveryAddress(),
                order.getCustomerInfo(),
                order.getScheduledFor(),
                order.getEstimatedPreparationMinutes(),
                order.getCancellationReason(),
                order.getPlacedAt(),
                order.getUpdatedAt(),
                order.getVersion()
        );
        underTest.updateWithoutLines(updatedOrder);

        // then
        var status = jdbcTemplate.queryForObject("SELECT order_status FROM orders WHERE id = ?", String.class, orderId.value());
        assertThat(status).isEqualTo("APPROVED_BY_FRONT_DESK");

        var version = jdbcTemplate.queryForObject("SELECT version FROM orders WHERE id = ?", Long.class, orderId.value());
        assertThat(version).isEqualTo(originalVersion + 1);

        var linesCount = jdbcTemplate.queryForObject("SELECT count(*) FROM order_lines WHERE order_id = ?", Long.class, orderId.value());
        assertThat(linesCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("Given concurrent stale order_When updateWithoutLines_Then throws OptimisticLockingFailureException")
    void GivenStaleOrder_WhenUpdateWithoutLines_ThenThrowsOptimisticLockingFailureException() {
        // given
        var orderId = OrderId.from("00000000-0000-0000-0000-000000000001");
        var optOrderA = underTest.findById(orderId);
        var optOrderB = underTest.findById(orderId);
        assertThat(optOrderA).isPresent();
        assertThat(optOrderB).isPresent();
        var orderA = optOrderA.get();
        var orderB = optOrderB.get();

        // when: first updater builds a new Order instance with updated status and current version
        var updatedA = Order.reconstruct(
                orderA.getId(),
                orderA.getType(),
                orderA.getDeliveryMode(),
                OrderStatus.APPROVED_BY_FRONT_DESK,
                orderA.getLines(),
                orderA.getDeliveryAddress(),
                orderA.getCustomerInfo(),
                orderA.getScheduledFor(),
                orderA.getEstimatedPreparationMinutes(),
                orderA.getCancellationReason(),
                orderA.getPlacedAt(),
                orderA.getUpdatedAt(),
                orderA.getVersion()
        );
        underTest.updateWithoutLines(updatedA);

        // then: second updater still holds stale version (orderB.version == original) and should fail
        var updatedB = Order.reconstruct(
                orderB.getId(),
                orderB.getType(),
                orderB.getDeliveryMode(),
                OrderStatus.APPROVED_BY_FRONT_DESK,
                orderB.getLines(),
                orderB.getDeliveryAddress(),
                orderB.getCustomerInfo(),
                orderB.getScheduledFor(),
                orderB.getEstimatedPreparationMinutes(),
                orderB.getCancellationReason(),
                orderB.getPlacedAt(),
                orderB.getUpdatedAt(),
                orderB.getVersion()
        );
        assertThatThrownBy(() -> underTest.updateWithoutLines(updatedB))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Given existing order_When updateWithLines_Then replaces lines and increments version")
    void GivenExistingOrder_WhenUpdateWithLines_ThenReplacesLinesAndIncrementsVersion() {
        // given
        var orderId = OrderId.from("00000000-0000-0000-0000-000000000001");
        var optOrder = underTest.findById(orderId);
        assertThat(optOrder).isPresent();
        var order = optOrder.get();
        var originalVersion = order.getVersion();
        assertThat(originalVersion).isZero();

        // new lines to replace existing ones
        var newLines = List.of(
                new OrderLine("salad", 2, new Money(new BigDecimal("9.99")), 1L),
                new OrderLine("soda", 3, new Money(new BigDecimal("2.50")), 1L)
        );

        // when
        var updatedOrder = Order.reconstruct(
                order.getId(),
                order.getType(),
                order.getDeliveryMode(),
                OrderStatus.CONFIRMED,
                newLines,
                order.getDeliveryAddress(),
                order.getCustomerInfo(),
                order.getScheduledFor(),
                order.getEstimatedPreparationMinutes(),
                order.getCancellationReason(),
                order.getPlacedAt(),
                order.getUpdatedAt(),
                order.getVersion()
        );

        underTest.updateWithLines(updatedOrder);

        // then
        var version = jdbcTemplate.queryForObject("SELECT version FROM orders WHERE id = ?", Long.class, orderId.value());
        assertThat(version).isEqualTo(originalVersion + 1);

        List<Map<String, Object>> linesFromDb = jdbcTemplate.queryForList("SELECT menu_item_id, quantity, unit_price FROM order_lines WHERE order_id = ? ORDER BY menu_item_id", orderId.value());
        assertThat(linesFromDb)
                .hasSize(2)
                .anyMatch(m -> "salad".equals(m.get("menu_item_id")) && ((Number)m.get("quantity")).intValue() == 2)
                .anyMatch(m -> "soda".equals(m.get("menu_item_id")) && ((Number)m.get("quantity")).intValue() == 3);
    }

    @Test
    @DisplayName("Given concurrent stale order_When updateWithLines_Then throws OptimisticLockingFailureException")
    void GivenStaleOrder_WhenUpdateWithLines_ThenThrowsOptimisticLockingFailureException() {
        // given
        var orderId = OrderId.from("00000000-0000-0000-0000-000000000001");
        var optA = underTest.findById(orderId);
        var optB = underTest.findById(orderId);
        assertThat(optA).isPresent();
        assertThat(optB).isPresent();
        var a = optA.get();
        var b = optB.get();

        var newLinesA = List.of(new OrderLine("pasta", 1, new Money(new BigDecimal("12.00")), 1L));
        var newLinesB = List.of(new OrderLine("burger", 1, new Money(new BigDecimal("11.00")), 1L));

        var updatedA = Order.reconstruct(
                a.getId(), a.getType(), a.getDeliveryMode(), OrderStatus.CONFIRMED, newLinesA,
                a.getDeliveryAddress(), a.getCustomerInfo(), a.getScheduledFor(), a.getEstimatedPreparationMinutes(), a.getCancellationReason(), a.getPlacedAt(), a.getUpdatedAt(), a.getVersion()
        );
        underTest.updateWithLines(updatedA);

        var updatedB = Order.reconstruct(
                b.getId(), b.getType(), b.getDeliveryMode(), OrderStatus.CONFIRMED, newLinesB,
                b.getDeliveryAddress(), b.getCustomerInfo(), b.getScheduledFor(), b.getEstimatedPreparationMinutes(), b.getCancellationReason(), b.getPlacedAt(), b.getUpdatedAt(), b.getVersion()
        );

        // then
        assertThatThrownBy(() -> underTest.updateWithLines(updatedB))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Given various orders_When findActiveOrders_Then returns only active orders with lines")
    void GivenVariousOrders_WhenFindActiveOrders_ThenReturnsOnlyActiveOrdersWithLines() {
        // when
        var activeOrders = underTest.findActiveOrders();

        // then - none should be COMPLETED or CANCELLED
        assertThat(activeOrders)
                .isNotEmpty()
                .allMatch(o -> o.getStatus() != OrderStatus.COMPLETED && o.getStatus() != OrderStatus.CANCELLED);

        // expected active ids from test data: 0001, 0002, 0006
        assertThat(activeOrders).extracting(Order::getId)
                .containsExactlyInAnyOrder(
                        OrderId.from("00000000-0000-0000-0000-000000000001"),
                        OrderId.from("00000000-0000-0000-0000-000000000002"),
                        OrderId.from("00000000-0000-0000-0000-000000000006")
                );

        // each returned order should have its lines loaded
        activeOrders.forEach(o -> assertThat(o.getLines()).isNotEmpty());
    }
}
