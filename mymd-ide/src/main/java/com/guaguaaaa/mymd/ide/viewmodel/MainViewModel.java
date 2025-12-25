package com.guaguaaaa.mymd.ide.viewmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.core.util.CslGenerator;
import com.guaguaaaa.mymd.core.ast.PandocNode;
import com.guaguaaaa.mymd.core.MyMDCompiler;
import com.guaguaaaa.mymd.core.api.CompilationResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel {

    private final StringProperty inputContent = new SimpleStringProperty("");
    private final StringProperty outputHtml = new SimpleStringProperty();
    private final StringProperty citationTemplate = new SimpleStringProperty();
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final BooleanProperty isCompiling = new SimpleBooleanProperty(false);

    public StringProperty inputContentProperty() { return inputContent; }
    public StringProperty outputHtmlProperty() { return outputHtml; }
    public StringProperty citationTemplateProperty() { return citationTemplate; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public BooleanProperty isCompilingProperty() { return isCompiling; }

    private String getPandocExecutable() {
        String pandocHome = System.getenv("PANDOC_HOME");
        if (pandocHome != null && !pandocHome.isEmpty()) {
            String separator = File.separator;
            if (!pandocHome.endsWith(separator)) {
                pandocHome += separator;
            }
            return pandocHome + "pandoc";
        }
        return "pandoc";
    }

    private File currentFile;

    // 获取当前文件对象
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * 从文件加载内容到输入框
     */
    public void loadFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        inputContent.set(content);
        this.currentFile = file;
    }

    /**
     * 将输入框内容保存到文件
     */
    public void saveFile(File file) throws IOException {
        // 先保存源文件 (Markdown)
        Files.writeString(file.toPath(), inputContent.get(), StandardCharsets.UTF_8);
        this.currentFile = file;

        // 触发后台 PDF 编译
        compilePdfInBackground(file);
    }

    /**
     * 后台 PDF 编译任务
     */
    private void compilePdfInBackground(File sourceFile) {
        // 如果已经在编译，不重复触发
        if (isCompiling.get()) return;

        isCompiling.set(true);
        statusMessage.set("Compiling PDF...");

        // 获取当前的 AST JSON 字符串 (在 UI 线程快速完成)
        String mymdText = inputContent.get();
        String jsonOutput;

        // 调用 Core 的编译器
        CompilationResult result = MyMDCompiler.compile(mymdText);

        if (result.hasErrors()) {
            statusMessage.set("Syntax Error: " + result.diagnostics.get(0).message);
            isCompiling.set(false);
            return;
        }

        jsonOutput = result.pandocJson;

        // 启动新线程运行 Pandoc
        Thread t = new Thread(() -> {
            try {
                // 准备输出文件名 (test.md -> test.pdf)
                String sourcePath = sourceFile.getAbsolutePath();
                String pdfPath = sourcePath.substring(0, sourcePath.lastIndexOf(".")) + ".pdf";

                // 准备资源文件 (Bib & CSL)
                File bibFile = getAssociatedBibFile();
                boolean useBib = bibFile.exists();
                File cslFile = new File("custom_style.csl");

                // 确保 CSL 存在
                if (!cslFile.exists()) {
                    String userTemplate = citationTemplate.get();
                    if (userTemplate == null || userTemplate.isBlank()) userTemplate = "{author} ({year}). {title}.";
                    String cslXml = CslGenerator.generateCslXml(userTemplate);
                    Files.writeString(cslFile.toPath(), cslXml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }

                // 构建 Pandoc 命令
                List<String> command = new ArrayList<>();
                command.add(getPandocExecutable());
                command.add("-f"); command.add("json");
                command.add("-t"); command.add("pdf");
                command.add("-o"); command.add(pdfPath);
                command.add("--pdf-engine=xelatex");
                command.add("-V"); command.add("mainfont=Hei");

                if (useBib) {
                    command.add("--citeproc");
                    command.add("--bibliography"); command.add(bibFile.getAbsolutePath());
                    command.add("--csl"); command.add(cslFile.getAbsolutePath());
                    command.add("--metadata=link-bibliography=false");
                }

                ProcessBuilder processBuilder = new ProcessBuilder(command);

                Process process = processBuilder.start();
                try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(jsonOutput);
                }

                // 读取错误流 (防止缓冲区满导致死锁)
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();

                // 更新 UI (回到 JavaFX 线程)
                javafx.application.Platform.runLater(() -> {
                    isCompiling.set(false);
                    if (exitCode == 0) {
                        statusMessage.set("PDF Saved: " + new File(pdfPath).getName());
                    } else {
                        statusMessage.set("PDF Error (Code " + exitCode + ")");
                        System.err.println("Pandoc Error:\n" + errorOutput);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    isCompiling.set(false);
                    statusMessage.set("Error: " + e.getMessage());
                });
            }
        });

        t.setDaemon(true);
        t.start();
    }

    /**
     * 根据当前 Markdown 文件，推算同名的 .bib 文件路径
     * 例如：/path/to/paper.md -> /path/to/paper.bib
     */
    private File getAssociatedBibFile() {
        if (this.currentFile == null) {
            // 如果还没保存过文件，暂时回退到默认的 test.bib
            return new File("test.bib");
        }

        String mdPath = this.currentFile.getAbsolutePath();
        String bibPath;
        if (mdPath.lastIndexOf(".") > 0) {
            bibPath = mdPath.substring(0, mdPath.lastIndexOf(".")) + ".bib";
        } else {
            bibPath = mdPath + ".bib";
        }

        return new File(bibPath);
    }

    /**
     * 动态生成 CSL 并调用 Pandoc 处理引用
     */
    public void convertToHtml() throws IOException, InterruptedException {
        String mymdText = inputContent.get();

        // 使用 MyMDCompiler 替代手动解析
        CompilationResult result = MyMDCompiler.compile(mymdText);

        if (result.hasErrors()) {
            // 如果有语法错误，直接在预览区显示错误信息
            outputHtml.set("<h3>Syntax Error</h3><p>" + result.diagnostics.get(0).message + "</p>");
            return;
        }

        String jsonOutput = result.pandocJson;

        // --- 调试：打印一下生成的 JSON，确认 AST 里是 Cite 对象而不是 Str ---
        System.out.println("DEBUG: Generated JSON AST fragment: " +
                (jsonOutput.length() > 500 ? jsonOutput.substring(0, 500) + "..." : jsonOutput));

        // 准备文件 (使用绝对路径)
        File bibFile = getAssociatedBibFile();
        File cslFile = new File("custom_style.csl");

        boolean useBib = bibFile.exists();

        // 检查文件是否存在
        if (!bibFile.exists()) {
            outputHtml.set("Error: Cannot find test.bib at: " + bibFile.getAbsolutePath());
            return;
        }

        // 生成 CSL
        String userCitationTemplate = citationTemplate.get();
        if (userCitationTemplate == null || userCitationTemplate.isBlank()) {
            // 给一个默认值兜底
            userCitationTemplate = "{author} ({year}). {title}.";
        }
        String cslXmlContent = CslGenerator.generateCslXml(userCitationTemplate);
        Files.writeString(cslFile.toPath(), cslXmlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Pandoc
        // 构建 ProcessBuilder
        List<String> command = new ArrayList<>();
        command.add(getPandocExecutable());
        command.add("-f"); command.add("json");
        command.add("-t"); command.add("html");

        if (useBib) {
            command.add("--citeproc");
            command.add("--bibliography"); command.add(bibFile.getAbsolutePath());
            command.add("--csl"); command.add(cslFile.getAbsolutePath());
            command.add("--metadata=link-bibliography=false");
        }
        command.add("--metadata=link-citations=true");

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        Process process = processBuilder.start();
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
        }

        // 读取标准输出 (HTML)
        StringBuilder htmlOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                htmlOutput.append(line).append("\n");
            }
        }

        // 读取错误输出 (Stderr)
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            // 注入 CSS 样式
            String css = """
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; line-height: 1.6; padding: 20px; max-width: 800px; margin: 0 auto; }
                    blockquote { border-left: 4px solid #dfe2e5; color: #6a737d; margin: 0; padding-left: 1em; }
                    hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; }
                    img { max-width: 100%; box-shadow: 0 4px 8px rgba(0,0,0,0.1); border-radius: 4px; }
                    a { color: #0366d6; text-decoration: none; }
                    a:hover { text-decoration: underline; }
                    .citation { color: #0366d6; }
                </style>
                """;

            outputHtml.set(css + htmlOutput.toString());
        } else {
            outputHtml.set("Error: Pandoc failed (Exit Code " + exitCode + ")\n" + errorOutput.toString());
        }
    }

    /**
     * Converts the MyMD content to LaTeX and saves it to a specified file.
     * This method follows the same two-step conversion pipeline as {@link #convertToHtml()}.
     *
     * @param outputFile The file to which the LaTeX content will be saved.
     * @throws IOException          if an I/O error occurs.
     * @throws InterruptedException if the process is interrupted.
     */
    public void saveAsLatex(File outputFile) throws IOException, InterruptedException {
        String mymdText = inputContent.get();

        CompilationResult result = MyMDCompiler.compile(mymdText);

        if (result.hasErrors()) {
            // 这里可以抛出异常或者在 UI 提示，这里简单抛出异常让 View 层捕获
            throw new IOException("Syntax Error: " + result.diagnostics.get(0).message);
        }

        String jsonOutput = result.pandocJson;

        ProcessBuilder processBuilder = new ProcessBuilder(
                getPandocExecutable(),
                "-f", "json",
                "-t", "latex",
                "-o", outputFile.getAbsolutePath()
        );

        Process process = processBuilder.start();
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                StringBuilder errorText = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorText.append(errorLine).append("\n");
                }
                outputHtml.set("Error: Pandoc failed to save LaTeX file with exit code " + exitCode + "\n" + errorText);
            }
        }
    }
}