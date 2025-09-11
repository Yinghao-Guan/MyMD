package com.guaguaaaa.mymd.viewmodel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public class MainViewModel {

    private final StringProperty inputContent = new SimpleStringProperty();
    private final StringProperty outputHtml = new SimpleStringProperty();

    public StringProperty inputContentProperty() {
        return inputContent;
    }

    public StringProperty outputHtmlProperty() {
        return outputHtml;
    }

    private String getPandocExecutable() {
        String pandocHome = System.getenv("PANDOC_HOME");
        if (pandocHome != null && !pandocHome.isEmpty()) {
            // 确保路径末尾有文件分隔符
            String separator = File.separator;
            if (!pandocHome.endsWith(separator)) {
                pandocHome += separator;
            }
            return pandocHome + "pandoc";
        }
        return "pandoc"; // 回退到默认行为
    }

    // 将原有的 convert() 方法重命名为 convertToHtml()
    public void convertToHtml() throws IOException, InterruptedException {
        String mymdText = inputContent.get();

        // 1. Parse MyMD content and generate Pandoc AST (JSON)
        CharStream input = CharStreams.fromString(mymdText);
        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();
        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);
        Gson gson = new GsonBuilder().create();
        String jsonOutput = gson.toJson(ast);

        // 2. Call Pandoc process to convert AST to HTML
        ProcessBuilder processBuilder = new ProcessBuilder(
                getPandocExecutable(),
                "-f", "json",
                "-t", "html"
        );


        Process process = processBuilder.start();
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
        }

        StringBuilder htmlOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                htmlOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            outputHtml.set(htmlOutput.toString());
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                StringBuilder errorText = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorText.append(errorLine).append("\n");
                }
                outputHtml.set("Error: Pandoc failed with exit code " + exitCode + "\n" + errorText);
            }
        }
    }

    // 新增的方法来处理保存为 LaTeX
    public void saveAsLatex(File outputFile) throws IOException, InterruptedException {
        String mymdText = inputContent.get();

        // 1. Parse MyMD content and generate Pandoc AST (JSON)
        CharStream input = CharStreams.fromString(mymdText);
        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();
        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);
        Gson gson = new GsonBuilder().create();
        String jsonOutput = gson.toJson(ast);

        // 2. Call Pandoc process to convert AST to LaTeX and save to file
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
            // 如果 Pandoc 失败，将错误信息显示在 UI 的 HTML 预览区域
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