package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface OrderRepository extends JpaRepository<Order, OrderId> {
}
