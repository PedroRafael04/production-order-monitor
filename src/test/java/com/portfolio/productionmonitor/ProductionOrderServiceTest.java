package com.portfolio.productionmonitor;

import com.portfolio.productionmonitor.dto.OrderDTOs.*;
import com.portfolio.productionmonitor.exception.InvalidStatusTransitionException;
import com.portfolio.productionmonitor.exception.ResourceNotFoundException;
import com.portfolio.productionmonitor.model.OrderStatus;
import com.portfolio.productionmonitor.model.ProductionOrder;
import com.portfolio.productionmonitor.repository.ExecutionLogRepository;
import com.portfolio.productionmonitor.repository.ProductionOrderRepository;
import com.portfolio.productionmonitor.service.ProductionOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductionOrderService Unit Tests")
class ProductionOrderServiceTest {

    @Mock private ProductionOrderRepository orderRepository;
    @Mock private ExecutionLogRepository logRepository;

    @InjectMocks
    private ProductionOrderService service;

    private ProductionOrder sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = ProductionOrder.builder()
                .id(1L)
                .orderCode("OP-2024-0001")
                .productName("Steel Shaft 40mm")
                .quantity(500)
                .status(OrderStatus.PENDING)
                .priority((short) 3)
                .scheduledAt(OffsetDateTime.now().plusDays(2))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ---------------------------------------------------------------
    // createOrder
    // ---------------------------------------------------------------

    @Test
    @DisplayName("should create order successfully when order code is unique")
    void createOrder_success() {
        when(orderRepository.existsByOrderCode("OP-2024-0001")).thenReturn(false);
        when(orderRepository.save(any())).thenReturn(sampleOrder);

        CreateOrderRequest request = new CreateOrderRequest(
                "OP-2024-0001", "Steel Shaft 40mm", 500, (short) 3,
                "CNC-01", "John", null, OffsetDateTime.now().plusDays(2)
        );

        OrderSummaryResponse result = service.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getOrderCode()).isEqualTo("OP-2024-0001");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when order code already exists")
    void createOrder_duplicateCode_throws() {
        when(orderRepository.existsByOrderCode("OP-2024-0001")).thenReturn(true);

        CreateOrderRequest request = new CreateOrderRequest(
                "OP-2024-0001", "Product", 10, (short) 1,
                null, null, null, OffsetDateTime.now().plusDays(1)
        );

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OP-2024-0001");
    }

    // ---------------------------------------------------------------
    // getOrderById
    // ---------------------------------------------------------------

    @Test
    @DisplayName("should return order detail when ID exists")
    void getOrderById_found() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(logRepository.findByOrderIdOrderByLoggedAtDesc(1L)).thenReturn(List.of());

        OrderDetailResponse result = service.getOrderById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderCode()).isEqualTo("OP-2024-0001");
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when ID does not exist")
    void getOrderById_notFound_throws() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---------------------------------------------------------------
    // updateStatus
    // ---------------------------------------------------------------

    @Test
    @DisplayName("should transition PENDING → RUNNING successfully")
    void updateStatus_pendingToRunning_success() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(sampleOrder))   // first call (validation)
                .thenReturn(Optional.of(sampleOrder));  // second call (re-fetch)
        when(orderRepository.updateStatus(1L, "RUNNING")).thenReturn(1);

        UpdateStatusRequest request = new UpdateStatusRequest(OrderStatus.RUNNING, "CNC-01", "John");
        OrderSummaryResponse result = service.updateStatus(1L, request);

        assertThat(result).isNotNull();
        verify(orderRepository).updateStatus(1L, "RUNNING");
    }

    @Test
    @DisplayName("should throw InvalidStatusTransitionException for illegal transition")
    void updateStatus_illegalTransition_throws() {
        sampleOrder.setStatus(OrderStatus.FINISHED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        UpdateStatusRequest request = new UpdateStatusRequest(OrderStatus.PENDING, null, null);

        assertThatThrownBy(() -> service.updateStatus(1L, request))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("FINISHED")
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("should throw InvalidStatusTransitionException for CANCELLED → RUNNING")
    void updateStatus_cancelledToRunning_throws() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        UpdateStatusRequest request = new UpdateStatusRequest(OrderStatus.RUNNING, null, null);

        assertThatThrownBy(() -> service.updateStatus(1L, request))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
