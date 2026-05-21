package org.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.example.userservice.model.RefreshToken;
import org.example.userservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken create(String userEmail) {
        repo.deleteByUserEmail(userEmail);
        RefreshToken token = new RefreshToken(
                UUID.randomUUID().toString(),
                userEmail,
                LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS)
        );
        return repo.save(token);
    }

    @Transactional
    public RefreshToken rotate(String oldToken) {
        RefreshToken existing = repo.findByToken(oldToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (existing.isExpired()) {
            repo.delete(existing);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        String userEmail = existing.getUserEmail();
        repo.delete(existing);
        return create(userEmail);
    }

    @Transactional
    public void revoke(String token) {
        repo.deleteByToken(token);
    }
}