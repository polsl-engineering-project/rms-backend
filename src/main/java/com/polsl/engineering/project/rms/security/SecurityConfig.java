package com.polsl.engineering.project.rms.security;

import com.auth0.jwt.algorithms.Algorithm;
import com.polsl.engineering.project.rms.security.jwt.JwtFilter;
import com.polsl.engineering.project.rms.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_WAITER = "WAITER";
    private static final String ROLE_COOK = "COOK";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(x ->
                        x.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Auth endpoints
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        // Swagger / OpenAPI
                        .requestMatchers("/swagger-ui*/**", "/v3/api-docs*/**").permitAll()

                        // Orders - anonymous only (placing orders)
                        .requestMatchers("/api/v1/orders/place-pick-up-order", "/api/v1/orders/place-delivery-order").anonymous()

                        // Orders - role based
                        .requestMatchers("/api/v1/orders/*/approve/front-desk", "/api/v1/orders/*/start-delivery", "/api/v1/orders/*/complete", "/api/v1/orders/*/cancel", "/api/v1/orders/*/change-lines").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_WAITER)
                        .requestMatchers("/api/v1/orders/*/approve/kitchen").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_COOK)
                        // Mark as ready - any authenticated user
                        .requestMatchers("/api/v1/orders/*/ready").authenticated()

                        .requestMatchers("/api/v1/bills/**").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER, ROLE_WAITER)

                        // Menu
                        .requestMatchers(HttpMethod.GET, "/api/v1/menu/**").permitAll()
                        .requestMatchers("/api/v1/menu/**").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)

                        // Users
                        .requestMatchers("/api/v1/users/*/change-password").authenticated()
                        .requestMatchers("/api/v1/users/me").authenticated()
                        .requestMatchers("/api/v1/users/**").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)

                        // fallback - authenticated
                        .anyRequest().authenticated()
                 )
                 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                 .build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder,
            UserCredentialsProvider credentialsProvider
    ) {
        return new UserPrincipalAuthenticationProvider(passwordEncoder, credentialsProvider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    public MessageDigest messageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    @Bean
    public Algorithm jwtAlgorithm(JwtProperties jwtProperties) {
        return Algorithm.HMAC256(jwtProperties.key());
    }

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

}
