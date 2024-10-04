package uysnon.javataskrunner.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uysnon.javataskrunner.dto.CodeExecutionRequest;
import uysnon.javataskrunner.service.CodeExecutionService;

@Component
@RequiredArgsConstructor
public class CodeExecutionConsumer {

    private final CodeExecutionService codeExecutionService;

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "tasks",
            groupId = "java-code-executor-group",
            concurrency = "1" // чтобы одновременно выполнялась = 1 программа, не было влияния одной программы на другую
    )
    public void consume(String request) throws JsonProcessingException {
        System.out.println("hello! request: " + request);
        CodeExecutionRequest codeExecutionRequest = objectMapper.readValue(request, CodeExecutionRequest.class);
//        codeExecutionService.executeCode(request);
    }
}