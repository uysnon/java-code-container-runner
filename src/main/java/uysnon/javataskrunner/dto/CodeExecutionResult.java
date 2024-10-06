package uysnon.javataskrunner.dto;

import lombok.Data;


@Data
public class CodeExecutionResult {
    private String taskId;
    private String status;
    private String output;
    private String error;
    private ProcessExecutionStatistics processExecutionStatistics;
    private int exitCode;
}
