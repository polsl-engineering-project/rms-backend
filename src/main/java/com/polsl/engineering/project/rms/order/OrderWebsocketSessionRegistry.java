package com.polsl.engineering.project.rms.order;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
class OrderWebsocketSessionRegistry {

    @Getter
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    void registerSession(WebSocketSession session) {
        sessions.add(session);
    }

    void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
    }

}
