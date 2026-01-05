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
    private final SyntaxHighlighter syntaxHighlighter = new SyntaxHighlighter();

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

        // 监听生成的 PDF 路径
        this.viewModel.generatedPdfPathProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                File pdfFile = new File(newVal);
                if (pdfFile.exists()) {
                    try {
                        // 1. 读取数据 (这里已经有了 ViewModel 的 100ms 缓冲，更安全)
                        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
                        String base64 = Base64.getEncoder().encodeToString(pdfBytes);

                        // 2. 检查 WebView 是否已经加载了 viewer.html
                        String currentLoc = previewWebView.getEngine().getLocation();
                        boolean isAlreadyLoaded = currentLoc != null && currentLoc.endsWith("viewer.html");

                        // 定义通用的 JS 注入代码：调用 PDF.js 打开数据
                        // 我们定义一个 updatePDF 函数，如果不存在则等待
                        String updateJs =
                                "function updatePDF(base64Data) {" +
                                        "   try {" +
                                        "       var pdfData = atob(base64Data);" +
                                        "       var uint8Array = new Uint8Array(pdfData.length);" +
                                        "       for (var i = 0; i < pdfData.length; i++) {" +
                                        "           uint8Array[i] = pdfData.charCodeAt(i);" +
                                        "       }" +
                                        "       if (window.PDFViewerApplication) {" +
                                        "           window.PDFViewerApplication.open({ data: uint8Array });" +
                                        "       }" +
                                        "   } catch(e) { console.error('Update Error: ' + e); }" +
                                        "}" +
                                        // 立即尝试调用
                                        "if (window.PDFViewerApplication && window.PDFViewerApplication.open) {" +
                                        "    updatePDF('" + base64 + "');" +
                                        "}";

                        Platform.runLater(() -> {
                            if (isAlreadyLoaded) {
                                // === 方案 A: 软更新 (Soft Update) ===
                                // 页面已在，直接喂数据，不刷新页面
                                previewWebView.getEngine().executeScript(updateJs);
                            } else {
                                // === 方案 B: 首次加载 (First Load) ===
                                var viewerResource = getClass().getResource("pdfjs/web/viewer.html");
                                if (viewerResource == null) return;

                                previewWebView.getEngine().load(viewerResource.toExternalForm());

                                // 监听加载完成
                                previewWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
                                    if (newState == Worker.State.SUCCEEDED) {
                                        // 页面加载完后，还需要轮询等待 PDF.js 内部初始化完成
                                        String initJs =
                                                "var checkTimer = setInterval(function() {" +
                                                        "   if (window.PDFViewerApplication && window.PDFViewerApplication.open) {" +
                                                        "       clearInterval(checkTimer);" +
                                                        "       updatePDF('" + base64 + "');" +
                                                        "   }" +
                                                        "}, 100);" + // 每100ms检查一次
                                                        updateJs; // 附带上 updatePDF 函数定义

                                        previewWebView.getEngine().executeScript(initJs);
                                    }
                                });
                            }
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
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                return syntaxHighlighter.computeHighlighting(text);
            }
        };

        task.setOnSucceeded(event -> {
            StyleSpans<Collection<String>> spans = task.getValue();
            if (spans != null) {
                // FIX: 长度检查，防止 IndexOutOfBoundsException
                int currentLength = codeArea.getLength();
                int spansLength = spans.length();

                if (currentLength == spansLength) {
                    codeArea.setStyleSpans(0, spans);
                }
            }
        });

        // 提交任务到线程池
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