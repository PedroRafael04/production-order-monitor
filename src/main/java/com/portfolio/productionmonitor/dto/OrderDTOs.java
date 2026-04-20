package com.portfolio.productionmonitor.dto;

import com.portfolio.productionmonitor.model.LogLevel;
import com.portfolio.productionmonitor.model.OrderStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

// ============================================================
// REQUEST DTOs
// ============================================================

public class OrderDTOs {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {

        @NotBlank(message = "Order code is required")
        @Size(max = 20, message = "Order code must not exceed 20 characters")
        @Pattern(regexp = "^OP-\\d{4}-\\d{4}$", message = "Order code must follow pattern OP-YYYY-NNNN")
        private String orderCode;

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must not exceed 150 characters")
        private String productName;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Priority is required")
        @Min(value = 1, message = "Priority must be between 1 and 5")
        @Max(value = 5, message = "Priority must be between 1 and 5")
        private Short priority;

        @Size(max = 50)
        private String machineId;

        @Size(max = 100)
        private String operatorName;

        private String notes;

        @NotNull(message = "Scheduled date is required")
        @Future(message = "Scheduled date must be in the future")
        private OffsetDateTime scheduledAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {

        @NotNull(message = "Status is required")
        private OrderStatus status;

        private String machineId;
        private String operatorName;
    }

    // ============================================================
    // RESPONSE DTOs
    // ============================================================

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummaryResponse {
        private Long id;
        private String orderCode;
        private String productName;
        private Integer quantity;
        private OrderStatus status;
        private Short priority;
        private String machineId;
        private String operatorName;
        private OffsetDateTime scheduledAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime finishedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailResponse {
        private Long id;
        private String orderCode;
        private String productName;
        private Integer quantity;
        private OrderStatus status;
        private Short priority;
        private String machineId;
        private String operatorName;
        private String notes;
        private OffsetDateTime scheduledAt;
        private OffsetDateTime startedAt;
        private OffsetDateTime finishedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private List<LogResponse> logs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogResponse {
        private Long id;
        private LogLevel level;
        private String message;
        private String metadata;
        private OffsetDateTime loggedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddLogRequest {

        @NotNull(message = "Log level is required")
        private LogLevel level;

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        private String message;

        private String metadata;
    }
}
