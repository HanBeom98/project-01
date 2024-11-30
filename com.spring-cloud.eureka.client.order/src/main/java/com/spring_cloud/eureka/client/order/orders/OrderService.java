package com.spring_cloud.eureka.client.order.orders;

import com.spring_cloud.eureka.client.order.core.client.ProductClient;
import com.spring_cloud.eureka.client.order.core.client.ProductResponseDto;
import com.spring_cloud.eureka.client.order.core.domain.Order;
import com.spring_cloud.eureka.client.order.core.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

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

    public Page<OrderResponseDto> getOrders(OrderSearchDto searchDto, Pageable pageable,String role, String userId) {
        return orderRepository.searchOrders(searchDto, pageable,role, userId);
    }
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));
        return toResponseDto(order);
    }

    @Transactional
    public OrderResponseDto updateOrder(Long orderId, OrderRequestDto requestDto,String userId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));

        order.updateOrder(requestDto.getOrderItemIds(), userId, OrderStatus.valueOf(requestDto.getStatus()));
        Order updatedOrder = orderRepository.save(order);

        return toResponseDto(updatedOrder);
    }

    @Transactional
    public void deleteOrder(Long orderId, String deletedBy) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));
        order.deleteOrder(deletedBy);
        orderRepository.save(order);
    }

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