package com.polsl.engineering.project.rms.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, UUID> {
    // Additional query methods can be added later (e.g. findByOrderId, findTopN etc.)
}

