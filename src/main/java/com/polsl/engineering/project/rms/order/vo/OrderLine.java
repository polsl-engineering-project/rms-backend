package com.polsl.engineering.project.rms.order.vo;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Objects;

@Getter
@Entity(name = "OrderLine")
@Table(name = "order_lines")
public class OrderLine {

    @Getter(AccessLevel.NONE)
    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id"))
    private OrderLineId id;

    @Column(name = "menu_item_id", nullable = false)
    private String menuItemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price"))
    private Money unitPrice;

    @Column(name = "menu_item_version", nullable = false)
    private long menuItemVersion;

    // JPA requires a default constructor
    protected OrderLine() {
    }

    public OrderLine(String menuItemId, int quantity, Money unitPrice, long menuItemVersion) {
        this.id = OrderLineId.generate();
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.menuItemVersion = menuItemVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OrderLine orderLine)) return false;
        return Objects.equals(id, orderLine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
