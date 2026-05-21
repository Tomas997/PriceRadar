package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.AuthResponse;
import org.example.userservice.dto.LoginRequest;
import org.example.userservice.dto.RegisterRequest;
import org.example.userservice.dto.UpdateProfileRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.model.RefreshToken;
import org.example.userservice.model.Role;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }
        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );
        if (request.telegramChatId() != null && !request.telegramChatId().isBlank()) {
            user.setTelegramChatId(request.telegramChatId());
        }
        userRepository.save(user);
        log.info("Registered new user email={}", user.getEmail());
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        loginRateLimiter.checkNotBlocked(request.email());
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginRateLimiter.recordFailure(request.email());
            throw new BadCredentialsException("Invalid credentials");
        }
        log.info("User logged in email={}", user.getEmail());
        return buildResponse(user);
    }

    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken newRefresh = refreshTokenService.rotate(refreshTokenStr);
        User user = userRepository.findByEmail(newRefresh.getUserEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("Refreshed tokens for email={}", user.getEmail());
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(accessToken, newRefresh.getToken(),
                user.getDisplayName(), user.getEmail(), user.getRole().name(), user.getTelegramChatId());
    }

    public void logout(String refreshTokenStr) {
        refreshTokenService.revoke(refreshTokenStr);
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return UserResponse.from(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        String tg = request.telegramChatId();
        user.setTelegramChatId(tg != null && !tg.isBlank() ? tg : null);
        userRepository.save(user);
        log.info("Updated profile email={}", email);
        return UserResponse.from(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private AuthResponse buildResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.create(user.getEmail()).getToken();
        return new AuthResponse(accessToken, refreshToken,
                user.getDisplayName(), user.getEmail(), user.getRole().name(), user.getTelegramChatId());
    }
}
