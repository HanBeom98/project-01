package com.spring_cloud.eureka.client.order.orders;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spring_cloud.eureka.client.order.core.domain.Order;
import com.spring_cloud.eureka.client.order.core.domain.QOrder;
import com.spring_cloud.eureka.client.order.core.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.spring_cloud.eureka.client.order.core.domain.QOrder.*;


@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderResponseDto> searchOrders(OrderSearchDto searchDto, Pageable pageable,String role, String userId) {

        List<OrderSpecifier<?>> orders = getAllOrderSpecifiers(pageable);

        QueryResults<Order> results = queryFactory
                .selectFrom(order)
                .where(
                        statusEq(searchDto.getStatus()),
                        orderItemIdsIn(searchDto.getOrderItemIds()),
                        userCheck(role, userId)
                )
                .orderBy(orders.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<OrderResponseDto> content = results.getResults().stream()
                .map(Order::toResponseDto)
                .collect(Collectors.toList());
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }
    private BooleanExpression userCheck(String role, String userId) {
        return role.equals("MEMBER")? order.createdBy.eq(userId): null;
    }

    private BooleanExpression orderItemIdsIn(List<Long> orderItemIds) {
        return orderItemIds != null && !orderItemIds.isEmpty() ? order.orderItemIds.any().in(orderItemIds) : null;
    }

    private List<OrderSpecifier<?>> getAllOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (pageable.getSort() != null) {
            for (Sort.Order sortOrder : pageable.getSort()) {
                com.querydsl.core.types.Order direction = sortOrder.isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC;
                switch (sortOrder.getProperty()) {
                    case "createdAt":
                        orders.add(new OrderSpecifier<>(direction, QOrder.order.createdAt));
                        break;
                    case "status":
                        orders.add(new OrderSpecifier<>(direction, QOrder.order.status));
                        break;
                    default:
                        break;
                }
            }
        }

        return orders;
    }
}