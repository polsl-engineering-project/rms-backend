package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.vo.*;
import com.polsl.engineering.project.rms.general.db.QueryLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Repository
class BillRepository {

    private final JdbcTemplate jdbcTemplate;
    private final BillDbMapper dbMapper;
    private final BillQueryParamsBuilder paramsBuilder;

    private static final String BILL_INSERT_SQL = """
            INSERT INTO bills (
                id,
                table_number,
                bill_status,
                user_id,
                total_amount,
                opened_at,
                closed_at,
                updated_at,
                version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String BILL_UPDATE_SQL = """
            UPDATE bills
            SET
                table_number = ?,
                bill_status = ?,
                user_id = ?,
                total_amount = ?,
                opened_at = ?,
                closed_at = ?,
                updated_at = ?,
                version = version + 1
            WHERE id = ? AND version = ?
            """;

    private static final String BILL_LINES_INSERT_SQL = """
            INSERT INTO bill_lines (
                id,
                bill_id,
                menu_item_id,
                quantity,
                unit_price,
                menu_item_name
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String BILL_LINES_DELETE_SQL = """
            DELETE FROM bill_lines
            WHERE bill_id = ?
            """;

    private static final String BILL_LINES_FIND_ALL_OPENED = """
            SELECT * FROM bills where bill_status = 'OPEN'
            """;

    Optional<Bill> findById(BillId billId) {
        final var billSql = """
                SELECT *
                FROM bills
                WHERE id = ?
                """;

        try {
            QueryLogging.logSql(log, QueryLogging.KIND_QUERY, billSql, billId.value());
            Bill bill = jdbcTemplate.queryForObject(billSql, (rs, _) -> {
                var id = rs.getObject("id", UUID.class);
                var idVo = new BillId(id);
                var tableNumber = TableNumber.of(rs.getInt("table_number"));
                var status = BillStatus.valueOf(rs.getString("bill_status"));
                var userId = rs.getString("user_id");
                var totalAmount = dbMapper.mapMoney(rs, "total_amount");
                var openedAt = dbMapper.mapInstant(rs, "opened_at");
                var closedAt = dbMapper.mapInstant(rs, "closed_at");
                var updatedAt = dbMapper.mapInstant(rs, "updated_at");
                var version = rs.getLong("version");
                var lines = loadBillLines(id);

                return Bill.reconstruct(
                        idVo,
                        tableNumber,
                        status,
                        lines,
                        userId,
                        totalAmount,
                        openedAt,
                        closedAt,
                        updatedAt,
                        version
                );
            }, billId.value());

            return Optional.ofNullable(bill);
        } catch (EmptyResultDataAccessException _) {
            return Optional.empty();
        }
    }

    List<Bill> findOpenBills() {
        QueryLogging.logSql(log, QueryLogging.KIND_QUERY, BILL_LINES_FIND_ALL_OPENED);

        return jdbcTemplate.query(BILL_LINES_FIND_ALL_OPENED, (rs, rowNum) -> {
            var id = rs.getObject("id", UUID.class);
            var idVo = new BillId(id);
            var tableNumber = TableNumber.of(rs.getInt("table_number"));
            var status = BillStatus.valueOf(rs.getString("bill_status"));
            var userId = rs.getString("user_id");
            var totalAmount = dbMapper.mapMoney(rs, "total_amount");
            var openedAt = dbMapper.mapInstant(rs, "opened_at");
            var closedAt = dbMapper.mapInstant(rs, "closed_at");
            var updatedAt = dbMapper.mapInstant(rs, "updated_at");
            var version = rs.getLong("version");
            var lines = loadBillLines(id);

            return Bill.reconstruct(
                    idVo,
                    tableNumber,
                    status,
                    lines,
                    userId,
                    totalAmount,
                    openedAt,
                    closedAt,
                    updatedAt,
                    version
            );
        });
    }

    boolean openBillExistsForTable(Integer tableNumber) {
        final var sql = """
            SELECT EXISTS (
                SELECT 1
                FROM bills
                WHERE table_number = ?
                  AND bill_status = 'OPEN'
            )
            """;

        QueryLogging.logSql(log, QueryLogging.KIND_QUERY, sql, tableNumber);

        var exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableNumber);
        return exists != null && exists;
    }


