package com.github.arseniyryabov.order.integrations.client;

import com.github.arseniyryabov.order.integrations.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setUp() {
        productServiceClient = new ProductServiceClient(restClient);
    }

    @Test
    void getProductById_Success() {
        UUID productId = UUID.randomUUID();
        ProductResponse expectedResponse = new ProductResponse(
                productId, "Test Product",
                new BigDecimal("99.99"), "Test Description",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/products/{productId}", productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductResponse.class)).thenReturn(expectedResponse);

        ProductResponse actualResponse = productServiceClient.getProductById(productId);

        assertNotNull(actualResponse);
        assertEquals(productId, actualResponse.getProductId());
        assertEquals("Test Product", actualResponse.getName());
        assertEquals(new BigDecimal("99.99"), actualResponse.getPrice());

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/products/{productId}", productId);
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).body(ProductResponse.class);
    }

    @Test
    void getProductById_ProductNotFound() {
        UUID productId = UUID.randomUUID();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/products/{productId}", productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Product not found",
                        null,
                        null,
                        null
                )
        );

        ProductResponse result = productServiceClient.getProductById(productId);

        assertNull(result);

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/products/{productId}", productId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }

    @Test
    void getProductById_ServerError() {
        UUID productId = UUID.randomUUID();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/products/{productId}", productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                new RuntimeException("Connection failed")
        );
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productServiceClient.getProductById(productId));

        assertTrue(exception.getMessage().contains("Ошибка при получении продукта"));

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/products/{productId}", productId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }

    @Test
    void getProductsByIds_Success() {
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        List<UUID> productIds = Arrays.asList(productId1, productId2);

        ProductResponse product1 = new ProductResponse(productId1, "Product 1",
                new BigDecimal("50.00"), "Desc 1", null, null);
        ProductResponse product2 = new ProductResponse(productId2, "Product 2",
                new BigDecimal("75.00"), "Desc 2", null, null);
        List<ProductResponse> expectedResponse = Arrays.asList(product1, product2);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/products/batch")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(productIds)).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(expectedResponse);

        List<ProductResponse> actualResponse = productServiceClient.getProductsByIds(productIds);

        assertNotNull(actualResponse);
        assertEquals(2, actualResponse.size());
        assertEquals(productId1, actualResponse.get(0).getProductId());
        assertEquals(productId2, actualResponse.get(1).getProductId());

        verify(restClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri("/products/batch");
        verify(requestBodySpec, times(1)).body(productIds);
        verify(requestBodySpec, times(1)).retrieve();
        verify(responseSpec, times(1)).body(any(ParameterizedTypeReference.class));
    }

    @Test
    void getProductsByIds_ServerError() {
        List<UUID> productIds = Arrays.asList(UUID.randomUUID());

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/products/batch")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(productIds)).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RuntimeException("Server Error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productServiceClient.getProductsByIds(productIds));

        assertTrue(exception.getMessage().contains("Ошибка при получении продуктов"));

        verify(restClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri("/products/batch");
        verify(requestBodySpec, times(1)).body(productIds);
        verify(requestBodySpec, times(1)).retrieve();
    }

    @Test
    void getProductPrice_Success() {
        UUID productId = UUID.randomUUID();
        BigDecimal expectedPrice = new BigDecimal("149.99");
        ProductResponse productResponse = new ProductResponse(
                productId, "Test Product", expectedPrice,
                "Description", null, null
        );

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/products/{productId}", productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ProductResponse.class)).thenReturn(productResponse);

        BigDecimal actualPrice = productServiceClient.getProductPrice(productId);

        assertEquals(expectedPrice, actualPrice);

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/products/{productId}", productId);
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).body(ProductResponse.class);
    }

    @Test
    void getProductPrice_ProductNotFound() {
        UUID productId = UUID.randomUUID();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/products/{productId}", productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(
                HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Product not found",
                        null,
                        null,
                        null
                )
        );

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productServiceClient.getProductPrice(productId));

        assertTrue(exception.getMessage().contains("Продукт с ID " + productId + " не найден"));

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/products/{productId}", productId);
        verify(requestHeadersSpec, times(1)).retrieve();
    }
}