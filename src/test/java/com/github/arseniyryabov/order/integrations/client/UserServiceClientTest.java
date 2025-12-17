package com.github.arseniyryabov.order.integrations.client;

import static org.junit.jupiter.api.Assertions.*;
import com.github.arseniyryabov.order.integrations.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        userServiceClient = new UserServiceClient(restClient);
    }

    @Test
    void getUserById_Success() {
        // Arrange
        Long userId = 1L;
        UserResponse expectedResponse = new UserResponse(userId, "john.doe", "Doe", "John");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/users/{id}", userId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(UserResponse.class)).thenReturn(expectedResponse);

        // Act
        UserResponse actualResponse = userServiceClient.getUserById(userId);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(userId, actualResponse.getId());
        assertEquals("john.doe", actualResponse.getUserName());
        assertEquals("Doe", actualResponse.getLastName());

        // Verify
        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/users/{id}", userId);
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).body(UserResponse.class);
    }

    @Test
    void getUserById_UserNotFound() {
        Long userId = 999L;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/users/{id}", userId)).thenReturn(requestHeadersSpec);

        HttpClientErrorException notFoundException = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "User not found",
                null,
                null,
                null
        );
        when(requestHeadersSpec.retrieve()).thenThrow(notFoundException);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userServiceClient.getUserById(userId));

        assertTrue(exception.getMessage().contains("Пользователь с ID " + userId + " не найден"));

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/users/{id}", userId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }

    @Test
    void getUserById_ServerError() {
        Long userId = 1L;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/users/{id}", userId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error")
        );

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userServiceClient.getUserById(userId));

        assertTrue(exception.getMessage().contains("Ошибка при получении пользователя"));

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/users/{id}", userId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }

    @Test
    void validateUserExists_Success() {
        Long userId = 1L;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/users/{id}", userId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        boolean result = userServiceClient.validateUserExists(userId);

        assertTrue(result);

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/users/{id}", userId);
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).toBodilessEntity();
    }

    @Test
    void validateUserExists_UserNotFound() {
        Long userId = 999L;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/users/{id}", userId)).thenReturn(requestHeadersSpec);

        HttpClientErrorException notFoundException = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "User not found",
                null,
                null,
                null
        );
        when(requestHeadersSpec.retrieve()).thenThrow(notFoundException);

        boolean result = userServiceClient.validateUserExists(userId);

        assertFalse(result, "Должно вернуться false при 404 ошибке");

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/users/{id}", userId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }

    @Test
    void validateUserExists_ServerError() {
        Long userId = 1L;

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/users/{id}", userId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                new RuntimeException("Connection failed")
        );

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userServiceClient.validateUserExists(userId));

        assertTrue(exception.getMessage().contains("Ошибка при проверке пользователя"));

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/users/{id}", userId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }
}