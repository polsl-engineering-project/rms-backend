package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.Money;
import com.polsl.engineering.project.rms.bill.vo.WaiterInfo;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

@Component
class BillDbMapper {

    WaiterInfo mapWaiterInfo(ResultSet rs) {
        try {
            var firstName = rs.getString("waiter_first_name");
            var lastName = rs.getString("waiter_last_name");
            var employeeId = rs.getString("waiter_employee_id");
            return new WaiterInfo(firstName, lastName, employeeId);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map WaiterInfo from ResultSet", e);
        }
    }

    Instant mapInstant(ResultSet rs, String column) {
        try {
            var ts = rs.getTimestamp(column, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            return ts == null ? null : ts.toInstant();
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map timestamp column '" + column + "' from ResultSet", e);
        }
    }

    Money mapMoney(ResultSet rs, String column) {
        try {
            BigDecimal amount = rs.getBigDecimal(column);
            return new Money(amount);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map money column '" + column + "' from ResultSet", e);
        }
    }

    BillLine mapBillLine(ResultSet rs) {
        try {
            var menuItemId = rs.getString("menu_item_id");
            var quantity = rs.getInt("quantity");
            var unitPrice = mapMoney(rs, "unit_price");
            var menuItemVersion = rs.getLong("menu_item_version");
            return new BillLine(menuItemId, quantity, unitPrice, menuItemVersion);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map BillLine from ResultSet", e);
        }
    }
}