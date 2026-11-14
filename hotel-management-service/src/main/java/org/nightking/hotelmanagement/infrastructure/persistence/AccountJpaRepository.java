package org.nightking.hotelmanagement.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, String> {
    Optional<AccountJpaEntity> findByUsername(String username);
}
