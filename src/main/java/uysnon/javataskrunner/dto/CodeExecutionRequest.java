package uysnon.javataskrunner.dto;

import lombok.Data;

import java.util.List;

@Data
public class CodeExecutionRequest {
    private String taskId;
    private String javaVersion;
    private String mainClass;
    private List<FileData> files;
    private List<String> arguments;
    private String stdin;
    private Integer timeout;
}