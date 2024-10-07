FROM alpine:3.18

RUN apk update && apk add --no-cache \
    bash \
    curl \
    ca-certificates \
    openjdk8 \
    openjdk11 \
    openjdk17 \
    && rm -rf /var/cache/apk/*

WORKDIR /app

COPY build/libs/java-code-container-runner.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]