    @Transactional
    void saveNewBill(Bill bill) {
        var params = paramsBuilder.buildInsertParams(bill);
        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, BILL_INSERT_SQL, params);
        jdbcTemplate.update(BILL_INSERT_SQL, params);
        insertLines(bill.getId().value(), bill.getLines());
    }

    @Transactional
    void updateWithoutLines(Bill bill) {
        var params = paramsBuilder.buildUpdateParams(bill);
        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, BILL_UPDATE_SQL, params);
        var updated = jdbcTemplate.update(BILL_UPDATE_SQL, params);
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Failed to update Bill, optimistic lock lost (id=" + bill.getId().value() + ")");
        }
    }

    @Transactional
    void updateWithLines(Bill bill) {
        var params = paramsBuilder.buildUpdateParams(bill);
        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, BILL_UPDATE_SQL, params);
        var updated = jdbcTemplate.update(BILL_UPDATE_SQL, params);
        if (updated == 0) {
            throw new OptimisticLockingFailureException("Failed to update Bill, optimistic lock lost (id=" + bill.getId().value() + ")");
        }

        QueryLogging.logSql(log, QueryLogging.KIND_UPDATE, BILL_LINES_DELETE_SQL, bill.getId().value());
        jdbcTemplate.update(BILL_LINES_DELETE_SQL, bill.getId().value());
        insertLines(bill.getId().value(), bill.getLines());
    }

    BillPayloads.BillPageResponse searchBills(BillPayloads.BillSearchRequest criteria, int page, int size) {
        var queryBuilder = new BillQueryBuilder(criteria);
        var countSql = queryBuilder.buildCountQuery();
        var selectSql = queryBuilder.buildSelectQuery(page, size);
        var selectParams = queryBuilder.getSelectParams();
        var countParams =  queryBuilder.getCountParams();

        QueryLogging.logSql(log, QueryLogging.KIND_QUERY, countSql, countParams.toArray());
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());

        QueryLogging.logSql(log, QueryLogging.KIND_QUERY, selectSql, selectParams.toArray());
        List<BillPayloads.BillSummaryResponse> content = jdbcTemplate.query(selectSql, (rs, _) -> {
            var id = rs.getObject("id", UUID.class);
            var tableNumber = rs.getInt("table_number");
            var status = BillStatus.valueOf(rs.getString("bill_status"));
            var userId = rs.getString("user_id");
            var totalAmount = dbMapper.mapMoney(rs, "total_amount");
            var itemCount = rs.getInt("item_count");
            var openedAt = dbMapper.mapInstant(rs, "opened_at");
            var closedAt = dbMapper.mapInstant(rs, "closed_at");
            var updatedAt = dbMapper.mapInstant(rs, "updated_at");

            return new BillPayloads.BillSummaryResponse(
                    id,
                    tableNumber,
                    status,
                    userId,
                    totalAmount.amount(),
                    itemCount,
                    openedAt,
                    closedAt,
                    updatedAt
            );
        }, selectParams.toArray());

        var totalPages = (int) Math.ceil((double) total / size);
        var isFirst = page == 0;
        var isLast = page == totalPages;
        var hasPrevious = page > 0;
        var hasNext = page < totalPages - 1;
        return new BillPayloads.BillPageResponse(content, page, size, total, totalPages, isFirst, isLast, hasPrevious, hasNext);
    }

    void insertLines(UUID billId, List<BillLine> lines) {
        if (lines == null || lines.isEmpty()) return;
        var batchArgs = new ArrayList<Object[]>();
        for (var line : lines) {
            var lineId = UUID.randomUUID();
            var unitPrice = line.unitPrice().amount();
            batchArgs.add(new Object[]{lineId, billId, line.menuItemId(), line.quantity(), unitPrice, line.menuItemName()});
        }
        var sample = batchArgs.size() > 3 ? batchArgs.subList(0, 3) : batchArgs;
        QueryLogging.logBatch(log, BILL_LINES_INSERT_SQL, batchArgs.size(), sample);
        jdbcTemplate.batchUpdate(BILL_LINES_INSERT_SQL, batchArgs);
    }

    List<BillLine> loadBillLines(UUID billId) {
        final var linesSql = """
                SELECT *
                FROM bill_lines
                WHERE bill_id = ?
                ORDER BY id
                """;
        QueryLogging.logSql(log, QueryLogging.KIND_QUERY, linesSql, billId);
        return jdbcTemplate.query(linesSql, (r2, _) -> dbMapper.mapBillLine(r2), billId);
    }
}
