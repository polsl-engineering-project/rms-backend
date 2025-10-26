package com.polsl.engineering.project.rms.security.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import com.polsl.engineering.project.rms.security.UserPrincipalAuthenticationToken;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    JwtService jwtService;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Given no Authorization header When doFilterInternal Then does not authenticate and delegates filter chain")
    void GivenNoAuthorizationHeader_WhenDoFilterInternal_ThenDoesNotAuthenticateAndDelegatesFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Given non-Bearer Authorization header When doFilterInternal Then does not authenticate and delegates filter chain")
    void GivenNonBearerAuthorizationHeader_WhenDoFilterInternal_ThenDoesNotAuthenticateAndDelegatesFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic abcdef");

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Given valid Bearer token When doFilterInternal Then sets authentication and delegates filter chain")
    void GivenValidBearerToken_WhenDoFilterInternal_ThenSetsAuthenticationAndDelegatesFilterChain() throws Exception {
        // given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        UUID userId = UUID.randomUUID();
        var roles = List.of(UserPrincipal.Role.ADMIN);
        var userPrincipal = new UserPrincipal(userId, roles);
        when(jwtService.parseJwt(token)).thenReturn(userPrincipal);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication)
                .isInstanceOf(UserPrincipalAuthenticationToken.class);
        assertThat(authentication.getPrincipal())
                .isEqualTo(userPrincipal);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        assertThat(authentication.getName()).isEqualTo(userId.toString());

        verify(jwtService, times(1)).parseJwt(token);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Given invalid Bearer token When doFilterInternal Then clears context and throws JwtVerificationFailedException")
    void GivenInvalidBearerToken_WhenDoFilterInternal_ThenClearsContextAndThrowsJwtVerificationFailedException() throws Exception {
        // given
        String token = "invalid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.parseJwt(token)).thenThrow(new JWTVerificationException("Invalid signature"));

        // when / then
        assertThatThrownBy(() -> jwtFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(JwtVerificationFailedException.class)
                .hasMessageContaining("JWT verification failed")
                .hasCauseInstanceOf(JWTVerificationException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtService, times(1)).parseJwt(token);
    }
}