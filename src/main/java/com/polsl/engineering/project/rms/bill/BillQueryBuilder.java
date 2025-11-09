package com.polsl.engineering.project.rms.bill;

import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Getter
class BillQueryBuilder {

    private final BillPayloads.BillSearchCriteria criteria;
    private final List<Object> params = new ArrayList<>();

    BillQueryBuilder(BillPayloads.BillSearchCriteria criteria) {
        this.criteria = criteria;
    }

    String buildCountQuery() {
        var sql = new StringBuilder();
        sql.append("""
                SELECT COUNT(DISTINCT b.id)
                FROM bills b
                """);

        appendJoins(sql);
        appendWhereClause(sql);

        return sql.toString();
    }

    String buildSelectQuery(int page, int size) {
        var sql = new StringBuilder();
        sql.append("""
                SELECT 
                    b.id,
                    b.table_number,
                    b.bill_status,
                    b.waiter_first_name,
                    b.waiter_last_name,
                    b.waiter_employee_id,
                    b.total_amount,
                    b.opened_at,
                    b.closed_at,
                    b.updated_at,
                    COALESCE(COUNT(bl.id), 0) as item_count
                FROM bills b
                LEFT JOIN bill_lines bl ON b.id = bl.bill_id
                """);

        appendWhereClause(sql);

        sql.append("\nGROUP BY b.id, b.table_number, b.bill_status, b.waiter_first_name, ")
                .append("b.waiter_last_name, b.waiter_employee_id, b.total_amount, ")
                .append("b.opened_at, b.closed_at, b.updated_at");

        appendOrderBy(sql);
        appendPagination(sql, page, size);

        return sql.toString();
    }

    private void appendJoins(StringBuilder sql) {
        if (criteria.getMenuItemId() != null && !criteria.getMenuItemId().isBlank()) {
            sql.append("INNER JOIN bill_lines bl ON b.id = bl.bill_id\n");
        }
    }

    private void appendWhereClause(StringBuilder sql) {
        List<String> conditions = new ArrayList<>();

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            var placeholders = String.join(", ", "?".repeat(criteria.getStatuses().size()).split(""));
            conditions.add("b.bill_status IN (" + placeholders + ")");
            criteria.getStatuses().forEach(status -> params.add(status.name()));
        }

        if (criteria.getOpenedFrom() != null) {
            conditions.add("b.opened_at >= ?");
            params.add(toTimestamp(criteria.getOpenedFrom(), true));
        }
        if (criteria.getOpenedTo() != null) {
            conditions.add("b.opened_at < ?");
            params.add(toTimestamp(criteria.getOpenedTo().plusDays(1), true));
        }

        if (criteria.getOpenedAfter() != null) {
            conditions.add("b.opened_at >= ?");
            params.add(Timestamp.from(criteria.getOpenedAfter()));
        }
        if (criteria.getOpenedBefore() != null) {
            conditions.add("b.opened_at < ?");
            params.add(Timestamp.from(criteria.getOpenedBefore()));
        }

        if (criteria.getClosedFrom() != null) {
            conditions.add("b.closed_at >= ?");
            params.add(toTimestamp(criteria.getClosedFrom(), true));
        }
        if (criteria.getClosedTo() != null) {
            conditions.add("b.closed_at < ?");
            params.add(toTimestamp(criteria.getClosedTo().plusDays(1), true));
        }

        if (criteria.getClosedAfter() != null) {
            conditions.add("b.closed_at >= ?");
            params.add(Timestamp.from(criteria.getClosedAfter()));
        }
        if (criteria.getClosedBefore() != null) {
            conditions.add("b.closed_at < ?");
            params.add(Timestamp.from(criteria.getClosedBefore()));
        }

        if (criteria.getWaiterEmployeeId() != null && !criteria.getWaiterEmployeeId().isBlank()) {
            conditions.add("b.waiter_employee_id = ?");
            params.add(criteria.getWaiterEmployeeId());
        }
        if (criteria.getWaiterFirstName() != null && !criteria.getWaiterFirstName().isBlank()) {
            conditions.add("LOWER(b.waiter_first_name) LIKE ?");
            params.add("%" + criteria.getWaiterFirstName().toLowerCase() + "%");
        }
        if (criteria.getWaiterLastName() != null && !criteria.getWaiterLastName().isBlank()) {
            conditions.add("LOWER(b.waiter_last_name) LIKE ?");
            params.add("%" + criteria.getWaiterLastName().toLowerCase() + "%");
        }

        if (criteria.getTableNumber() != null) {
            conditions.add("b.table_number = ?");
            params.add(criteria.getTableNumber());
        }
        if (criteria.getTableNumbers() != null && !criteria.getTableNumbers().isEmpty()) {
            var placeholders = String.join(", ", "?".repeat(criteria.getTableNumbers().size()).split(""));
            conditions.add("b.table_number IN (" + placeholders + ")");
            criteria.getTableNumbers().forEach(params::add);
        }

        if (criteria.getMinTotalAmount() != null) {
            conditions.add("b.total_amount >= ?");
            params.add(criteria.getMinTotalAmount());
        }
        if (criteria.getMaxTotalAmount() != null) {
            conditions.add("b.total_amount <= ?");
            params.add(criteria.getMaxTotalAmount());
        }

        if (criteria.getMenuItemId() != null && !criteria.getMenuItemId().isBlank()) {
            conditions.add("bl.menu_item_id = ?");
            params.add(criteria.getMenuItemId());
        }

        if (criteria.getSearchText() != null && !criteria.getSearchText().isBlank()) {
            conditions.add("(LOWER(b.waiter_first_name) LIKE ? OR LOWER(b.waiter_last_name) LIKE ?)");
            String searchPattern = "%" + criteria.getSearchText().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (!conditions.isEmpty()) {
            sql.append("\nWHERE ").append(String.join(" AND ", conditions));
        }
    }

    private void appendOrderBy(StringBuilder sql) {
        var sortBy = criteria.getSortBy() != null
                ? criteria.getSortBy()
                : BillPayloads.BillSortField.OPENED_AT;
        var direction = criteria.getSortDirection() != null
                ? criteria.getSortDirection()
                : BillPayloads.SortDirection.DESC;

        sql.append("\nORDER BY ");

        switch (sortBy) {
            case OPENED_AT -> sql.append("b.opened_at");
            case CLOSED_AT -> sql.append("b.closed_at");
            case UPDATED_AT -> sql.append("b.updated_at");
            case TOTAL_AMOUNT -> sql.append("b.total_amount");
            case TABLE_NUMBER -> sql.append("b.table_number");
            case WAITER_LAST_NAME -> sql.append("b.waiter_last_name, b.waiter_first_name");
        }

        sql.append(" ").append(direction.name());
    }

    private void appendPagination(StringBuilder sql, int page, int size) {
        sql.append("\nLIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);
    }

    private Timestamp toTimestamp(LocalDate date, boolean startOfDay) {
        var instant = startOfDay
                ? date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Timestamp.from(instant);
    }
}