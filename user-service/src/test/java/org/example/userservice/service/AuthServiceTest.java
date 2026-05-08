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

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser() {
        User u = new User("Alice", "alice@example.com", "hashed", Role.USER);
        u.setId(1L);
        return u;
    }

    @Test
    void register_savesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser());
        when(jwtService.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("Alice");
    }

    @Test
    void login_throwsBadCredentials_whenUserNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentials_whenPasswordWrong() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrongpassword");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser()));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
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
