package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_outbox_event", indexes = {
        @Index(name = "idx_outbox_orderid", columnList = "order_id"),
        @Index(name = "idx_outbox_type_created", columnList = "type, created_at")
})
public class OrderOutboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "type", nullable = false, updatable = false)
    private String type;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected OrderOutboxEvent() {
        // for JPA
    }

    OrderOutboxEvent(UUID id, UUID orderId, String type, String payload, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static OrderOutboxEvent of(OrderId orderId, String type, String payload) {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        return new OrderOutboxEvent(UUID.randomUUID(), orderId.value(), type, payload, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderOutboxEvent)) return false;
        OrderOutboxEvent that = (OrderOutboxEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

