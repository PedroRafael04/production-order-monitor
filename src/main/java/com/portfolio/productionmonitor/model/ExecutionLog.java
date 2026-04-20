package com.portfolio.productionmonitor.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "execution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogLevel level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Optional JSONB metadata (e.g. machine readings, batch info).
     * Stored as String; PostgreSQL handles the JSONB column type.
     */
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private OffsetDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        this.loggedAt = OffsetDateTime.now();
    }
}
