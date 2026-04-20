package com.portfolio.productionmonitor.service;

import com.portfolio.productionmonitor.dto.OrderDTOs.*;
import com.portfolio.productionmonitor.exception.InvalidStatusTransitionException;
import com.portfolio.productionmonitor.exception.ResourceNotFoundException;
import com.portfolio.productionmonitor.model.ExecutionLog;
import com.portfolio.productionmonitor.model.OrderStatus;
import com.portfolio.productionmonitor.model.ProductionOrder;
import com.portfolio.productionmonitor.repository.ExecutionLogRepository;
import com.portfolio.productionmonitor.repository.ProductionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionOrderService {

    private final ProductionOrderRepository orderRepository;
    private final ExecutionLogRepository logRepository;

    // Valid status transitions
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,   Set.of(OrderStatus.RUNNING, OrderStatus.CANCELLED),
            OrderStatus.RUNNING,   Set.of(OrderStatus.FINISHED, OrderStatus.CANCELLED),
            OrderStatus.FINISHED,  Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Transactional
    public OrderSummaryResponse createOrder(CreateOrderRequest request) {
        if (orderRepository.existsByOrderCode(request.getOrderCode())) {
            throw new IllegalArgumentException("Order code already exists: " + request.getOrderCode());
        }

        ProductionOrder order = ProductionOrder.builder()
                .orderCode(request.getOrderCode())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .priority(request.getPriority())
                .machineId(request.getMachineId())
                .operatorName(request.getOperatorName())
                .notes(request.getNotes())
                .scheduledAt(request.getScheduledAt())
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);

        appendLog(order, com.portfolio.productionmonitor.model.LogLevel.INFO,
                "Order created and queued for processing.", null);

        log.info("Created production order [{}]", order.getOrderCode());
        return toSummaryResponse(order);
    }

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Long id) {
        ProductionOrder order = findOrderOrThrow(id);
        List<ExecutionLog> logs = logRepository.findByOrderIdOrderByLoggedAtDesc(id);
        return toDetailResponse(order, logs);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderByCode(String code) {
        ProductionOrder order = orderRepository.findByOrderCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + code));
        List<ExecutionLog> logs = logRepository.findByOrderIdOrderByLoggedAtDesc(order.getId());
        return toDetailResponse(order, logs);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listOrders(
            OrderStatus status,
            OffsetDateTime start,
            OffsetDateTime end,
            Pageable pageable) {

        return orderRepository.findByFilters(status, start, end, pageable)
                .map(this::toSummaryResponse);
    }

    // ---------------------------------------------------------------
    // UPDATE STATUS
    // ---------------------------------------------------------------

    @Transactional
    public OrderSummaryResponse updateStatus(Long id, UpdateStatusRequest request) {
        try {
            log.debug("Starting updateStatus for orderId={}, targetStatus={}", id, request.getStatus());
            
            ProductionOrder order = findOrderOrThrow(id);
            OrderStatus current = order.getStatus();
            OrderStatus target  = request.getStatus();

            log.debug("Current status: {}, Target status: {}", current, target);

            if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
                throw new InvalidStatusTransitionException(
                        String.format("Cannot transition from %s to %s", current, target));
            }

            if (request.getMachineId() != null)    order.setMachineId(request.getMachineId());
            if (request.getOperatorName() != null) order.setOperatorName(request.getOperatorName());

            int updated = orderRepository.updateStatus(id, target.name());
            if (updated == 0) throw new ResourceNotFoundException("Order not found: " + id);

            // Re-fetch to get DB-updated timestamps
            order = findOrderOrThrow(id);

            appendLog(order, com.portfolio.productionmonitor.model.LogLevel.INFO,
                    "Status changed from " + current + " to " + target + ".", null);

            log.info("Order [{}] status: {} → {}", order.getOrderCode(), current, target);
            return toSummaryResponse(order);
        } catch (Exception e) {
            log.error("Error updating status for orderId={}", id, e);
            throw e;
        }
    }

    // ---------------------------------------------------------------
    // LOGS
    // ---------------------------------------------------------------

    @Transactional
    public LogResponse addLog(Long orderId, AddLogRequest request) {
        ProductionOrder order = findOrderOrThrow(orderId);

        ExecutionLog executionLog = ExecutionLog.builder()
                .order(order)
                .level(request.getLevel())
                .message(request.getMessage())
                .metadata(request.getMetadata())
                .build();

        executionLog = logRepository.save(executionLog);
        return toLogResponse(executionLog);
    }

    @Transactional(readOnly = true)
    public List<LogResponse> getLogs(Long orderId) {
        findOrderOrThrow(orderId);
        return logRepository.findByOrderIdOrderByLoggedAtDesc(orderId)
                .stream().map(this::toLogResponse).toList();
    }

    // ---------------------------------------------------------------
    // DASHBOARD
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(OffsetDateTime start, OffsetDateTime end) {
        Object[] row = (Object[]) orderRepository.getDashboardStats(
                start != null ? start.toString() : null,
                end   != null ? end.toString()   : null
        );

        return Map.of(
                "total",             row[0],
                "pending",           row[1],
                "running",           row[2],
                "finished",          row[3],
                "cancelled",         row[4],
                "avgDurationHours",  row[5] != null ? row[5] : BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMachineWorkload() {
        return orderRepository.getMachineWorkloadReport().stream()
                .map(row -> Map.<String, Object>of(
                        "machineId",    row[0],
                        "totalOrders",  row[1],
                        "completed",    row[2],
                        "inProgress",   row[3],
                        "avgQuantity",  row[4]
                ))
                .toList();
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private ProductionOrder findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private void appendLog(ProductionOrder order, com.portfolio.productionmonitor.model.LogLevel level,
                           String message, String metadata) {
        ExecutionLog entry = ExecutionLog.builder()
                .order(order)
                .level(level)
                .message(message)
                .metadata(metadata)
                .build();
        logRepository.save(entry);
    }

    // ---------------------------------------------------------------
    // MAPPERS
    // ---------------------------------------------------------------

    private OrderSummaryResponse toSummaryResponse(ProductionOrder o) {
        return OrderSummaryResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .productName(o.getProductName())
                .quantity(o.getQuantity())
                .status(o.getStatus())
                .priority(o.getPriority())
                .machineId(o.getMachineId())
                .operatorName(o.getOperatorName())
                .scheduledAt(o.getScheduledAt())
                .startedAt(o.getStartedAt())
                .finishedAt(o.getFinishedAt())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private OrderDetailResponse toDetailResponse(ProductionOrder o, List<ExecutionLog> logs) {
        return OrderDetailResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .productName(o.getProductName())
                .quantity(o.getQuantity())
                .status(o.getStatus())
                .priority(o.getPriority())
                .machineId(o.getMachineId())
                .operatorName(o.getOperatorName())
                .notes(o.getNotes())
                .scheduledAt(o.getScheduledAt())
                .startedAt(o.getStartedAt())
                .finishedAt(o.getFinishedAt())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .logs(logs.stream().map(this::toLogResponse).toList())
                .build();
    }

    private LogResponse toLogResponse(ExecutionLog l) {
        return LogResponse.builder()
                .id(l.getId())
                .level(l.getLevel())
                .message(l.getMessage())
                .metadata(l.getMetadata())
                .loggedAt(l.getLoggedAt())
                .build();
    }
}
