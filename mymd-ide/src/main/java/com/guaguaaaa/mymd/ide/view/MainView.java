package com.guaguaaaa.mymd.ide.view;

import com.guaguaaaa.mymd.core.api.Diagnostic;
import com.guaguaaaa.mymd.ide.viewmodel.MainViewModel;
import com.guaguaaaa.mymd.ide.util.SyntaxHighlighter;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.scene.layout.StackPane;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.concurrent.Worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Base64;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.io.File;
import java.io.IOException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import netscape.javascript.JSObject;

public class MainView {

    @FXML private WebView previewWebView;
    @FXML private javafx.scene.control.TextField templateField;
    @FXML private javafx.scene.control.Label statusLabel;
    @FXML private javafx.scene.control.ProgressBar progressBar;
    @FXML private StackPane editorContainer;

    private CodeArea codeArea;
    private MainViewModel viewModel;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<Diagnostic> currentDiagnostics = new ArrayList<>();

    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;

        this.codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStylesheets().add(getClass().getResource("editor.css").toExternalForm());
        codeArea.setStyle("-fx-font-family: 'Monospaced', 'Consolas', 'Courier New'; -fx-font-size: 14;");
        editorContainer.getChildren().add(codeArea);

        String content = this.viewModel.inputContentProperty().get();
        codeArea.replaceText(0, 0, content == null ? "" : content);

