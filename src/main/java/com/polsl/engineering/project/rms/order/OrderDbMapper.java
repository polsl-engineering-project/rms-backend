package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.Address;
import com.polsl.engineering.project.rms.order.vo.CustomerInfo;
import com.polsl.engineering.project.rms.order.vo.Money;
import com.polsl.engineering.project.rms.order.vo.OrderLine;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
class OrderDbMapper {

    CustomerInfo mapCustomerInfo(ResultSet rs) {
        try {
            var firstName = rs.getString("customer_first_name");
            var lastName = rs.getString("customer_last_name");
            var phone = rs.getString("customer_phone_number");
            return new CustomerInfo(firstName, lastName, phone);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map CustomerInfo from ResultSet", e);
        }
    }

    Address mapDeliveryAddress(ResultSet rs) {
        try {
            var street = rs.getString("delivery_street");
            if (street == null) return null;
            var houseNumber = rs.getString("delivery_house_number");
            var apartmentNumber = rs.getString("delivery_apartment_number");
            var city = rs.getString("delivery_city");
            var postalCode = rs.getString("delivery_postal_code");
            return new Address(street, houseNumber, apartmentNumber, city, postalCode);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map Address from ResultSet", e);
        }
    }

    LocalTime mapScheduledFor(ResultSet rs) {
        try {
            Time scheduledTime = rs.getTime("scheduled_for");
            return scheduledTime == null ? null : scheduledTime.toLocalTime();
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map scheduled_for from ResultSet", e);
        }
    }

    Instant mapInstant(ResultSet rs, String column) {
        try {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toInstant();
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map timestamp column '" + column + "' from ResultSet", e);
        }
    }

    LocalDateTime mapLocalDateTime(ResultSet rs, String column) {
        try {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toLocalDateTime();
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map timestamp column '" + column + "' from ResultSet", e);
        }
    }

    Integer mapNullableInteger(ResultSet rs, String column) {
        try {
            return rs.getObject(column, Integer.class);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map integer column '" + column + "' from ResultSet", e);
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

    OrderLine mapOrderLine(ResultSet rs) {
        try {
            var menuItemId = rs.getString("menu_item_id");
            var menuItemName = rs.getString("menu_item_name");
            var quantity = rs.getInt("quantity");
            var unitPrice = mapMoney(rs, "unit_price");
            return new OrderLine(menuItemId, quantity, unitPrice, menuItemName);
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("Failed to map OrderLine from ResultSet", e);
        }
    }
}
