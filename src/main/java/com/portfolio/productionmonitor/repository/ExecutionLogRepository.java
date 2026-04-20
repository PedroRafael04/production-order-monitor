package com.portfolio.productionmonitor.repository;

import com.portfolio.productionmonitor.model.ExecutionLog;
import com.portfolio.productionmonitor.model.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    List<ExecutionLog> findByOrderIdOrderByLoggedAtDesc(Long orderId);

    Page<ExecutionLog> findByOrderIdAndLevelOrderByLoggedAtDesc(Long orderId, LogLevel level, Pageable pageable);

    // Logs in a specific time range for an order (native SQL)
    @Query(value = """
            SELECT *
            FROM   execution_logs
            WHERE  order_id  = :orderId
              AND  logged_at BETWEEN :start AND :end
            ORDER  BY logged_at DESC
            """, nativeQuery = true)
    List<ExecutionLog> findByOrderIdAndPeriod(
            @Param("orderId") Long orderId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    // Count errors per order (used in health check logic)
    @Query("""
            SELECT COUNT(l) FROM ExecutionLog l
            WHERE l.order.id = :orderId AND l.level = 'ERROR'
            """)
    long countErrorsByOrderId(@Param("orderId") Long orderId);
}
