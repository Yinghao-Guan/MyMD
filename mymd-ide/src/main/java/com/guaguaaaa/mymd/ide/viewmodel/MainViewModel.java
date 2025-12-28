package com.guaguaaaa.mymd.ide.viewmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.core.util.CslGenerator;
import com.guaguaaaa.mymd.core.ast.PandocNode;
import com.guaguaaaa.mymd.core.MyMDCompiler;
import com.guaguaaaa.mymd.core.api.CompilationResult;
import com.guaguaaaa.mymd.core.api.Diagnostic;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
public class MainViewModel {

    private final StringProperty inputContent = new SimpleStringProperty("");
    private final StringProperty generatedPdfPath = new SimpleStringProperty();

    private final StringProperty citationTemplate = new SimpleStringProperty();
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final BooleanProperty isCompiling = new SimpleBooleanProperty(false);
    private final ObservableList<Diagnostic> diagnostics = FXCollections.observableArrayList();

    public StringProperty inputContentProperty() { return inputContent; }
    public StringProperty generatedPdfPathProperty() { return generatedPdfPath; }

    public StringProperty citationTemplateProperty() { return citationTemplate; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public BooleanProperty isCompilingProperty() { return isCompiling; }
    public ObservableList<Diagnostic> getDiagnostics() { return diagnostics; }

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
        Files.writeString(file.toPath(), inputContent.get(), StandardCharsets.UTF_8);
        this.currentFile = file;

        compilePdfInBackground(file);
    }

    /**
     * 后台 PDF 编译任务
     */
    private void compilePdfInBackground(File sourceFile) {
        if (isCompiling.get()) return;

        isCompiling.set(true);
        statusMessage.set("Compiling...");

        String mymdText = inputContent.get();
        if (mymdText == null || mymdText.isBlank()) {
            isCompiling.set(false);
            statusMessage.set("Skipped: Content is empty");
            return;
        }

        CompilationResult result = MyMDCompiler.compile(mymdText);

        Platform.runLater(() -> {
            diagnostics.setAll(result.diagnostics);
        });

        if (result.hasErrors()) {
            Platform.runLater(() -> {
                statusMessage.set("Syntax Error: " + result.diagnostics.get(0).message);
                isCompiling.set(false);
            });
            return;
        }

        String jsonOutput = result.pandocJson;

        Thread t = new Thread(() -> {
            try {
                String sourcePath = sourceFile.getAbsolutePath();
                String basePath = sourcePath.lastIndexOf(".") > 0 ?
                        sourcePath.substring(0, sourcePath.lastIndexOf(".")) :
                        sourcePath;
                String pdfPath = basePath + ".pdf";
                String texPath = basePath + ".tex";

                File bibFile = getAssociatedBibFile();
                boolean useBib = bibFile.exists();
                File cslFile = new File("custom_style.csl");

                if (!cslFile.exists()) {
                    String userTemplate = citationTemplate.get();
                    if (userTemplate == null || userTemplate.isBlank()) userTemplate = "{author} ({year}). {title}.";
                    String cslXml = CslGenerator.generateCslXml(userTemplate);
                    Files.writeString(cslFile.toPath(), cslXml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }

                List<String> texCommand = new ArrayList<>();
                texCommand.add(getPandocExecutable());
                texCommand.add("-f"); texCommand.add("json");
                texCommand.add("-t"); texCommand.add("latex");
                texCommand.add("-s");
                texCommand.add("-o"); texCommand.add(texPath);

                if (useBib) {
                    texCommand.add("--citeproc");
                    texCommand.add("--bibliography"); texCommand.add(bibFile.getAbsolutePath());
                    texCommand.add("--csl"); texCommand.add(cslFile.getAbsolutePath());
                    texCommand.add("--metadata=link-bibliography=false");
                }

                ProcessBuilder texPb = new ProcessBuilder(texCommand);
                Process texProcess = texPb.start();
                try (OutputStreamWriter writer = new OutputStreamWriter(texProcess.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(jsonOutput);
                }
                int texExit = texProcess.waitFor();
                if (texExit != 0) {
                    System.err.println("Failed to generate .tex file. Exit code: " + texExit);
                } else {
                    System.out.println("LaTeX file generated: " + texPath);
                }

                List<String> pdfCommand = new ArrayList<>();
                pdfCommand.add(getPandocExecutable());
                pdfCommand.add("-f"); pdfCommand.add("json");
                pdfCommand.add("-t"); pdfCommand.add("pdf");
                pdfCommand.add("-o"); pdfCommand.add(pdfPath);
                pdfCommand.add("--pdf-engine=xelatex");
                // 保持字体设置
                // command.add("-V"); command.add("mainfont=PingFang SC");
                pdfCommand.add("-V"); pdfCommand.add("mainfont=Microsoft YaHei");

                if (useBib) {
                    pdfCommand.add("--citeproc");
                    pdfCommand.add("--bibliography"); pdfCommand.add(bibFile.getAbsolutePath());
                    pdfCommand.add("--csl"); pdfCommand.add(cslFile.getAbsolutePath());
                    pdfCommand.add("--metadata=link-bibliography=false");
                }

                ProcessBuilder pdfPb = new ProcessBuilder(pdfCommand);
                Process pdfProcess = pdfPb.start();
                try (OutputStreamWriter writer = new OutputStreamWriter(pdfProcess.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(jsonOutput);
                }

                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pdfProcess.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                }

                int exitCode = pdfProcess.waitFor();

                if (exitCode == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }

                Platform.runLater(() -> {
                    isCompiling.set(false);
                    if (exitCode == 0) {
                        statusMessage.set("Saved: " + new File(pdfPath).getName() + " (& .tex)");

                        generatedPdfPath.set(null);
                        generatedPdfPath.set(pdfPath);

                    } else {
                        statusMessage.set("PDF Error (Code " + exitCode + ")");
                        System.err.println("Pandoc Error:\n" + errorOutput);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
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
                statusMessage.set("Error: Pandoc failed to save LaTeX file with exit code " + exitCode + "\n" + errorText);
            }
        }
    }
}