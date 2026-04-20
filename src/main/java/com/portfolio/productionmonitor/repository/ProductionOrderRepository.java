package com.portfolio.productionmonitor.repository;

import com.portfolio.productionmonitor.model.OrderStatus;
import com.portfolio.productionmonitor.model.ProductionOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

        // ---------------------------------------------------------------
        // Basic finders
        // ---------------------------------------------------------------

        Optional<ProductionOrder> findByOrderCode(String orderCode);

        boolean existsByOrderCode(String orderCode);

        // ---------------------------------------------------------------
        // Filter by status
        // ---------------------------------------------------------------

        Page<ProductionOrder> findByStatusOrderByScheduledAtDesc(OrderStatus status, Pageable pageable);

        // ---------------------------------------------------------------
        // Filter by period (JPQL)
        // ---------------------------------------------------------------

        @Query("""
                        SELECT o FROM ProductionOrder o
                        WHERE o.scheduledAt BETWEEN :start AND :end
                        ORDER BY o.scheduledAt DESC
                        """)
        Page<ProductionOrder> findByPeriod(
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        Pageable pageable);

        // ---------------------------------------------------------------
        // Filter by status AND period (JPQL)
        // ---------------------------------------------------------------

        @Query("""
                        SELECT o FROM ProductionOrder o
                        WHERE o.status = coalesce(:status, o.status)
                          AND o.scheduledAt >= coalesce(:start, o.scheduledAt)
                          AND o.scheduledAt <= coalesce(:end, o.scheduledAt)
                        """)
        Page<ProductionOrder> findByFilters(
                        @Param("status") OrderStatus status,
                        @Param("start") OffsetDateTime start,
                        @Param("end") OffsetDateTime end,
                        Pageable pageable);

        // ---------------------------------------------------------------
        // Status update (native SQL — explicit for portfolio showcase)
        // ---------------------------------------------------------------

        @Modifying
        @Query(value = """
                        UPDATE production_orders
                        SET    status      = CAST(:status AS order_status),
                               started_at  = CASE WHEN CAST(:status AS order_status) = 'RUNNING'  THEN NOW() ELSE started_at  END,
                               finished_at = CASE WHEN CAST(:status AS order_status) IN ('FINISHED','CANCELLED') THEN NOW() ELSE finished_at END,
                               updated_at  = NOW()
                        WHERE  id = :id
                        """, nativeQuery = true)
        int updateStatus(@Param("id") Long id, @Param("status") String status);

        // ---------------------------------------------------------------
        // Dashboard summary (native SQL — aggregate query showcase)
        // ---------------------------------------------------------------

        @Query(value = """
                        SELECT
                            COUNT(*)                                         AS total,
                            COUNT(*) FILTER (WHERE status = 'PENDING')      AS pending,
                            COUNT(*) FILTER (WHERE status = 'RUNNING')      AS running,
                            COUNT(*) FILTER (WHERE status = 'FINISHED')     AS finished,
                            COUNT(*) FILTER (WHERE status = 'CANCELLED')    AS cancelled,
                            AVG(EXTRACT(EPOCH FROM (finished_at - started_at)) / 3600.0)
                                FILTER (WHERE status = 'FINISHED')           AS avg_duration_hours
                        FROM production_orders
                        WHERE scheduled_at >= coalesce(CAST(:start AS TIMESTAMPTZ), scheduled_at)
                          AND scheduled_at <= coalesce(CAST(:end   AS TIMESTAMPTZ), scheduled_at)
                        """, nativeQuery = true)
        List<Object[]> getDashboardStats(
                        @Param("start") String start,
                        @Param("end") String end);

        // ---------------------------------------------------------------
        // Machine workload report (native SQL)
        // ---------------------------------------------------------------

        @Query(value = """
                        SELECT
                            machine_id,
                            COUNT(*)                                        AS total_orders,
                            COUNT(*) FILTER (WHERE status = 'FINISHED')    AS completed,
                            COUNT(*) FILTER (WHERE status = 'RUNNING')     AS in_progress,
                            AVG(quantity)                                   AS avg_quantity
                        FROM production_orders
                        WHERE machine_id IS NOT NULL
                        GROUP BY machine_id
                        ORDER BY total_orders DESC
                        """, nativeQuery = true)
        List<Object[]> getMachineWorkloadReport();
}
