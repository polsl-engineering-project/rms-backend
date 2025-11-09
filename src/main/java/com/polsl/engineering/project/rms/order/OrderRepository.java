package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import com.polsl.engineering.project.rms.order.vo.OrderType;
import com.polsl.engineering.project.rms.order.vo.DeliveryMode;
import com.polsl.engineering.project.rms.order.vo.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.polsl.engineering.project.rms.common.db.QueryLogging;

@RequiredArgsConstructor
@Slf4j
@Repository
class OrderRepository {

    private final JdbcTemplate jdbcTemplate;
    private final OrderDbMapper dbMapper;
    private final OrderQueryParamsBuilder paramsBuilder;

    private static final String ORDER_INSERT_SQL = """
            INSERT INTO orders (
                id,
                order_type,
                delivery_mode,
                order_status,
                delivery_street,
                delivery_house_number,
                delivery_apartment_number,
                delivery_city,
                delivery_postal_code,
                customer_first_name,
                customer_last_name,
                customer_phone_number,
                scheduled_for,
                estimated_preparation_minutes,
                cancellation_reason,
                placed_at,
                updated_at,
                version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String ORDER_UPDATE_SQL = """
            UPDATE orders
            SET
                order_type = ?,
                delivery_mode = ?,
                order_status = ?,
                delivery_street = ?,
                delivery_house_number = ?,
                delivery_apartment_number = ?,
                delivery_city = ?,
                delivery_postal_code = ?,
                customer_first_name = ?,
                customer_last_name = ?,
                customer_phone_number = ?,
                scheduled_for = ?,
                estimated_preparation_minutes = ?,
                cancellation_reason = ?,
                placed_at = ?,
                updated_at = ?,
                version = version + 1
            WHERE id = ? AND version = ?
            """;

    private static final String ORDER_LINES_INSERT_SQL = """
            INSERT INTO order_lines (
                id,
                order_id,
                menu_item_id,
                quantity,
                unit_price,
                menu_item_version
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String ORDER_LINES_DELETE_SQL = """
            DELETE FROM order_lines
            WHERE order_id = ?
            """;

    public Optional<Order> findById(OrderId orderId) {
        final var orderSql = """
                SELECT *
                FROM orders
                WHERE id = ?
                """;

        try {
            QueryLogging.logSql(log, QueryLogging.KIND_QUERY, orderSql, orderId.value());
            Order order = jdbcTemplate.queryForObject(orderSql, (rs, _) -> {
                var id = rs.getObject("id", java.util.UUID.class);
                var idVo = new OrderId(id);

                var type = OrderType.valueOf(rs.getString("order_type"));
                var deliveryMode = DeliveryMode.valueOf(rs.getString("delivery_mode"));
                var status = OrderStatus.valueOf(rs.getString("order_status"));

                var customerInfo = dbMapper.mapCustomerInfo(rs);

                var deliveryAddress = dbMapper.mapDeliveryAddress(rs);

                var scheduledFor = dbMapper.mapScheduledFor(rs);

                var estimatedPreparationMinutes = dbMapper.mapNullableInteger(rs, "estimated_preparation_minutes");

                var cancellationReason = rs.getString("cancellation_reason");

                var placedAt = dbMapper.mapInstant(rs, "placed_at");
                var updatedAt = dbMapper.mapInstant(rs, "updated_at");

                var version = rs.getLong("version");

                var lines = loadOrderLines(id);

                return Order.reconstruct(
                        idVo,
                        type,
                        deliveryMode,
                        status,
                        lines,
                        deliveryAddress,
                        customerInfo,
                        scheduledFor,
                        estimatedPreparationMinutes,
                        cancellationReason,
                        placedAt,
                        updatedAt,
                        version
                );
            }, orderId.value());

            return Optional.ofNullable(order);
        } catch (EmptyResultDataAccessException _) {
            return Optional.empty();
        }
    }

    @Transactional
    public void saveNewOrder(Order order) {
        var params = paramsBuilder.buildInsertParams(order);
        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, ORDER_INSERT_SQL, params);
        jdbcTemplate.update(ORDER_INSERT_SQL, params);
        insertLines(order.getId().value(), order.getLines());
    }

    @Transactional
    public void updateWithoutLines(Order order) {
        var params = buildUpdateParams(order);
        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, ORDER_UPDATE_SQL, params);
        var updated = jdbcTemplate.update(ORDER_UPDATE_SQL, params);
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Failed to update Order, optimistic lock lost (id=" + order.getId().value() + ")");
        }
    }

    @Transactional
    public void updateWithLines(Order order) {
        var params = buildUpdateParams(order);
        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, ORDER_UPDATE_SQL, params);
        var updated = jdbcTemplate.update(ORDER_UPDATE_SQL, params);
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Failed to update Order, optimistic lock lost (id=" + order.getId().value() + ")");
        }

        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, ORDER_LINES_DELETE_SQL, order.getId().value());
        jdbcTemplate.update(ORDER_LINES_DELETE_SQL, order.getId().value());
        insertLines(order.getId().value(), order.getLines());
    }

    private Object[] buildUpdateParams(Order order) {
        return paramsBuilder.buildUpdateParams(order);
    }

    private void insertLines(java.util.UUID orderId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) return;
        var batchArgs = new ArrayList<Object[]>();
        for (var line : lines) {
            var lineId = UUID.randomUUID();
            var unitPrice = line.unitPrice().amount();
            batchArgs.add(new Object[]{lineId, orderId, line.menuItemId(), line.quantity(), unitPrice, line.menuItemVersion()});
        }
        var sample = batchArgs.size() > 3 ? batchArgs.subList(0, 3) : batchArgs;
        QueryLogging.logBatch(log, ORDER_LINES_INSERT_SQL, batchArgs.size(), sample);
        jdbcTemplate.batchUpdate(ORDER_LINES_INSERT_SQL, batchArgs);
    }

    private List<OrderLine> loadOrderLines(java.util.UUID orderId) {
        final var linesSql = """
                SELECT *
                FROM order_lines
                WHERE order_id = ?
                ORDER BY id
                """;
        QueryLogging.logSql(log, QueryLogging.KIND_QUERY, linesSql, orderId);
        return jdbcTemplate.query(linesSql, (r2, _) -> dbMapper.mapOrderLine(r2), orderId);
    }

}
