package com.ThoughtsAndOpinions.IdentityService.repository;


import com.ThoughtsAndOpinions.IdentityService.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);

    // Maps to your "LIKE username%" SearchRequest requirement
    List<UserEntity> findTop5ByUsernameStartingWithIgnoreCase(String prefix);

}
