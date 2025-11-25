package com.polsl.engineering.project.rms.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import com.polsl.engineering.project.rms.security.jwt.JwtVerificationFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {

        String token = extractTokenFromQuery(request);

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket connection attempt without token");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        try {
            var userPrincipal = jwtService.parseJwt(token);
            attributes.put("userPrincipal", userPrincipal);
            attributes.put("authenticated", true);
            return true;
        } catch (JwtVerificationFailedException e) {
            log.error("Invalid JWT token in WebSocket connection", e);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String extractTokenFromQuery(ServerHttpRequest request) {
        var uri = request.getURI();
        var queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        var tokenList = queryParams.get("token");

        if (tokenList != null && !tokenList.isEmpty()) {
            return tokenList.getFirst();
        }

        return null;
    }
}