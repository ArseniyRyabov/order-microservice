package com.github.arseniyryabov.order.integrations.client;

import com.github.arseniyryabov.order.integrations.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient userServiceRestClient;
    private static final String GET_USER_BY_ID_URL = "/users/{id}";

    public UserResponse getUserById(Long userId) {
        try {
            return userServiceRestClient.get()
                    .uri(GET_USER_BY_ID_URL, userId)
                    .retrieve()
                    .body(UserResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении пользователя: " + e.getMessage(), e);
        }
    }

    public boolean validateUserExists(Long userId) {
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
