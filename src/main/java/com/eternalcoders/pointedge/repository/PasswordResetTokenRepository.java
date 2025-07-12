package com.eternalcoders.pointedge.repository;

import com.eternalcoders.pointedge.entity.PasswordResetToken;
import com.eternalcoders.pointedge.entity.Employee;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    // Add this method exactly
    @Transactional
    void deleteByUser(Employee user);

    // Also add this to support Option 1 (if needed)
    Optional<PasswordResetToken> findByUser(Employee user);
}