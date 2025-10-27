package com.polsl.engineering.project.rms.security;

import com.polsl.engineering.project.rms.security.exception.InvalidCredentialsException;
import com.polsl.engineering.project.rms.security.exception.NullCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPrincipalAuthenticationProviderTest {

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserCredentialsProvider credentialsProvider;

    @Test
    @DisplayName("Given valid UsernamePasswordAuthenticationToken When authenticate Then returns UserPrincipalAuthenticationToken with principal and authorities")
    void GivenValidUsernamePasswordToken_WhenAuthenticate_ThenReturnUserPrincipalAuthenticationToken() {
        // Given
        var provider = new UserPrincipalAuthenticationProvider(passwordEncoder, credentialsProvider);
        var username = "john.doe";
        var rawPassword = "s3cr3t";
        var encodedPassword = "{bcrypt}hash";
        var roles = List.of(UserPrincipal.Role.ADMIN, UserPrincipal.Role.MANAGER);
        var userCredentials = new UserCredentials(UUID.randomUUID(), encodedPassword, roles);
        var expectedPrincipal = new UserPrincipal(userCredentials.id(), userCredentials.roles());

        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(credentialsProvider.getUserCredentials(any()))
                .thenReturn(Optional.of(userCredentials));

        var input = new UsernamePasswordAuthenticationToken(username, rawPassword);

        // When
        var result = provider.authenticate(input);

        // Then
        assertThat(result)
                .isInstanceOf(UserPrincipalAuthenticationToken.class)
                .matches(Authentication::isAuthenticated);
        assertThat(result.getPrincipal()).isEqualTo(expectedPrincipal);
        assertThat(result.getName()).isEqualTo(expectedPrincipal.id().toString());
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrderElementsOf(
                        roles.stream().map(GrantedAuthority::getAuthority).toList()
                );
        assertThat(userCredentials.toUserPrincipal()).isEqualTo(expectedPrincipal);

        // And verify interactions and passed credentials
        var captor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(credentialsProvider).getUserCredentials(captor.capture());
        verifyNoMoreInteractions(passwordEncoder, credentialsProvider);

        assertThat(captor.getValue()).isEqualTo(username);
    }

    @Test
    @DisplayName("Given non UsernamePasswordAuthentication Authentication When authenticate Then throws IllegalArgumentException with message")
    void GivenNonUsernamePasswordAuthentication_WhenAuthenticate_ThenThrowIllegalArgumentException() {
        // Given
        var provider = new UserPrincipalAuthenticationProvider(passwordEncoder, credentialsProvider);
        var otherAuth = mock(Authentication.class);

        // When
        var call = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> provider.authenticate(otherAuth);

        // Then
        assertThatThrownBy(call)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported authentication type");

        verifyNoInteractions(passwordEncoder, credentialsProvider);
    }

    @Test
    @DisplayName("Given UsernamePasswordAuthenticationToken with null credentials When authenticate Then throws NullCredentialsException")
    void GivenNullCredentials_WhenAuthenticate_ThenThrowNullCredentialsException() {
        // Given
        var provider = new UserPrincipalAuthenticationProvider(passwordEncoder, credentialsProvider);
        var input = new UsernamePasswordAuthenticationToken("john.doe", null);

        // When
        var call = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> provider.authenticate(input);

        // Then
        assertThatThrownBy(call)
                .isInstanceOf(NullCredentialsException.class);

        verifyNoInteractions(passwordEncoder, credentialsProvider);
    }

    @Test
    void GivenInvalidCredentials_WhenAuthenticate_ThenThrowInvalidCredentialsException() {
        // Given
        var provider = new UserPrincipalAuthenticationProvider(passwordEncoder, credentialsProvider);
        var username = "john.doe";
        var rawPassword = "bad-pass";
        var encodedPassword = "encoded-bad";
        when(credentialsProvider.getUserCredentials(username))
                .thenReturn(Optional.empty());

        var input = new UsernamePasswordAuthenticationToken(username, rawPassword);

        // When
        var call = (org.assertj.core.api.ThrowableAssert.ThrowingCallable) () -> provider.authenticate(input);

        // Then
        assertThatThrownBy(call)
                .isInstanceOf(InvalidCredentialsException.class);

        verify(credentialsProvider).getUserCredentials(username);
        verifyNoMoreInteractions(passwordEncoder, credentialsProvider);
    }

    @Test
    @DisplayName("GIVEN authentication class WHEN supports THEN true for UsernamePasswordAuthenticationToken and false otherwise")
    void GivenAuthenticationClass_WhenSupports_ThenCorrectBooleanReturned() {
        // Given
        var provider = new UserPrincipalAuthenticationProvider(passwordEncoder, credentialsProvider);

        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(Authentication.class)).isFalse();
    }
}