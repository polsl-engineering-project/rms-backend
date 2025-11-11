package com.polsl.engineering.project.rms.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, UUID> {
    
    Slice<OrderOutboxEvent> findByProcessedIsFalse(Pageable pageable);

    @Modifying
    void deleteByProcessedIsTrue();
    
}

