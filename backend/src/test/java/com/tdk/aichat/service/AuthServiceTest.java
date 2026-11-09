package com.tdk.aichat.service;

import com.tdk.aichat.dto.request.LoginRequest;
import com.tdk.aichat.dto.request.SignupRequest;
import com.tdk.aichat.dto.response.AuthResponse;
import com.tdk.aichat.entity.User;
import com.tdk.aichat.exception.DuplicateEmailException;
import com.tdk.aichat.repository.UserRepository;
import com.tdk.aichat.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John")
                .email("john@example.com")
                .password("hashed")
                .build();
    }

    @Test
    void signup_shouldCreateUserAndReturnToken() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtService.generateToken("john@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.signup(
                new SignupRequest("John", "john@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signup_shouldNormalizeEmailToLowercase() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtService.generateToken(anyString())).thenReturn("token");

        authService.signup(new SignupRequest("John", "JOHN@EXAMPLE.COM", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void signup_shouldThrowDuplicateEmailException_whenEmailExists() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(
                new SignupRequest("John", "john@example.com", "password123")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_shouldHashPassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtService.generateToken(anyString())).thenReturn("token");

        authService.signup(new SignupRequest("John", "john@example.com", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("bcrypt-hash");
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken("john@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(
                new LoginRequest("john@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_shouldThrowForInvalidCredentials() {
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("john@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
