package ImageEditorApp;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SplashScreen {
    
    // Muestra pantalla bienvenida
    public static void show(Stage primaryStage, Runnable onFinished) {
        Stage splashStage = new Stage();
        StackPane splashRoot = new StackPane();
        splashRoot.getChildren().add(new Label("Bienvenido al Editor de Imágenes"));
        Scene splashScene = new Scene(splashRoot, 400, 200);
        splashStage.setScene(splashScene);
        splashStage.show();

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(3000);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    splashStage.close();
                    onFinished.run();
                });
            }
        };

        new Thread(loadTask).start();
    }
}