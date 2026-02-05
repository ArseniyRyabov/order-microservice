package com.github.arseniyryabov.order.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enum для статусов заказа.
 * Использование enum вместо строковых констант дает:
 * 1. Типобезопасность
 * 2. Автодополнение в IDE
 * 3. Валидация на этапе компиляции
 * 4. Легкость расширения
 */
public enum OrderStatus {

    CREATED("CREATED", "Создан"),
    PROCESSING("PROCESSING", "В обработке"),
    SHIPPED("SHIPPED", "Отправлен"),
    DELIVERED("DELIVERED", "Доставлен"),
    CANCELLED("CANCELLED", "Отменен"),
    PENDING("PENDING", "В ожидании");

    private final String code;
    /**
     * -- GETTER --
     *  Получение описания статуса
     */
    @Getter
    private final String description;

    // Статическая карта для быстрого поиска по коду
    private static final Map<String, OrderStatus> BY_CODE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(OrderStatus::getCode, Function.identity()));

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Получение кода статуса (для JSON сериализации)
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * Десериализация из строки (для JSON десериализации)
     * @JsonCreator позволяет Jackson создавать enum из строки
     */
    @JsonCreator
    public static OrderStatus fromCode(String code) {
        OrderStatus status = BY_CODE.get(code.toUpperCase());
        if (status == null) {
            throw new IllegalArgumentException("Неизвестный статус заказа: " + code);
        }
        return status;
    }

    /**
     * Проверка, является ли статус конечным (завершенным)
     */
    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }

    /**
     * Проверка, может ли заказ быть отменен из текущего статуса
     */
    public boolean canBeCancelled() {
        return !this.isFinal() && this != CANCELLED;
    }

    /**
     * Получение следующего возможного статуса
     */
    public OrderStatus getNextStatus() {
        switch (this) {
            case CREATED:
                return PROCESSING;
            case PROCESSING:
                return SHIPPED;
            case SHIPPED:
                return DELIVERED;
            case PENDING:
                return PROCESSING;
            default:
                return this;
        }
    }

    @Override
    public String toString() {
        return code + " - " + description;
    }
}