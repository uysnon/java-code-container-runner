package uysnon.javataskrunner.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uysnon.javataskrunner.dto.CodeExecutionRequest;
import uysnon.javataskrunner.kafka.config.JavaVersionsConfigurationProperties;
import uysnon.javataskrunner.service.CodeExecutionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeExecutionConsumer {

    private final CodeExecutionService codeExecutionService;
    private final ObjectMapper objectMapper;
    private final JavaVersionsConfigurationProperties javaVersions;


    @KafkaListener(
            topics = "tasks",
            groupId = "java-code-executor-group",
            concurrency = "1" // чтобы одновременно выполнялась = 1 программа, не было влияния одной программы на другую
    )
    public void consume(String request) throws JsonProcessingException {
        log.info("read request: {}");
        CodeExecutionRequest codeExecutionRequest = objectMapper.readValue(request, CodeExecutionRequest.class);
        String requestJavaVersion = codeExecutionRequest.getJavaVersion();
        if (!Strings.isEmpty(requestJavaVersion) && !Strings.isEmpty(javaVersions.getVersions().get(requestJavaVersion))) {
            log.info("try to execute code");
            codeExecutionService.executeCode(javaVersions.getVersions().get(requestJavaVersion), codeExecutionRequest);
            log.info("code execution completed");
        }
    }
}