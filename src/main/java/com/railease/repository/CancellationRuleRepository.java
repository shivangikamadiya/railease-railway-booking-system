package com.railease.repository;

import com.railease.entity.CancellationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationRuleRepository extends JpaRepository<CancellationRule, Long> {

    @Query("SELECT r FROM CancellationRule r WHERE r.isActive = true " +
            "ORDER BY r.minHoursBeforeDeparture DESC, r.maxHoursBeforeDeparture ASC")
    List<CancellationRule> findActiveRules();

    @Query("SELECT r FROM CancellationRule r ORDER BY r.minHoursBeforeDeparture DESC, r.maxHoursBeforeDeparture ASC")
    List<CancellationRule> findAllOrdered();
}
