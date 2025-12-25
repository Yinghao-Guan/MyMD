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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.io.File;
import java.io.IOException;

/**
 * Controller class for the main application view.
 * This class handles user interactions and binds the view components to the ViewModel.
 */
public class MainView {

    @FXML
    private WebView previewWebView;
    @FXML
    private javafx.scene.control.TextField templateField;

    @FXML private javafx.scene.control.Label statusLabel;
    @FXML private javafx.scene.control.ProgressBar progressBar;

    // 用于放置 RichTextFX CodeArea 的容器
    @FXML
    private StackPane editorContainer;

    // 核心编辑器组件
    private CodeArea codeArea;

    private MainViewModel viewModel;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<Diagnostic> currentDiagnostics = new ArrayList<>();

    /**
     * Sets the ViewModel and establishes data bindings between the view and the ViewModel.
     * @param viewModel The ViewModel instance to be used by this controller.
     */
    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;

        // 初始化 CodeArea (IDE 风格编辑器)
        this.codeArea = new CodeArea();

        // 启用行号
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.getStylesheets().add(getClass().getResource("editor.css").toExternalForm());

        // 设置样式 (等宽字体，字号14)
        codeArea.setStyle("-fx-font-family: 'Monospaced', 'Consolas', 'Courier New'; -fx-font-size: 14;");

        // 将编辑器加入到 FXML 的 StackPane 容器中
        editorContainer.getChildren().add(codeArea);

        // 数据绑定与监听 (CodeArea <-> ViewModel)

        // 初始加载：将 ViewModel 中的内容填入编辑器
        String content = this.viewModel.inputContentProperty().get();
        codeArea.replaceText(0, 0, content == null ? "" : content);

