package kz.samat.smarthealth.userservice.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import kz.samat.smarthealth.userservice.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    /** Locks the row, so two concurrent refreshes cannot both use the same refresh token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Session> findLockedById(UUID id);

    @Modifying
    @Query("delete from Session s where s.user.id = :userId and s.expiresAt < :now")
    void deleteExpiredByUserId(UUID userId, Instant now);
}
