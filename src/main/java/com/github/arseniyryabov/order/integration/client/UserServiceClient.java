package com.github.arseniyryabov.order.integration.client;

import com.github.arseniyryabov.order.exception.UserNotFoundException;
import com.github.arseniyryabov.order.integration.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;

    public void getUserById(Long userId) {
        try {
            userServiceRestClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(UserResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException(userId);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении пользователя: " + e.getMessage(), e);
        }
    }

    // Проверка пользователя
    public boolean isUserExists(Long userId) {
        try {
            userServiceRestClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при проверке пользователя: " + e.getMessage(), e);
        }
    }
}
