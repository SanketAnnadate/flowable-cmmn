package com.br.workflow_cmmn.service;

import com.br.workflow_cmmn.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserService {
    private final WebClient webClient;
    
    public UserService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();
    }
    
    public Flux<User> getAllUsers() {
        return webClient.get()
            .uri("/users")
            .retrieve()
            .bodyToFlux(User.class)
            .map(this::mapToUserWithRole);
    }
    
    public Mono<User> getUserById(String id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(User.class)
            .map(this::mapToUserWithRole);
    }
    
    private User mapToUserWithRole(User user) {
        // Assign roles based on user ID for demo
        if ("1".equals(user.getId())) {
            user.setRole("ADMIN");
        } else {
            int id = Integer.parseInt(user.getId());
            switch (id % 4) {
                case 0 -> user.setRole("REVIEWER");
                case 1 -> user.setRole("ADMIN");
                case 2 -> user.setRole("UPLOADER");
                case 3 -> user.setRole("PREPARATOR");
            }
        }
        return user;
    }
}