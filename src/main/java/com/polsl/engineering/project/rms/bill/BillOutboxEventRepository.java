package com.polsl.engineering.project.rms.bill;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface BillOutboxEventRepository extends JpaRepository<BillOutboxEvent, UUID> {

    Slice<BillOutboxEvent> findByProcessedIsFalse(Pageable pageable);

    @Modifying
    void deleteByProcessedIsTrue();

}

