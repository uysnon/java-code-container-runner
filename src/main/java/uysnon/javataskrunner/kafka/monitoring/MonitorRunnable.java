package uysnon.javataskrunner.kafka.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import uysnon.javataskrunner.dto.ProcessExecutionStatistics;

import java.util.ArrayList;
import java.util.List;

public class MonitorRunnable implements Runnable {


    private final OperatingSystem os;
    private final long pid;
    private volatile boolean running = true;
    private final List<Long> memoryUsageBytesStatList = new ArrayList<>();

    @Getter
    private Result result;

    private ProcessExecutionStatistics processExecutionStatistics;

    public MonitorRunnable(OperatingSystem os, long pid) {
        this.os = os;
        this.pid = pid;
    }

    @Override
    public void run() {
        while (running) {
            OSProcess process = os.getProcess((int) pid);
            if (process != null) {
                memoryUsageBytesStatList.add(process.getResidentSetSize());
                try {
                    Thread.sleep(20); // Интервал мониторинга (500 мс)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            double avgMemory = memoryUsageBytesStatList.stream().mapToLong(l -> l).average().orElse(0.0);
            long maxMemory = memoryUsageBytesStatList.stream().mapToLong(l -> l).max().orElse(0L);
            result = new Result(avgMemory, maxMemory);
        }
    }

    public void stopMonitoring() {
        running = false;
    }

    @Getter
    @AllArgsConstructor
    public static class Result {

        private double avgMemoryInBytes;

        private long maxMemoryInBytes;
    }
}

