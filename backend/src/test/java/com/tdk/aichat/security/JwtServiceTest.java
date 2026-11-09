package com.tdk.aichat.security;

import com.tdk.aichat.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-that-is-at-least-32-characters-long");
        props.setExpirationMs(3600000L);
        jwtService = new JwtService(props);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractSubject_shouldReturnEmail() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken("user@example.com");
        UserDetails userDetails = User.withUsername("user@example.com")
                .password("pass").authorities(Collections.emptyList()).build();
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForWrongUser() {
        String token = jwtService.generateToken("user@example.com");
        UserDetails other = User.withUsername("other@example.com")
                .password("pass").authorities(Collections.emptyList()).build();
        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void isTokenExpired_shouldReturnFalseForFreshToken() {
        String token = jwtService.generateToken("user@example.com");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() throws InterruptedException {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-that-is-at-least-32-characters-long");
        shortProps.setExpirationMs(1L);
        JwtService shortJwt = new JwtService(shortProps);

        String token = shortJwt.generateToken("user@example.com");
        Thread.sleep(10);

        UserDetails userDetails = User.withUsername("user@example.com")
                .password("pass").authorities(Collections.emptyList()).build();
        assertThat(shortJwt.isTokenValid(token, userDetails)).isFalse();
    }
}
