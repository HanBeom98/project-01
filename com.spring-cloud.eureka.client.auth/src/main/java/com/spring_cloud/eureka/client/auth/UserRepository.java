package com.spring_cloud.eureka.client.auth;

import com.spring_cloud.eureka.client.auth.core.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
