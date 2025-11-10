package com.polsl.engineering.project.rms.order;

import com.polsl.engineering.project.rms.order.vo.OrderId;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
class OrderWebsocketSessionRegistry {

    private final Set<WebSocketSession> staffSessions = ConcurrentHashMap.newKeySet();
    private final Map<OrderId, Set<WebSocketSession>> customerSessions = new ConcurrentHashMap<>();

    void registerStaffSession(WebSocketSession session) {
        staffSessions.add(session);
    }

    void unregisterStaffSession(WebSocketSession session) {
        staffSessions.remove(session);
    }

    Set<WebSocketSession> getStaffSessions() {
        return staffSessions;
    }

    void registerCustomerSession(OrderId orderId, WebSocketSession session) {
        customerSessions.computeIfAbsent(orderId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    void unregisterCustomerSession(OrderId orderId, WebSocketSession session) {
        var sessions = customerSessions.get(orderId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                customerSessions.remove(orderId);
            }
        }
    }

    Set<WebSocketSession> getCustomerSessions(OrderId orderId) {
        return customerSessions.getOrDefault(orderId, Set.of());
    }

}