        // 监听 ViewModel 变化 (例如用户点击 "Open File") -> 更新编辑器
        // 加判断防止死循环
        this.viewModel.inputContentProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(codeArea.getText())) {
                codeArea.replaceText(newVal);
            }
        });

        // 实时预览逻辑 (Debounce)
        PauseTransition debounceTimer = new PauseTransition(Duration.millis(500));
        debounceTimer.setOnFinished(event -> {
            try {
                this.viewModel.convertToHtml();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 监听编辑器输入 -> 更新 ViewModel 并触发预览 + 高亮
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            // 更新 ViewModel
            this.viewModel.inputContentProperty().set(newText);

            // 重置预览倒计时
            debounceTimer.playFromStart();

            // 触发语法高亮 (异步执行)
            computeHighlightingAsync(newText);
        });

        viewModel.getDiagnostics().addListener((javafx.collections.ListChangeListener.Change<? extends Diagnostic> c) -> {
            // 更新本地缓存
            this.currentDiagnostics = new ArrayList<>(viewModel.getDiagnostics());
            // 触发重绘 (使用当前文本)
            computeHighlightingAsync(codeArea.getText());
        });

        // 其他 UI 绑定
        templateField.textProperty().bindBidirectional(this.viewModel.citationTemplateProperty());
        statusLabel.textProperty().bind(this.viewModel.statusMessageProperty());

        progressBar.visibleProperty().bind(this.viewModel.isCompilingProperty());
        progressBar.progressProperty().bind(
                javafx.beans.binding.Bindings.when(this.viewModel.isCompilingProperty())
                        .then(-1.0)
                        .otherwise(0.0)
        );

        this.viewModel.outputHtmlProperty().addListener((obs, oldVal, newVal) -> {
            previewWebView.getEngine().loadContent(newVal);
        });

        // 同步滚动逻辑 (Editor -> Preview)
        codeArea.estimatedScrollYProperty().addListener((obs, oldVal, newVal) -> {
            // 获取编辑器总高度估算值
            double totalHeight = codeArea.getTotalHeightEstimate();
            // 获取编辑器视口高度
            double viewportHeight = codeArea.getHeight();

            // 防止除以零或由极小高度导致的错误
            if (totalHeight <= viewportHeight) {
                return;
            }

            // 计算当前滚动百分比
            double scrollY = newVal.doubleValue();
            double percentage = scrollY / (totalHeight - viewportHeight);

            // 限制在 0.0 到 1.0 之间
            percentage = Math.max(0.0, Math.min(1.0, percentage));

            // 同步给 WebView
            syncPreviewScroll(percentage);
        });
    }

    private void computeHighlightingAsync(String text) {
        List<Diagnostic> diagnosticsSnapshot = new ArrayList<>(this.currentDiagnostics);

        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                // 计算语法高亮 (Base Layer)
                StyleSpans<Collection<String>> syntaxSpans = SyntaxHighlighter.computeHighlighting(text);

                // 计算错误高亮 (Error Layer)
                StyleSpans<Collection<String>> errorSpans = SyntaxHighlighter.computeErrorHighlighting(text, diagnosticsSnapshot);

                // 合并图层 (Overlay)
                // 逻辑：将两个集合合并 (A + B)
                return syntaxSpans.overlay(errorSpans, (syntaxStyle, errorStyle) -> {
                    Collection<String> combined = new ArrayList<>(syntaxStyle);
                    combined.addAll(errorStyle);
                    return combined;
                });
            }
        };

        task.setOnSucceeded(event -> {
            // 只有当编辑器文本长度没变时才应用（防止打字过快导致越界）
            if (codeArea.getLength() == text.length()) {
                codeArea.setStyleSpans(0, task.getValue());
            }
        });

        executor.execute(task);
    }

    /**
     * Handles the "Save as LaTeX" button action.
     */
    @FXML
    private void handleSaveAsLatex() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as LaTeX File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LaTeX Files", "*.tex"));
        fileChooser.setInitialFileName("output.tex");

        // 使用 editorContainer 获取 Window
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

    /**
     * Displays a standard error dialog with a detailed message.
     */
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

    /**
     * 处理 "Open" 菜单动作
     */
    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.mymd", "*.txt"));

        File initialDir = new File("sandbox");
        if (initialDir.exists()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        // 使用 editorContainer 获取 Window
        File file = fileChooser.showOpenDialog(editorContainer.getScene().getWindow());
        if (file != null) {
            try {
                viewModel.loadFile(file);
            } catch (IOException e) {
                showErrorDialog("Open Failed", "Could not load file.", e.getMessage());
            }
        }
    }

    /**
     * 处理 "Save" 菜单动作
     */
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

    /**
     * 处理 "Save As" 菜单动作
     */
    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.mymd"));

        File initialDir = new File("sandbox");
        if (initialDir.exists()) fileChooser.setInitialDirectory(initialDir);

        // 使用 editorContainer 获取 Window
        File file = fileChooser.showSaveDialog(editorContainer.getScene().getWindow());
        if (file != null) {
            try {
                viewModel.saveFile(file);
            } catch (IOException e) {
                showErrorDialog("Save Failed", "Could not save file.", e.getMessage());
            }
        }
    }

    /**
     * 同步滚动预览视图
     * @param percentage 滚动百分比 (0.0 ~ 1.0)
     */
    private void syncPreviewScroll(double percentage) {
        if (previewWebView == null || previewWebView.getEngine() == null) {
            return;
        }

        // 使用 JavaScript 控制 WebView 滚动
        // document.body.scrollHeight - window.innerHeight 计算可滚动的最大距离
        String script = String.format(
                "window.scrollTo(0, (document.body.scrollHeight - window.innerHeight) * %f);",
                percentage
        );

        try {
            previewWebView.getEngine().executeScript(script);
        } catch (Exception e) {
            // 页面可能还没加载完，忽略脚本错误
        }
    }

    /**
     * 清理资源，关闭后台线程
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 处理 "Exit" 菜单动作
     */
    @FXML
    private void handleExit() {
        shutdown();
        javafx.application.Platform.exit();
    }
}