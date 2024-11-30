package com.spring_cloud.eureka.client.order.orders;

import com.spring_cloud.eureka.client.order.core.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Setter
public class OrderSearchDto {
    private OrderStatus status;
    private List<Long> orderItemIds;
    private String sortBy;
    private Pageable pageable;
}