package com.spring_cloud.eureka.client.order.core.client;

import org.springframework.stereotype.Component;

@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductResponseDto getProduct(Long id) {
        ProductResponseDto fallbackResponse = new ProductResponseDto();
        fallbackResponse.setId(id);
        fallbackResponse.setName("Unavailable Product");
        fallbackResponse.setDescription("Product service is down. This is fallback response.");
        fallbackResponse.setPrice(0);
        fallbackResponse.setQuantity(0);
        return fallbackResponse;
    }

    @Override
    public void reduceProductQuantity(Long id, int quantity) {
        throw new RuntimeException("Product service is currently unavailable. Cannot reduce quantity.");
    }
}
