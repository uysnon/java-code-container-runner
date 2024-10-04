package uysnon.javataskrunner.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    // Создание топика "tasks"
    @Bean
    public NewTopic tasksTopic() {
        return new NewTopic("tasks", 1, (short) 2);
    }

    // Создание топика "results"
    @Bean
    public NewTopic resultsTopic() {
        return new NewTopic("results", 1, (short) 2);
    }
}