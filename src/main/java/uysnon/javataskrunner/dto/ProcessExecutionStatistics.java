package uysnon.javataskrunner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessExecutionStatistics {
    private long timeMs;
    private long processAvgMemoryUsageBytes;
    private long processMaxMemoryUsageBytes;
    private int exitCode;
}
