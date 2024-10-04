package uysnon.javataskrunner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uysnon.javataskrunner.dto.CodeExecutionRequest;
import uysnon.javataskrunner.dto.CodeExecutionResult;
import uysnon.javataskrunner.kafka.producer.producer.CodeExecutionProducer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CodeExecutionService {

    private final CodeExecutionProducer codeExecutionProducer;

    public void executeCode(CodeExecutionRequest request) {
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
            int compileExitCode = compileCode(taskDir);
            if (compileExitCode != 0) {
                String compileErrors = readFile(taskDir.resolve("compile_errors.txt"));
                sendErrorResult(request.getTaskId(), "Ошибка компиляции:\n" + compileErrors);
                return;
            }

            // Выполнение кода
            int runExitCode = runCode(taskDir, request);
            String runOutput = readFile(taskDir.resolve("run_output.txt"));
            String runErrors = readFile(taskDir.resolve("run_errors.txt"));

            // Отправка результата
            CodeExecutionResult result = new CodeExecutionResult();
            result.setTaskId(request.getTaskId());
            result.setStatus(runExitCode == 0 ? "success" : "error");
            result.setOutput(runOutput);
            result.setError(runErrors);
            result.setExitCode(runExitCode);

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

    private int compileCode(Path taskDir) throws IOException, InterruptedException {
        ProcessBuilder compileProcessBuilder = new ProcessBuilder();
        compileProcessBuilder.command("javac", "-d", taskDir.toString());

        // Добавляем все .java файлы в команду компиляции
        Files.walk(taskDir)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> compileProcessBuilder.command().add(path.toString()));

        compileProcessBuilder.directory(taskDir.toFile());
        compileProcessBuilder.redirectError(taskDir.resolve("compile_errors.txt").toFile());

        Process compileProcess = compileProcessBuilder.start();
        return compileProcess.waitFor();
    }

    private int runCode(Path taskDir, CodeExecutionRequest request) throws IOException, InterruptedException {
        List<String> runCommand = new ArrayList<>();
        runCommand.add("java");
        runCommand.add("-cp");
        runCommand.add(taskDir.toString());
        runCommand.add(request.getMainClass());

        // Добавляем аргументы, если они есть
        if (request.getArguments() != null) {
            runCommand.addAll(request.getArguments());
        }

        ProcessBuilder runProcessBuilder = new ProcessBuilder(runCommand);
        runProcessBuilder.directory(taskDir.toFile());
        runProcessBuilder.redirectOutput(taskDir.resolve("run_output.txt").toFile());
        runProcessBuilder.redirectError(taskDir.resolve("run_errors.txt").toFile());

        Process runProcess = runProcessBuilder.start();

        // Передача stdin, если имеется
        if (request.getStdin() != null && !request.getStdin().isEmpty()) {
            OutputStream stdin = runProcess.getOutputStream();
            stdin.write(request.getStdin().getBytes());
            stdin.flush();
            stdin.close();
        }

        // Установка тайм-аута
        int timeout = request.getTimeout() != null ? request.getTimeout() : 5;
        if (!runProcess.waitFor(timeout, TimeUnit.SECONDS)) {
            runProcess.destroyForcibly();
            throw new RuntimeException("Превышено время выполнения программы.");
        }

        return runProcess.exitValue();
    }

    private String readFile(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            return Files.readString(filePath);
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