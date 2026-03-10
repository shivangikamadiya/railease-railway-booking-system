package com.railease.repository;

import com.railease.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isEnabled = true WHERE u.email = :email")
    int enableUser(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.password = :password WHERE u.email = :email")
    int updatePassword(@Param("email") String email, @Param("password") String password);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.sessionId = :sessionId, u.lastLogin = CURRENT_TIMESTAMP WHERE u.userId = :userId")
    int updateSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}