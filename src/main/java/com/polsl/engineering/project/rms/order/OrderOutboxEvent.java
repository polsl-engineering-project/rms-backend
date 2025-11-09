package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.event.OrderEventType;
import com.polsl.engineering.project.rms.order.vo.OrderId;
import jakarta.persistence.*;
import jdk.jfr.EventType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "order_outbox_events", indexes = {
        @Index(name = "idx_outbox_orderid", columnList = "order_id"),
        @Index(name = "idx_outbox_type_created", columnList = "type, created_at")
})
public class OrderOutboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private OrderEventType type;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter(AccessLevel.PACKAGE)
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    protected OrderOutboxEvent() {
        // for JPA
    }

    OrderOutboxEvent(UUID id, UUID orderId, OrderEventType type, String payload, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static OrderOutboxEvent of(OrderId orderId, OrderEventType type, String payload) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        return new OrderOutboxEvent(UUID.randomUUID(), orderId.value(), type, payload, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderOutboxEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

