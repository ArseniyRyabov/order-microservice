# Этап 1: Сборка приложения
FROM gradle:9.2.1-jdk25-alpine AS builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем исходный код
COPY . .

# Даем права на выполнение скриптов
RUN chmod +x ./gradlew

# Собираем приложение
RUN ./gradlew clean bootJar --no-daemon || gradle clean bootJar --no-daemon

# Этап 2: Запуск приложения
FROM eclipse-temurin:25-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR из этапа builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Создаем пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "/app/app.jar"]