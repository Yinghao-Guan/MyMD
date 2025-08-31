package com.guaguaaaa.mymd.view;

import com.guaguaaaa.mymd.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
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
    private void handleConvert() throws IOException, InterruptedException {
        viewModel.convertToHtml();
    }

    // 新增的方法来处理保存为 LaTeX
    @FXML
    private void handleSaveAsLatex() throws IOException, InterruptedException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as LaTeX File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LaTeX Files", "*.tex"));
        fileChooser.setInitialFileName("output.tex");

        File file = fileChooser.showSaveDialog(null); // null表示在当前窗口打开

        if (file != null) {
            viewModel.saveAsLatex(file);
        }
    }
}