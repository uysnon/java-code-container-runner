package uysnon.javataskrunner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;
import uysnon.javataskrunner.dto.CodeExecutionRequest;
import uysnon.javataskrunner.dto.CodeExecutionResult;
import uysnon.javataskrunner.dto.ProcessExecutionStatistics;
import uysnon.javataskrunner.kafka.monitoring.MonitorRunnable;
import uysnon.javataskrunner.kafka.producer.producer.CodeExecutionProducer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeExecutionService {

    private final CodeExecutionProducer codeExecutionProducer;

    public void executeCode(String javaBinPath, CodeExecutionRequest request) {
        // Создаем временную директорию для задачи
        Path taskDir;
        try {
            taskDir = Files.createTempDirectory("task_" + (request.getTaskId() != null ? request.getTaskId() : UUID.randomUUID()));
        } catch (IOException e) {
            sendErrorResult(request.getTaskId(), "Ошибка при создании временной директории: " + e.getMessage());
            return;
        }

        try {
            // Сохранение файлов
            if (request.getFiles() != null && !request.getFiles().isEmpty()) {
                for (var file : request.getFiles()) {
                    Path filePath = taskDir.resolve(file.getName());
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, file.getContent().getBytes());
                }
            } else {
                sendErrorResult(request.getTaskId(), "Нет файлов для обработки в задаче.");
                return;
            }

            // Компиляция кода
            int compileExitCode = compileCode(javaBinPath, taskDir);
            if (compileExitCode != 0) {
                String compileErrors = readFile(taskDir.resolve("compile_errors.txt"));
                sendErrorResult(request.getTaskId(), "Ошибка компиляции:\n" + compileErrors);
                return;
            }

            // Выполнение кода
            CodeExecutionResult result = runCode(javaBinPath, taskDir, request);

            // Отправка результата
            codeExecutionProducer.sendResult(result);

        } catch (Exception e) {
            sendErrorResult(request.getTaskId(), "Ошибка при выполнении кода: " + e.getMessage());
        } finally {
            // Очистка временной директории
            try {
                Files.walk(taskDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                // Логируем ошибку, но не прерываем выполнение
                e.printStackTrace();
            }
        }
    }

    private int compileCode(String javaBinPath, Path taskDir) throws IOException, InterruptedException {
        ProcessBuilder compileProcessBuilder = new ProcessBuilder();
        compileProcessBuilder.command(javaBinPath + File.separator + "javac", "-d", taskDir.toString());

        // Добавляем все .java файлы в команду компиляции
        Files.walk(taskDir)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> compileProcessBuilder.command().add(path.toString()));

        compileProcessBuilder.directory(taskDir.toFile());
        compileProcessBuilder.redirectError(taskDir.resolve("compile_errors.txt").toFile());

        Process compileProcess = compileProcessBuilder.start();
        return compileProcess.waitFor();
    }

    private CodeExecutionResult runCode(String javaBinPath, Path taskDir, CodeExecutionRequest request) throws IOException, InterruptedException {
        List<String> runCommand = new ArrayList<>();

        runCommand.addAll(List.of(
                javaBinPath + File.separator + "java",
                "-cp",
                taskDir.toString(),
                request.getMainClass()
        ));


        // Добавляем аргументы, если они есть
        if (request.getArguments() != null) {
            runCommand.addAll(request.getArguments());
        }

        ProcessBuilder runProcessBuilder = new ProcessBuilder(runCommand);
        runProcessBuilder.directory(taskDir.toFile());
        runProcessBuilder.redirectOutput(taskDir.resolve("run_output.txt").toFile());
        runProcessBuilder.redirectError(taskDir.resolve("run_errors.txt").toFile());


        // Используем OSHI для получения информации о процессе
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();


        Process runProcess = runProcessBuilder.start();
        long startTimeMillis = System.currentTimeMillis();
        long pid = runProcess.pid();
        log.debug("Запущен процесс с pid = {}", pid);

        // Начинаем мониторинг в отдельном потоке
        MonitorRunnable monitor = new MonitorRunnable(os, pid);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        // Передача stdin, если имеется
        if (request.getStdin() != null && !request.getStdin().isEmpty()) {
            OutputStream stdin = runProcess.getOutputStream();
            stdin.write(request.getStdin().getBytes());
            stdin.flush();
            stdin.close();
        }

        // Установка тайм-аута
        int timeout = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 10;
        if (!runProcess.waitFor(timeout, TimeUnit.SECONDS)) {
            runProcess.destroyForcibly();
            throw new RuntimeException("Превышено время выполнения программы.");
        }
        long endTimeMillis = System.currentTimeMillis();

        monitor.stopMonitoring();
        monitorThread.join();

        ProcessExecutionStatistics processExecutionStatistics = ProcessExecutionStatistics
                .builder()
                .timeMs(endTimeMillis - startTimeMillis)
                .processAvgMemoryUsageBytes((long) monitor.getResult().getAvgMemoryInBytes())
                .processMaxMemoryUsageBytes(monitor.getResult().getMaxMemoryInBytes())
                .exitCode(runProcess.exitValue())
                .build();

        String runOutput = readFile(taskDir.resolve("run_output.txt"));
        String runErrors = readFile(taskDir.resolve("run_errors.txt"));

        CodeExecutionResult result = new CodeExecutionResult();
        result.setTaskId(request.getTaskId());
        result.setStatus(processExecutionStatistics.getExitCode() == 0 ? "success" : "error");
        result.setOutput(runOutput);
        result.setError(runErrors);
        result.setProcessExecutionStatistics(processExecutionStatistics);

        return result;
    }

    private String readFile(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        }
        return "";
    }

    private void sendErrorResult(String taskId, String errorMessage) {
        CodeExecutionResult result = new CodeExecutionResult();
        result.setTaskId(taskId);
        result.setStatus("error");
        result.setError(errorMessage);
        result.setExitCode(-1);

        codeExecutionProducer.sendResult(result);
    }

}