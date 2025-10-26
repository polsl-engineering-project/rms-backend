package com.polsl.engineering.project.rms.security.auth;

import com.polsl.engineering.project.rms.security.auth.dto.LoginRequest;
import com.polsl.engineering.project.rms.security.auth.dto.LoginResponse;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        var token = jwtService.createJwt(authentication);

        return new LoginResponse(token);
    }

}
