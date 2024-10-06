package uysnon.javataskrunner.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "application.java")
public class JavaVersionsConfigurationProperties {
    private Map<String, String> versions;
}
