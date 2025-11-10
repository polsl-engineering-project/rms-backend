package com.polsl.engineering.project.rms.bill;

import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Getter
class BillQueryBuilder {

    private final BillPayloads.BillSearchRequest criteria;
    private final List<Object> selectParams = new ArrayList<>();
    private final List<Object> countParams = new ArrayList<>();

    BillQueryBuilder(BillPayloads.BillSearchRequest criteria) {
        this.criteria = criteria;
    }

    String buildCountQuery() {
        var sql = new StringBuilder();
        sql.append("""
                SELECT COUNT(DISTINCT b.id)
                FROM bills b
                """);

        appendJoins(sql);
        appendWhereClause(sql, countParams);

        return sql.toString();
    }

    String buildSelectQuery(int page, int size) {
        var sql = new StringBuilder();
        sql.append("""
                SELECT 
                    b.id,
                    b.table_number,
                    b.bill_status,
                    b.payment_method,
                    b.waiter_first_name,
                    b.waiter_last_name,
                    b.waiter_employee_id,
                    b.total_amount,
                    b.paid_amount,
                    b.opened_at,
                    b.closed_at,
                    b.paid_at,
                    b.updated_at,
                    COALESCE(COUNT(bl.id), 0) as item_count
                FROM bills b
                LEFT JOIN bill_lines bl ON b.id = bl.bill_id
                """);

        appendWhereClause(sql, selectParams);

        sql.append("\nGROUP BY b.id, b.table_number, b.bill_status, b.payment_method, b.waiter_first_name, ")
                .append("b.waiter_last_name, b.waiter_employee_id, b.total_amount, b.paid_amount, ")
                .append("b.opened_at, b.closed_at, b.paid_at, b.updated_at");

        appendOrderBy(sql);
        appendPagination(sql, page, size);

        return sql.toString();
    }

    private void appendJoins(StringBuilder sql) {
        if (criteria.menuItemId() != null && !criteria.menuItemId().isBlank()) {
            sql.append("INNER JOIN bill_lines bl ON b.id = bl.bill_id\n");
        }
    }

    private void appendWhereClause(StringBuilder sql, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            var placeholders = String.join(", ", "?".repeat(criteria.statuses().size()).split(""));
            conditions.add("b.bill_status IN (" + placeholders + ")");
            criteria.statuses().forEach(status -> params.add(status.name()));
        }

        if (criteria.paymentMethod() != null) {
            conditions.add("b.payment_method = ?");
            params.add(criteria.paymentMethod().name());
        }

        if (criteria.openedFrom() != null) {
            conditions.add("b.opened_at >= ?");
            params.add(LocalDateTime.ofInstant(criteria.openedFrom(), ZoneOffset.UTC));
        }
        if (criteria.openedTo() != null) {
            conditions.add("b.opened_at <= ?");
            params.add(LocalDateTime.ofInstant(criteria.openedTo(), ZoneOffset.UTC));
        }

        if (criteria.closedFrom() != null) {
            conditions.add("b.closed_at >= ?");
            params.add(LocalDateTime.ofInstant(criteria.closedFrom(), ZoneOffset.UTC));
        }
        if (criteria.closedTo() != null) {
            conditions.add("b.closed_at <= ?");
            params.add(LocalDateTime.ofInstant(criteria.closedTo(), ZoneOffset.UTC));
        }

        if (criteria.paidFrom() != null) {
            conditions.add("b.paid_at >= ?");
            params.add(LocalDateTime.ofInstant(criteria.paidFrom(), ZoneOffset.UTC));
        }
        if (criteria.paidTo() != null) {
            conditions.add("b.paid_at <= ?");
            params.add(LocalDateTime.ofInstant(criteria.paidTo(), ZoneOffset.UTC));
        }

        if (criteria.waiterEmployeeId() != null && !criteria.waiterEmployeeId().isBlank()) {
            conditions.add("b.waiter_employee_id = ?");
            params.add(criteria.waiterEmployeeId());
        }
        if (criteria.waiterFirstName() != null && !criteria.waiterFirstName().isBlank()) {
            conditions.add("LOWER(b.waiter_first_name) LIKE ?");
            params.add("%" + criteria.waiterFirstName().toLowerCase() + "%");
        }
        if (criteria.waiterLastName() != null && !criteria.waiterLastName().isBlank()) {
            conditions.add("LOWER(b.waiter_last_name) LIKE ?");
            params.add("%" + criteria.waiterLastName().toLowerCase() + "%");
        }

        if (criteria.tableNumber() != null) {
            conditions.add("b.table_number = ?");
            params.add(criteria.tableNumber());
        }
        if (criteria.tableNumbers() != null && !criteria.tableNumbers().isEmpty()) {
            var placeholders = String.join(", ", "?".repeat(criteria.tableNumbers().size()).split(""));
            conditions.add("b.table_number IN (" + placeholders + ")");
            criteria.tableNumbers().forEach(params::add);
        }

        if (criteria.minTotalAmount() != null) {
            conditions.add("b.total_amount >= ?");
            params.add(criteria.minTotalAmount());
        }
        if (criteria.maxTotalAmount() != null) {
            conditions.add("b.total_amount <= ?");
            params.add(criteria.maxTotalAmount());
        }

        if (criteria.minPaidAmount() != null) {
            conditions.add("b.paid_amount >= ?");
            params.add(criteria.minPaidAmount());
        }
        if (criteria.maxPaidAmount() != null) {
            conditions.add("b.paid_amount <= ?");
            params.add(criteria.maxPaidAmount());
        }

        if (criteria.menuItemId() != null && !criteria.menuItemId().isBlank()) {
            conditions.add("bl.menu_item_id = ?");
            params.add(criteria.menuItemId());
        }

        if (!conditions.isEmpty()) {
            sql.append("\nWHERE ").append(String.join(" AND ", conditions));
        }
    }

    private void appendOrderBy(StringBuilder sql) {
        var sortBy = criteria.sortBy() != null
                ? criteria.sortBy()
                : BillPayloads.BillSortField.OPENED_AT;
        var direction = criteria.sortDirection() != null
                ? criteria.sortDirection()
                : BillPayloads.SortDirection.DESC;

        sql.append("\nORDER BY ");

        switch (sortBy) {
            case OPENED_AT -> sql.append("b.opened_at");
            case CLOSED_AT -> sql.append("b.closed_at");
            case PAID_AT ->  sql.append("b.paid_at");
            case UPDATED_AT -> sql.append("b.updated_at");
            case TOTAL_AMOUNT -> sql.append("b.total_amount");
            case PAID_AMOUNT -> sql.append("b.paid_amount");
            case TABLE_NUMBER -> sql.append("b.table_number");
            case WAITER_LAST_NAME -> sql.append("b.waiter_last_name, b.waiter_first_name");
        }

        sql.append(" ").append(direction.name());
    }

    private void appendPagination(StringBuilder sql, int page, int size) {
        sql.append("\nLIMIT ? OFFSET ?");
        selectParams.add(size);
        selectParams.add(page * size);
    }

}