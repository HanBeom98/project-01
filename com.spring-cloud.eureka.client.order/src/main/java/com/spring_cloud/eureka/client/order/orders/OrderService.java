package com.spring_cloud.eureka.client.order.orders;

import com.spring_cloud.eureka.client.order.core.client.ProductClient;
import com.spring_cloud.eureka.client.order.core.client.ProductResponseDto;
import com.spring_cloud.eureka.client.order.core.domain.Order;
import com.spring_cloud.eureka.client.order.core.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    /**
     * 주문 생성 로직
     */
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto, String userId) {
        // Check if products exist and if they have enough quantity
        for (Long productId : requestDto.getOrderItemIds()) {
            ProductResponseDto product = productClient.getProduct(productId);
            log.info("############################ Product 수량 확인 : " + product.getQuantity());
            if (product.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product with ID " + productId + " is out of stock.");
            }
        }

        // Reduce the quantity of each product by 1
        for (Long productId : requestDto.getOrderItemIds()) {
            productClient.reduceProductQuantity(productId, 1);
        }

        Order order = Order.createOrder(requestDto.getOrderItemIds(), userId);
        Order savedOrder = orderRepository.save(order);
        return toResponseDto(savedOrder);
    }

    /**
     * 주문 조회 API (캐싱 적용)
     */
    @Cacheable(value = "orders", key = "#orderId", unless = "#result == null")
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));
        return toResponseDto(order);
    }

    /**
     * 주문 목록 조회
     */
    public Page<OrderResponseDto> getOrders(OrderSearchDto searchDto, Pageable pageable, String role, String userId) {
        return orderRepository.searchOrders(searchDto, pageable, role, userId);
    }

    /**
     * 주문 업데이트 API (캐시 무효화)
     */
    @CacheEvict(value = "orders", allEntries = true)
    @Transactional
    public OrderResponseDto updateOrder(Long orderId, OrderRequestDto requestDto, String userId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));

        order.updateOrder(requestDto.getOrderItemIds(), userId, OrderStatus.valueOf(requestDto.getStatus()));
        Order updatedOrder = orderRepository.save(order);

        return toResponseDto(updatedOrder);
    }

    /**
     * 주문 삭제 API (캐시 무효화)
     */
    @CacheEvict(value = "orders", allEntries = true)
    @Transactional
    public void deleteOrder(Long orderId, String deletedBy) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));
        order.deleteOrder(deletedBy);
        orderRepository.save(order);
    }

    /**
     * 주문 Entity -> Response DTO 변환
     */
    private OrderResponseDto toResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getCreatedBy(),
                order.getUpdatedAt(),
                order.getUpdatedBy(),
                order.getOrderItemIds()
        );
    }
}
