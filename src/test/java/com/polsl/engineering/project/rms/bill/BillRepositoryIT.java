package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import com.polsl.engineering.project.rms.bill.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({BillRepository.class, BillDbMapper.class, BillQueryParamsBuilder.class})
@DisplayName("Integration tests for BillRepository")
class BillRepositoryIT extends ContainersEnvironment {

    @Autowired
    BillRepository underTest;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        var populator = new ResourceDatabasePopulator(new ClassPathResource("init/sql/bill.sql"));
        populator.execute(dataSource);
    }

    @Test
    @DisplayName("Given existing bill id, When findById, Then returns bill with expected fields and lines")
    void GivenExistingBillId_WhenFindById_ThenReturnBill() {
        // given
        var billId = BillId.from("00000000-0000-0000-0000-000000000001");

        // when
        var optBill = underTest.findById(billId);

        // then
        assertThat(optBill).isPresent();
        var bill = optBill.get();

        assertThat(bill.getId()).isEqualTo(billId);
        assertThat(bill.getTableNumber().value()).isEqualTo(5);
        assertThat(bill.getStatus()).isEqualTo(BillStatus.OPEN);

        assertThat(bill.getUserId()).isEqualTo("W123");

        assertThat(bill.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("72.00"));

        var lines = bill.getLines();
        assertThat(lines).hasSize(2);
        assertThat(lines).extracting(BillLine::menuItemId).containsExactlyInAnyOrder("pizza", "salad");

        var pizzaLine = lines.stream().filter(l -> l.menuItemId().equals("pizza")).findFirst().orElseThrow();
        assertThat(pizzaLine.quantity()).isEqualTo(2);
        assertThat(pizzaLine.unitPrice().amount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(pizzaLine.menuItemName()).isEqualTo("Pizza");

        var saladLine = lines.stream().filter(l -> l.menuItemId().equals("salad")).findFirst().orElseThrow();
        assertThat(saladLine.quantity()).isEqualTo(1);
        assertThat(saladLine.unitPrice().amount()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("Given non-existing bill id, When findById, Then returns empty optional")
    void GivenNonExistingBillId_WhenFindById_ThenReturnEmptyOptional() {
        // given
        var billId = BillId.from("00000000-0000-0000-0000-000000000099");

        // when
        var optBill = underTest.findById(billId);

        // then
        assertThat(optBill).isEmpty();
    }

    @Test
    @DisplayName("Given table with open bill, When openBillExistsForTable, Then returns true")
    void GivenTableWithOpenBill_WhenOpenBillExistsForTable_ThenReturnsTrue() {
        // given
        var tableNumber = 5;

        // when
        var exists = underTest.openBillExistsForTable(tableNumber);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Given table without open bill, When openBillExistsForTable, Then returns false")
    void GivenTableWithoutOpenBill_WhenOpenBillExistsForTable_ThenReturnsFalse() {
        // given
        var tableNumber = 100;

        // when
        var exists = underTest.openBillExistsForTable(tableNumber);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Given table with closed bill, When openBillExistsForTable, Then returns false")
    void GivenTableWithClosedBill_WhenOpenBillExistsForTable_ThenReturnsFalse() {
        // given
        var tableNumber = 7; // has closed bill

        // when
        var exists = underTest.openBillExistsForTable(tableNumber);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Given table with open bill, When openBillExistsForTable, Then returns true")
    void GivenTableWithOpenedBill_WhenOpenBillExistsForTable_ThenReturnsTrue() {
        // given
        var tableNumber = 5; // has opened bill

        // when
        var exists = underTest.openBillExistsForTable(tableNumber);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Given new bill, When saveNewBill, Then row is persisted in bills and bill_lines")
    void GivenNewBill_WhenSaveNewBill_ThenPersistedInDb() {
        // given
        var lines = List.of(
                new BillLine("soup", 2, new Money(new BigDecimal("8.00")), "Soup"),
                new BillLine("bread", 1, new Money(new BigDecimal("3.00")), "Bread")
        );
        var userId = UUID.randomUUID().toString();
        var cmd = new com.polsl.engineering.project.rms.bill.cmd.OpenBillCommand(
                TableNumber.of(15),
                userId,
                lines
        );

        var result = Bill.open(cmd, Clock.systemUTC());
        assertThat(result.isSuccess()).isTrue();
        var bill = result.getValue();

        // when
        underTest.saveNewBill(bill);

        // then
        var status = jdbcTemplate.queryForObject(
                "SELECT bill_status FROM bills WHERE id = ?",
                String.class,
                bill.getId().value()
        );
        assertThat(status).isEqualTo("OPEN");

        var tableNumber = jdbcTemplate.queryForObject(
                "SELECT table_number FROM bills WHERE id = ?",
                Integer.class,
                bill.getId().value()
        );
        assertThat(tableNumber).isEqualTo(15);

        var dbUserId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM bills WHERE id = ?",
                String.class,
                bill.getId().value()
        );
        assertThat(dbUserId).isEqualTo(userId);

        List<Map<String, Object>> linesFromDb = jdbcTemplate.queryForList(
                "SELECT menu_item_id, quantity, unit_price FROM bill_lines WHERE bill_id = ?",
                bill.getId().value()
        );
        assertThat(linesFromDb).hasSize(2);
        assertThat(linesFromDb)
                .anyMatch(m -> "soup".equals(m.get("menu_item_id")) && ((Number) m.get("quantity")).intValue() == 2)
                .anyMatch(m -> "bread".equals(m.get("menu_item_id")) && ((Number) m.get("quantity")).intValue() == 1);
    }

    @Test
    @DisplayName("Given existing bill, When updateWithoutLines, Then updates fields and increments version without touching lines")
    void GivenExistingBill_WhenUpdateWithoutLines_ThenUpdatesFieldsAndIncrementsVersion() {
        // given
        var billId = BillId.from("00000000-0000-0000-0000-000000000001");
        var optBill = underTest.findById(billId);
        assertThat(optBill).isPresent();
        var bill = optBill.get();
        var originalVersion = bill.getVersion();
        assertThat(originalVersion).isZero();

        // when
        var closeResult = bill.close(Clock.systemUTC());
        assertThat(closeResult.isSuccess()).isTrue();
        underTest.updateWithoutLines(bill);

        // then
        var status = jdbcTemplate.queryForObject(
                "SELECT bill_status FROM bills WHERE id = ?",
                String.class,
                billId.value()
        );
        assertThat(status).isEqualTo("CLOSED");

        var version = jdbcTemplate.queryForObject(
                "SELECT version FROM bills WHERE id = ?",
                Long.class,
                billId.value()
        );
        assertThat(version).isEqualTo(originalVersion + 1);

        var linesCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bill_lines WHERE bill_id = ?",
                Long.class,
                billId.value()
        );
        assertThat(linesCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("Given concurrent stale bill, When updateWithoutLines, Then throws OptimisticLockingFailureException")
    void GivenStaleBill_WhenUpdateWithoutLines_ThenThrowsOptimisticLockingFailureException() {
        // given
        var billId = BillId.from("00000000-0000-0000-0000-000000000001");
        var optBillA = underTest.findById(billId);
        var optBillB = underTest.findById(billId);
        assertThat(optBillA).isPresent();
        assertThat(optBillB).isPresent();
        var billA = optBillA.get();
        var billB = optBillB.get();

        // when
        assertThat(billA.close(Clock.systemUTC()).isSuccess()).isTrue();
        underTest.updateWithoutLines(billA);

        // then
        assertThat(billB.close(Clock.systemUTC()).isSuccess()).isTrue();
        assertThatThrownBy(() -> underTest.updateWithoutLines(billB))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Given existing bill, When updateWithLines, Then replaces lines and increments version")
    void GivenExistingBill_WhenUpdateWithLines_ThenReplacesLinesAndIncrementsVersion() {
        // given
        var billId = BillId.from("00000000-0000-0000-0000-000000000001");
        var optBill = underTest.findById(billId);
        assertThat(optBill).isPresent();
        var bill = optBill.get();
        var originalVersion = bill.getVersion();
        assertThat(originalVersion).isZero();

        var newLines = List.of(new BillLine("tea", 3, new Money(new BigDecimal("5.00")), "Tea"));
        var addCmd = new com.polsl.engineering.project.rms.bill.cmd.AddItemsToBillCommand(newLines);
        var addResult = bill.addItems(addCmd, Clock.systemUTC());
        assertThat(addResult.isSuccess()).isTrue();

        // when
        underTest.updateWithLines(bill);

        // then
        var version = jdbcTemplate.queryForObject(
                "SELECT version FROM bills WHERE id = ?",
                Long.class,
                billId.value()
        );
        assertThat(version).isEqualTo(originalVersion + 1);

        List<Map<String, Object>> linesFromDb = jdbcTemplate.queryForList(
                "SELECT menu_item_id, quantity FROM bill_lines WHERE bill_id = ? ORDER BY menu_item_id",
                billId.value()
        );
        assertThat(linesFromDb).hasSize(3);
        assertThat(linesFromDb)
                .anyMatch(m -> "pizza".equals(m.get("menu_item_id")))
                .anyMatch(m -> "salad".equals(m.get("menu_item_id")))
                .anyMatch(m -> "tea".equals(m.get("menu_item_id")) && ((Number) m.get("quantity")).intValue() == 3);
    }

    @Test
    @DisplayName("Given concurrent stale bill, When updateWithLines, Then throws OptimisticLockingFailureException")
    void GivenStaleBill_WhenUpdateWithLines_ThenThrowsOptimisticLockingFailureException() {
        // given
        var billId = BillId.from("00000000-0000-0000-0000-000000000001");
        var optA = underTest.findById(billId);
        var optB = underTest.findById(billId);
        assertThat(optA).isPresent();
        assertThat(optB).isPresent();
        var billA = optA.get();
        var billB = optB.get();

        var newLinesA = List.of(new BillLine("item-a", 1, new Money(new BigDecimal("10.00")), "ItemA"));
        var newLinesB = List.of(new BillLine("item-b", 1, new Money(new BigDecimal("11.00")), "ItemB"));

        var addCmdA = new com.polsl.engineering.project.rms.bill.cmd.AddItemsToBillCommand(newLinesA);
        assertThat(billA.addItems(addCmdA, Clock.systemUTC()).isSuccess()).isTrue();
        underTest.updateWithLines(billA);

        var addCmdB = new com.polsl.engineering.project.rms.bill.cmd.AddItemsToBillCommand(newLinesB);
        assertThat(billB.addItems(addCmdB, Clock.systemUTC()).isSuccess()).isTrue();

        // then
        assertThatThrownBy(() -> underTest.updateWithLines(billB))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Given search criteria, When searchBills, Then returns matching bills with pagination")
    void GivenSearchCriteria_WhenSearchBills_ThenReturnsMatchingBills() {
        // given
        var criteria = BillPayloads.BillSearchRequest.builder()
                .statuses(List.of(BillStatus.OPEN))
                .page(0)
                .size(10)
                .build();

        // when
        var result = underTest.searchBills(criteria, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).status()).isEqualTo(BillStatus.OPEN);
        assertThat(result.content().get(0).tableNumber()).isEqualTo(5);
    }

    @Test
    @DisplayName("Given various bills_When findOpenBills_Then returns only open bills with lines")
    void GivenVariousBills_WhenFindOpenBills_ThenReturnsOnlyOpenBillsWithLines() {
        // when
        var activeOrders = underTest.findOpenBills();

        // then
        assertThat(activeOrders)
                .isNotEmpty()
                .allMatch(o -> o.getStatus().equals(BillStatus.OPEN));

        assertThat(activeOrders).extracting(Bill::getId)
                .containsExactlyInAnyOrder(
                        BillId.from("00000000-0000-0000-0000-000000000001")
                );

        activeOrders.forEach(o -> assertThat(o.getLines()).isNotEmpty());
    }
}