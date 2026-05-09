package org.example.userservice.service;

import org.example.userservice.dto.AuthResponse;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.model.Role;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    private User sampleUser() {
        User u = new User("Alice", "alice@example.com", "hashed", Role.USER);
        u.setId(1L);
        return u;
    }

    @Test
    void register_savesUserAndReturnsToken() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser());
        when(jwtService.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(
                new RegisterRequest("Alice", "alice@example.com", "password123", null));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_passwordIsHashed_notStoredPlaintext() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedvalue");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(), any())).thenReturn("token");

        authService.register(new RegisterRequest("Alice", "alice@example.com", "password123", null));

        verify(passwordEncoder).encode("password123");
        // verify save is called with encoded password, not plaintext
        verify(userRepository).save(argThat(u -> u.getPassword().equals("$2a$10$hashedvalue")));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Alice", "alice@example.com", "password123", null)))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("Alice");
    }

    @Test
    void login_throwsBadCredentials_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentials_whenPasswordWrong() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_sameErrorForWrongUserAndWrongPassword() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        // both scenarios throw same exception type — attacker can't distinguish
        assertThatThrownBy(() -> authService.login(new LoginRequest("nonexistent@example.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrongpassword")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void me_returnsUserResponse_whenUserExists() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));

        var response = authService.me("alice@example.com");

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.username()).isEqualTo("Alice");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void loadUserByUsername_returnsUserDetails() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));

        var userDetails = authService.loadUserByUsername("alice@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("alice@example.com");
    }
}
