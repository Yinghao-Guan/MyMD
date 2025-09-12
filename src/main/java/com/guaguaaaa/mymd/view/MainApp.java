package com.guaguaaaa.mymd.view;

import com.guaguaaaa.mymd.viewmodel.MainViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the application
 * This class sets up the JavaFX stage, loads the FXML view, and links it to the ViewModel.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create the ViewModel instance
        MainViewModel viewModel = new MainViewModel();

        // Create the FXMLLoader
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainView.fxml"));

        // Load the FXML. The FXMLLoader will automatically create the MainView instance and inject its @FXML fields.
        Parent root = fxmlLoader.load();

        // Get the controller instance created by the FXMLLoader
        MainView controller = fxmlLoader.getController();

        // Inject the ViewModel into the controller after it has been loaded
        controller.setViewModel(viewModel);

        // Set up the scene and stage
        primaryStage.setTitle("MyMD Editor");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }

    /**
     * The main method to launch the JavaFX application.
     *
     * @param args Command line arguments.
     */

    public static void main(String[] args) {
        launch(args);
    }
}