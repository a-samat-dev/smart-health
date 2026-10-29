package kz.samat.smarthealth.userservice.repository;

import java.util.Optional;
import java.util.UUID;

import kz.samat.smarthealth.userservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
