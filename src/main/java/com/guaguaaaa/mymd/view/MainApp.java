package com.guaguaaaa.mymd.view;

import com.guaguaaaa.mymd.viewmodel.MainViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 创建 ViewModel 实例
        MainViewModel viewModel = new MainViewModel();

        // 创建 FXMLLoader
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainView.fxml"));

        // 加载 FXML。此时，FXMLLoader 会自动创建 MainView 实例并注入 @FXML 字段。
        Parent root = fxmlLoader.load();

        // 获取 FXMLLoader 创建的控制器实例
        MainView controller = fxmlLoader.getController();

        // 在控制器被加载并初始化后，再注入 ViewModel
        controller.setViewModel(viewModel);

        // 设置场景和舞台
        primaryStage.setTitle("MyMD Editor");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}