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

/**
 * The ViewModel for the main application, handling the core logic of text conversion.
 * It manages the input and output content properties and orchestrates the conversion
 * process using ANTLR and an external Pandoc executable.
 */

public class MainViewModel {

    private final StringProperty inputContent = new SimpleStringProperty();
    private final StringProperty outputHtml = new SimpleStringProperty();

    /**
     * Provides access to the input text property for data binding.
     * @return The StringProperty for the input text.
     */
    public StringProperty inputContentProperty() {
        return inputContent;
    }

    /**
     * Provides access to the output HTML property for data binding.
     * @return The StringProperty for the output HTML.
     */
    public StringProperty outputHtmlProperty() {
        return outputHtml;
    }

    /**
     * Determines the path to the Pandoc executable.
     * It first checks the PANDOC_HOME environment variable; if not found, it defaults to "pandoc".
     * @return The path to the Pandoc executable.
     */

    private String getPandocExecutable() {
        String pandocHome = System.getenv("PANDOC_HOME");
        if (pandocHome != null && !pandocHome.isEmpty()) {
            // Ensure the path ends with a file separator
            String separator = File.separator;
            if (!pandocHome.endsWith(separator)) {
                pandocHome += separator;
            }
            return pandocHome + "pandoc";
        }
        return "pandoc";
    }

    /**
     * Converts the MyMD content from the input property to HTML.
     * This method implements a two-step conversion pipeline:
     * 1. The MyMD text is parsed into an ANTLR parse tree.
     * 2. A custom visitor transforms the parse tree into a Pandoc JSON AST.
     * 3. The JSON is piped to the Pandoc command-line tool, which performs the final HTML conversion.
     *
     * @throws IOException          if an I/O error occurs.
     * @throws InterruptedException if the process is interrupted.
     */
    public void convertToHtml() throws IOException, InterruptedException {
        String mymdText = inputContent.get();

        // Parse MyMD content and generate Pandoc AST (JSON)
        CharStream input = CharStreams.fromString(mymdText);
        MyMDLexer lexer = new MyMDLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MyMDParser parser = new MyMDParser(tokens);
        ParseTree tree = parser.document();
        PandocAstVisitor visitor = new PandocAstVisitor();
        PandocNode ast = visitor.visit(tree);
        Gson gson = new GsonBuilder().create();
        String jsonOutput = gson.toJson(ast);

        // Call Pandoc process to convert AST to HTML
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