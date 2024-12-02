package com.spring_cloud.eureka.client.product;

import com.spring_cloud.eureka.client.product.service.RedisService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class RedisTestRunner implements CommandLineRunner {

    private final RedisService redisService;

    public RedisTestRunner(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("CommandLineRunner 실행 중...");

        // Redis에 데이터 저장
        redisService.save("testKey", "Hello, Redis!");

        // Redis에서 데이터 조회
        String value = redisService.find("testKey");
        System.out.println("Redis에서 조회된 값: " + value);
    }

}
