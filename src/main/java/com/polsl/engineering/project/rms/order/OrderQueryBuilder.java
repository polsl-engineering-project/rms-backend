package com.polsl.engineering.project.rms.order;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Getter
class OrderQueryBuilder {

    private final OrderPayloads.OrderSearchRequest criteria;
    private final List<Object> selectParams = new ArrayList<>();
    private final List<Object> countParams = new ArrayList<>();

    OrderQueryBuilder(OrderPayloads.OrderSearchRequest criteria) {
        this.criteria = criteria;
    }

    String buildCountQuery() {
        var sql = new StringBuilder();
        sql.append("""
                SELECT COUNT(*)
                FROM orders o
                """);

        appendWhereClause(sql, countParams);

        return sql.toString();
    }

    String buildSelectQuery(int page, int size) {
        var sql = new StringBuilder();
        sql.append("""
                SELECT
                    o.id,
                    o.order_status,
                    o.delivery_mode,
                    o.customer_first_name,
                    o.placed_at,
                    o.updated_at
                FROM orders o
                """);

        appendWhereClause(sql, selectParams);

        appendOrderBy(sql);
        appendPagination(sql, page, size);

        return sql.toString();
    }

    private void appendWhereClause(StringBuilder sql, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            var placeholders = String.join(", ", "?".repeat(criteria.statuses().size()).split(""));
            conditions.add("o.order_status IN (" + placeholders + ")");
            criteria.statuses().forEach(s -> params.add(s.name()));
        }

        if (criteria.placedFrom() != null) {
            conditions.add("o.placed_at >= ?");
            params.add(LocalDateTime.ofInstant(criteria.placedFrom(), ZoneOffset.UTC));
        }
        if (criteria.placedTo() != null) {
            conditions.add("o.placed_at <= ?");
            params.add(LocalDateTime.ofInstant(criteria.placedTo(), ZoneOffset.UTC));
        }

        if (criteria.customerFirstName() != null && !criteria.customerFirstName().isBlank()) {
            conditions.add("o.customer_first_name = ?");
            params.add(criteria.customerFirstName());
        }

        if (criteria.deliveryMode() != null) {
            conditions.add("o.delivery_mode = ?");
            params.add(criteria.deliveryMode().name());
        }

        if (!conditions.isEmpty()) {
            sql.append("\nWHERE ").append(String.join(" AND ", conditions));
        }
    }

    private void appendOrderBy(StringBuilder sql) {
        var sortBy = criteria.sortBy() != null
                ? criteria.sortBy()
                : OrderPayloads.OrderSortField.PLACED_AT;
        var direction = criteria.sortDirection() != null
                ? criteria.sortDirection()
                : OrderPayloads.SortDirection.DESC;

        sql.append("\nORDER BY ");

        switch (sortBy) {
            case PLACED_AT -> sql.append("o.placed_at");
            case UPDATED_AT -> sql.append("o.updated_at");
            case DELIVERY_MODE -> sql.append("o.delivery_mode");
            case STATUS -> sql.append("o.order_status");
        }

        sql.append(" ").append(direction.name());
    }

    private void appendPagination(StringBuilder sql, int page, int size) {
        sql.append("\nLIMIT ? OFFSET ?");
        selectParams.add(size);
        selectParams.add(page * size);
    }

}

