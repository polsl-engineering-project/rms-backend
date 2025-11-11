package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.vo.BillLine;
import com.polsl.engineering.project.rms.bill.vo.Money;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

@Component
class BillDbMapper {

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
            var amount = rs.getBigDecimal(column);
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
            var menuItemName = rs.getString("menu_item_name");
            var menuItemVersion = rs.getLong("menu_item_version");
            return new BillLine(menuItemId, quantity, unitPrice, menuItemName, menuItemVersion);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map BillLine from ResultSet", e);
        }
    }
}