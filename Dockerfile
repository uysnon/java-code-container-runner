# Используем официальный образ с Java
FROM openjdk:17-jdk-alpine

# Указываем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем Gradle сборку в контейнер
COPY ../../build/libs/java-code-container-runner.jar /app/app.jar

# Открываем порт, который будет слушать наше приложение
EXPOSE 8080

# Указываем команду для запуска Spring Boot приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]