package com.portfolio.productionmonitor.model;

/**
 * Represents the full lifecycle of a production order.
 * Transitions: PENDING → RUNNING → FINISHED
 *              PENDING → CANCELLED
 *              RUNNING → CANCELLED
 */
public enum OrderStatus {
    PENDING,
    RUNNING,
    FINISHED,
    CANCELLED
}
