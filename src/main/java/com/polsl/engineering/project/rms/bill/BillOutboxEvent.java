package com.polsl.engineering.project.rms.bill;

import com.polsl.engineering.project.rms.bill.event.BillEventType;
import com.polsl.engineering.project.rms.bill.vo.BillId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "bill_outbox_events", indexes = {
        @Index(name = "idx_outbox_billid", columnList = "bill_id"),
        @Index(name = "idx_outbox_bill_type_created", columnList = "type, created_at")
})
class BillOutboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "bill_id", nullable = false, updatable = false)
    private UUID billId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private BillEventType type;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter(AccessLevel.PACKAGE)
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    protected BillOutboxEvent() {
    }

    BillOutboxEvent(UUID id, UUID billId, BillEventType type, String payload, Instant createdAt) {
        this.id = id;
        this.billId = billId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static BillOutboxEvent of(BillId billId, BillEventType type, String payload) {
        Objects.requireNonNull(billId, "orderId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        return new BillOutboxEvent(UUID.randomUUID(), billId.value(), type, payload, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillOutboxEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

