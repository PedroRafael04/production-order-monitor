package com.portfolio.productionmonitor.controller;

import com.portfolio.productionmonitor.dto.OrderDTOs.*;
import com.portfolio.productionmonitor.model.OrderStatus;
import com.portfolio.productionmonitor.service.ProductionOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final ProductionOrderService service;

    // ---------------------------------------------------------------
    // POST /api/v1/orders
    // ---------------------------------------------------------------
    @PostMapping
    public ResponseEntity<OrderSummaryResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(request));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/orders/code/{code}
    // ---------------------------------------------------------------
    @GetMapping("/code/{code}")
    public ResponseEntity<OrderDetailResponse> getOrderByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getOrderByCode(code));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/orders/id/{id}
    //----------------------------------------------------------------
    @GetMapping("/id/{id}")
    public ResponseEntity<OrderDetailResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOrderById(id));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/orders?status=PENDING&start=...&end=...&page=0&size=20
    // ---------------------------------------------------------------
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(service.listOrders(status, start, end, pageable));
    }

    // ---------------------------------------------------------------
    // PATCH /api/v1/orders/{id}/status
    // ---------------------------------------------------------------
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderSummaryResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/orders/{id}/logs
    // ---------------------------------------------------------------
    @PostMapping("/{id}/logs")
    public ResponseEntity<LogResponse> addLog(
            @PathVariable Long id,
            @Valid @RequestBody AddLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addLog(id, request));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/orders/{id}/logs
    // ---------------------------------------------------------------
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<LogResponse>> getLogs(@PathVariable Long id) {
        return ResponseEntity.ok(service.getLogs(id));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/orders/dashboard/stats
    // ---------------------------------------------------------------
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        return ResponseEntity.ok(service.getDashboardStats(start, end));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/orders/dashboard/machines
    // ---------------------------------------------------------------
    @GetMapping("/dashboard/machines")
    public ResponseEntity<List<Map<String, Object>>> getMachineWorkload() {
        return ResponseEntity.ok(service.getMachineWorkload());
    }
}