        this.viewModel.inputContentProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(codeArea.getText())) {
                codeArea.replaceText(newVal);
            }
        });

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            this.viewModel.inputContentProperty().set(newText);
            computeHighlightingAsync(newText);
        });

        viewModel.getDiagnostics().addListener((javafx.collections.ListChangeListener.Change<? extends Diagnostic> c) -> {
            this.currentDiagnostics = new ArrayList<>(viewModel.getDiagnostics());
            computeHighlightingAsync(codeArea.getText());
        });

        this.viewModel.generatedPdfPathProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                File pdfFile = new File(newVal);
                if (pdfFile.exists()) {
                    try {
                        // 1. 读取资源路径
                        var viewerResource = getClass().getResource("pdfjs/web/viewer.html");
                        if (viewerResource == null) {
                            System.err.println("Error: Could not find pdfjs/web/viewer.html");
                            return;
                        }
                        String viewerUrl = viewerResource.toExternalForm();

                        // 2. 读取 PDF 并转 Base64
                        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
                        String base64 = Base64.getEncoder().encodeToString(pdfBytes);

                        // 3. 加载 viewer.html
                        Platform.runLater(() -> {
                            // 添加一个简单的 Java 控制台桥接，方便在 IDEA 控制台看到 JS 报错
                            previewWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                                if (newState == Worker.State.SUCCEEDED) {
                                    // 注入 JavaBridge 用于调试
                                    JSObject window = (JSObject) previewWebView.getEngine().executeScript("window");
                                    window.setMember("javaConsole", new JavaConsoleBridge());

                                    // 重定向 JS console.log 到 Java System.out
                                    previewWebView.getEngine().executeScript(
                                            "console.log = function(message) { javaConsole.log(message); };" +
                                                    "console.error = function(message) { javaConsole.error(message); };"
                                    );

                                    // 4. 核心修复：使用 setInterval 轮询等待 PDFViewerApplication 初始化
                                    String jsCode =
                                            "function loadPdfLoop() {" +
                                                    "   var checkInterval = setInterval(function() {" +
                                                    "       if (window.PDFViewerApplication && window.PDFViewerApplication.open) {" +
                                                    "           clearInterval(checkInterval);" +
                                                    "           console.log('PDF.js ready. Opening PDF...');" +
                                                    "           try {" +
                                                    "               var pdfData = atob('" + base64 + "');" +
                                                    "               var uint8Array = new Uint8Array(pdfData.length);" +
                                                    "               for (var i = 0; i < pdfData.length; i++) {" +
                                                    "                   uint8Array[i] = pdfData.charCodeAt(i);" +
                                                    "               }" +
                                                    "               window.PDFViewerApplication.open({ data: uint8Array });" +
                                                    "           } catch (e) { console.error('Load error: ' + e); }" +
                                                    "       } else {" +
                                                    "           console.log('Waiting for PDF.js...');" +
                                                    "       }" +
                                                    "   }, 200);" + // 每 200ms 检查一次
                                                    "}" +
                                                    "loadPdfLoop();";

                                    previewWebView.getEngine().executeScript(jsCode);
                                }
                            });

                            previewWebView.getEngine().load(viewerUrl);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        templateField.textProperty().bindBidirectional(this.viewModel.citationTemplateProperty());
        statusLabel.textProperty().bind(this.viewModel.statusMessageProperty());

        progressBar.visibleProperty().bind(this.viewModel.isCompilingProperty());
        progressBar.progressProperty().bind(
                javafx.beans.binding.Bindings.when(this.viewModel.isCompilingProperty())
                        .then(-1.0)
                        .otherwise(0.0)
        );
    }

    public static class JavaConsoleBridge {
        public void log(String text) {
            System.out.println("JS LOG: " + text);
        }
        public void error(String text) {
            System.err.println("JS ERROR: " + text);
        }
    }

    private void computeHighlightingAsync(String text) {
        List<Diagnostic> diagnosticsSnapshot = new ArrayList<>(this.currentDiagnostics);

        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                StyleSpans<Collection<String>> syntaxSpans = SyntaxHighlighter.computeHighlighting(text);
                StyleSpans<Collection<String>> errorSpans = SyntaxHighlighter.computeErrorHighlighting(text, diagnosticsSnapshot);
                return syntaxSpans.overlay(errorSpans, (syntaxStyle, errorStyle) -> {
                    Collection<String> combined = new ArrayList<>(syntaxStyle);
                    combined.addAll(errorStyle);
                    return combined;
                });
            }
        };

        task.setOnSucceeded(event -> {
            if (codeArea.getLength() == text.length()) {
                codeArea.setStyleSpans(0, task.getValue());
            }
        });

        executor.execute(task);
    }

    @FXML
    private void handleSaveAsLatex() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as LaTeX File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LaTeX Files", "*.tex"));
        fileChooser.setInitialFileName("output.tex");
        File file = fileChooser.showSaveDialog(editorContainer.getScene().getWindow());
        if (file != null) {
            try {
                viewModel.saveAsLatex(file);
            } catch (IOException | InterruptedException e) {
                showErrorDialog("Save Failed", "An error occurred while saving the LaTeX file.", e.toString());
                e.printStackTrace();
            }
        }
    }

    private void showErrorDialog(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        TextArea textArea = new TextArea(contentText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.mymd", "*.txt"));
        File initialDir = new File("sandbox");
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }
        File file = fileChooser.showOpenDialog(editorContainer.getScene().getWindow());
        if (file != null) {
            try {
                viewModel.loadFile(file);
            } catch (IOException e) {
                showErrorDialog("Open Failed", "Could not load file.", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSave() {
        File currentFile = viewModel.getCurrentFile();
        if (currentFile != null) {
            try {
                viewModel.saveFile(currentFile);
            } catch (IOException e) {
                showErrorDialog("Save Failed", "Could not save file.", e.getMessage());
            }
        } else {
            handleSaveAs();
        }
    }

    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.mymd"));
        File initialDir = new File("sandbox");
        if (initialDir.exists()) fileChooser.setInitialDirectory(initialDir);
        File file = fileChooser.showSaveDialog(editorContainer.getScene().getWindow());
        if (file != null) {
            try {
                viewModel.saveFile(file);
            } catch (IOException e) {
                showErrorDialog("Save Failed", "Could not save file.", e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @FXML
    private void handleExit() {
        shutdown();
        javafx.application.Platform.exit();
    }
}