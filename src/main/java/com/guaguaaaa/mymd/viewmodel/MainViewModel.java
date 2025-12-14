package com.guaguaaaa.mymd.viewmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guaguaaaa.mymd.CslGenerator; // 引入刚刚写的生成器
import com.guaguaaaa.mymd.MyMDLexer;
import com.guaguaaaa.mymd.MyMDParser;
import com.guaguaaaa.mymd.PandocAstVisitor;
import com.guaguaaaa.mymd.pandoc.PandocNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class MainViewModel {

    private final StringProperty inputContent = new SimpleStringProperty();
    private final StringProperty outputHtml = new SimpleStringProperty();
    private final StringProperty citationTemplate = new SimpleStringProperty();

    public StringProperty inputContentProperty() { return inputContent; }
    public StringProperty outputHtmlProperty() { return outputHtml; }
    public StringProperty citationTemplateProperty() { return citationTemplate; }

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

    // 获取当前文件对象（View层可能需要用它来判断是调用 Save 还是 SaveAs）
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
    }

    /**
     * 核心修改：动态生成 CSL 并调用 Pandoc 处理引用
     */
    /**
     * 核心修改：使用绝对路径，并打印 stderr 以便调试
     */
    public void convertToHtml() throws IOException, InterruptedException {
        String mymdText = inputContent.get();

        // 1. 生成 AST (保持不变)
        CharStream input = CharStreams.fromString(mymdText);
        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();
        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);
        Gson gson = new GsonBuilder().create();
        String jsonOutput = gson.toJson(ast);

        // --- 调试：打印一下生成的 JSON，确认 AST 里是 Cite 对象而不是 Str ---
        System.out.println("DEBUG: Generated JSON AST fragment: " +
                (jsonOutput.length() > 500 ? jsonOutput.substring(0, 500) + "..." : jsonOutput));

        // 2. 准备文件 (使用绝对路径！)
        File bibFile = new File("test.bib");
        File cslFile = new File("custom_style.csl");

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

        // 3. 调用 Pandoc
        ProcessBuilder processBuilder = new ProcessBuilder(
                getPandocExecutable(),
                "-f", "json",
                "-t", "html",
                "--citeproc",
                "--bibliography", bibFile.getAbsolutePath(),
                "--csl", cslFile.getAbsolutePath(),
                "--metadata=link-citations=true",
                "--metadata=link-bibliography=false"
        );

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

        // 读取错误输出 (Stderr) - 关键！无论 exitCode 是多少都要读
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
                    /* 修复引用点击跳转不可见的问题 */
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

        CharStream input = CharStreams.fromString(mymdText);
        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();
        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);
        Gson gson = new GsonBuilder().create();
        String jsonOutput = gson.toJson(ast);

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