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

/**
 * Controller class for the main application view.
 * This class handles user interactions and binds the view components to the ViewModel.
 */
public class MainView {

    @FXML
    private TextArea inputTextArea;
    @FXML
    private WebView previewWebView;

    private MainViewModel viewModel;

    /**
     * Sets the ViewModel and establishes data bindings between the view and the ViewModel.
     * @param viewModel The ViewModel instance to be used by this controller.
     */

    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
        inputTextArea.textProperty().bindBidirectional(this.viewModel.inputContentProperty());
        this.viewModel.outputHtmlProperty().addListener((obs, oldVal, newVal) -> {
            previewWebView.getEngine().loadContent(newVal);
        });
    }

    /**
     * Handles the "Convert to HTML Preview" button action.
     * Triggers the conversion of MyMD text to HTML and updates the preview.
     */
    @FXML
    private void handleConvert() {
        try {
            viewModel.convertToHtml();
        } catch (IOException | InterruptedException e) {
            showErrorDialog("Conversion Failed", "An error occurred while converting to HTML.", e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Handles the "Save as LaTeX" button action.
     * Opens a file chooser dialog and saves the converted LaTeX content to the selected file.
     */
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
     * Displays a standard error dialog with a detailed message.
     * @param title The title of the dialog window.
     * @param headerText The main error message.
     * @param contentText The detailed content of the error.
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