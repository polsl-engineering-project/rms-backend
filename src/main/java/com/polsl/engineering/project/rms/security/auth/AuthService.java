package com.polsl.engineering.project.rms.security.auth;
import com.polsl.engineering.project.rms.security.auth.dto.LoginRequest;
import com.polsl.engineering.project.rms.security.auth.dto.TokenPair;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    TokenPair login(LoginRequest request, HttpServletRequest httpRequest) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        var tokens = jwtService.createTokens(authentication, request.username(), extractDeviceInfo(httpRequest), extractIpAddress(httpRequest));
        return new TokenPair(tokens.accessToken(), tokens.refreshToken());
    }

    public TokenPair refreshTokens(String rawToken, HttpServletRequest httpRequest) {
        return jwtService.refreshTokens(rawToken, extractDeviceInfo(httpRequest), extractIpAddress(httpRequest));
    }

    public void revokeToken(String rawToken){
        jwtService.revokeToken(rawToken);
    }

    public void revokeAllUserTokens(UUID userId) {
        jwtService.revokeAllUserTokens(userId);
    }


    private String extractDeviceInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String extractIpAddress(HttpServletRequest request) {
        var ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
