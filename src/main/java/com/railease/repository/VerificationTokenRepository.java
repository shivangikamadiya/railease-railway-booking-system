package com.railease.repository;

import com.railease.entity.User;
import com.railease.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.token = :token")
    int markTokenAsUsed(@Param("token") String token);
}