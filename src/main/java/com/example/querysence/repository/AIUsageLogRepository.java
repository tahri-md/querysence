package com.example.querysence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.querysence.model.AIUsageLog;
import com.example.querysence.model.User;

@Repository
public interface AIUsageLogRepository extends JpaRepository<AIUsageLog, Long> {

        @Query("SELECT COUNT(al) FROM AIUsageLog al WHERE al.user = :user AND al.createdAt >= :since")
    Long countByUserSince(User user, LocalDateTime since);

       List<AIUsageLog> findByUser(User user);

       List<AIUsageLog> findByUserId(Long userId);

       List<AIUsageLog> findByFeature(String feature);

       @Query("SELECT SUM(a.promptTokens + a.completionTokens) FROM AIUsageLog a " +
                     "WHERE a.user.id = :userId AND a.createdAt >= :startDate")
       Long getTotalTokensByUserSince(@Param("userId") Long userId,
                     @Param("startDate") LocalDateTime startDate);

       @Query("SELECT COUNT(a) FROM AIUsageLog a WHERE a.user.id = :userId " +
                     "AND a.createdAt >= :startDate")
       Long countRequestsByUserSince(@Param("userId") Long userId,
                     @Param("startDate") LocalDateTime startDate);

       @Query("SELECT a.feature, COUNT(a) as count FROM AIUsageLog a " +
                     "WHERE a.user.id = :userId GROUP BY a.feature ORDER BY count DESC")
       List<Object[]> getFeatureUsageStats(@Param("userId") Long userId);
}