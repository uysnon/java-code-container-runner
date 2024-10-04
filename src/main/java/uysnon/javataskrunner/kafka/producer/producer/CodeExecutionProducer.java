package uysnon.javataskrunner.kafka.producer.producer;


import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import uysnon.javataskrunner.dto.CodeExecutionResult;

@Component
@RequiredArgsConstructor
public class CodeExecutionProducer {

    private final KafkaTemplate<String, CodeExecutionResult> kafkaTemplate;

    public void sendResult(CodeExecutionResult result) {
        kafkaTemplate.send("results", result.getTaskId(), result);
    }
}