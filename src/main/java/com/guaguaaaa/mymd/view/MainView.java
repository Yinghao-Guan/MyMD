package com.guaguaaaa.mymd.view;

import com.guaguaaaa.mymd.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;

public class MainView {

    @FXML
    private TextArea inputTextArea;
    @FXML
    private WebView previewWebView;

    private MainViewModel viewModel;

    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
        inputTextArea.textProperty().bindBidirectional(this.viewModel.inputContentProperty());
        this.viewModel.outputHtmlProperty().addListener((obs, oldVal, newVal) -> {
            previewWebView.getEngine().loadContent(newVal);
        });
    }

    @FXML
    private void handleConvert() {
        try {
            viewModel.convertToHtml();
        } catch (IOException | InterruptedException e) {
            showErrorDialog("Conversion Failed", "An error occurred while converting to HTML.", e.toString());
            // 可以在控制台打印完整的堆栈信息用于调试
            e.printStackTrace();
        }
    }

    // 新增的方法来处理保存为 LaTeX
    @FXML
    private void handleSaveAsLatex() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as LaTeX File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LaTeX Files", "*.tex"));
        fileChooser.setInitialFileName("output.tex");

        File file = fileChooser.showSaveDialog(null);

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
     * 显示一个标准的错误信息弹窗。
     * @param title 对话框的标题
     * @param headerText 错误的主要信息
     * @param contentText 错误的详细内容
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
}