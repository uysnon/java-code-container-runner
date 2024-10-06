# Базовый образ Alpine для легковесности
FROM alpine:3.18

# Обновляем пакеты и устанавливаем зависимости
RUN apk update && apk add --no-cache \
    bash \
    curl \
    ca-certificates \
    openjdk8 \
    openjdk11 \
    openjdk17 \
    && rm -rf /var/cache/apk/*

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем Gradle сборку в контейнер
COPY build/libs/java-code-container-runner.jar /app/app.jar

# Открываем порт, который будет слушать наше приложение
EXPOSE 8080

# Указываем команду для запуска Spring Boot приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]