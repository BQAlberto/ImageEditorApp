package ImageEditorApp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    private VBox root;
    private TabPane tabPane;
    private ListView<String> history;
    private ProgressBar progressBar;
    private File defaultSaveDirectory = new File(System.getProperty("user.home"), "ProcessedImages");
    private static final int MAX_THREADS = 4; // Número máximo de hilos concurrentes
    private ExecutorService executorService; // Pool de hilos

    private static final double FIXED_IMAGE_WIDTH = 200;
    private static final double FIXED_IMAGE_HEIGHT = 200;

    @Override
    public void start(Stage primaryStage) {
        executorService = Executors.newFixedThreadPool(MAX_THREADS); // Inicializar el pool de hilos
        SplashScreen.show(primaryStage, () -> initializeMainApplication(primaryStage));
    }

    private void initializeMainApplication(Stage primaryStage) {
        root = new VBox();
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);

        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(20);
        HBox progressBox = new HBox();
        progressBox.getChildren().add(progressBar);
        progressBox.setStyle("-fx-alignment: center;");

        history = new ListView<>();
        history.setPrefHeight(200);
        history.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(tabPane, Priority.ALWAYS);
        VBox.setVgrow(history, Priority.ALWAYS);

        Button openImagesButton = createOpenImagesButton(primaryStage);
        Button batchProcessButton = createBatchProcessButton(primaryStage);

        root.getChildren().addAll(openImagesButton, batchProcessButton, tabPane, history, progressBox);

        if (!defaultSaveDirectory.exists()) {
            defaultSaveDirectory.mkdir();
        }

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Editor de Imágenes");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Button createOpenImagesButton(Stage primaryStage) {
        Button openImagesButton = new Button("Abrir Imágenes");

        openImagesButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));
            java.util.List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);

            if (files != null && !files.isEmpty()) {
                for (File file : files) {
                    Image image = new Image(file.toURI().toString());
                    createImageProcessingView(file, image);
                }
            }
        });

        return openImagesButton;
    }

    private Button createBatchProcessButton(Stage primaryStage) {
        Button batchProcessButton = new Button("Procesar Carpeta");

        batchProcessButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Seleccionar Carpeta");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);

            if (selectedDirectory != null && selectedDirectory.isDirectory()) {
                File[] imageFiles = selectedDirectory.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));

                if (imageFiles != null && imageFiles.length > 0) {
                    progressBar.setVisible(true);
                    progressBar.setProgress(0);

                    Service<Void> batchProcessingService = new Service<>() {
                        @Override
                        protected Task<Void> createTask() {
                            return new Task<>() {
                                @Override
                                protected Void call() throws Exception {
                                    int totalFiles = imageFiles.length;
                                    int processedFiles = 0;

                                    for (File file : imageFiles) {
                                        Image image = new Image(file.toURI().toString());
                                        Platform.runLater(() -> createImageProcessingView(file, image));

                                        processedFiles++;
                                        updateProgress((double) processedFiles / totalFiles, 1);
                                        Thread.sleep(1000);
                                    }
                                    return null;
                                }
                            };
                        }
                    };

                    batchProcessingService.setOnSucceeded(e -> {
                        progressBar.setVisible(false);
                        history.getItems().add("Procesamiento completado.");
                    });

                    batchProcessingService.setOnFailed(e -> {
                        progressBar.setVisible(false);
                        history.getItems().add("Error durante el procesamiento.");
                    });

                    progressBar.progressProperty().bind(batchProcessingService.progressProperty());
                    executorService.submit(batchProcessingService::start);
                } else {
                    history.getItems().add("No se encontraron imágenes en la carpeta seleccionada.");
                }
            } else {
                history.getItems().add("No se seleccionó ninguna carpeta.");
            }
        });

        return batchProcessButton;
    }

    private void createImageProcessingView(File file, Image image) {
        HBox imagesBox = new HBox(10);
        imagesBox.setStyle("-fx-alignment: center;");

        ImageView originalImageView = new ImageView(image);
        originalImageView.setPreserveRatio(true);
        originalImageView.setFitWidth(FIXED_IMAGE_WIDTH);
        originalImageView.setFitHeight(FIXED_IMAGE_HEIGHT);

        ImageView processedImageView = new ImageView();
        processedImageView.setPreserveRatio(true);
        processedImageView.setFitWidth(FIXED_IMAGE_WIDTH);
        processedImageView.setFitHeight(FIXED_IMAGE_HEIGHT);
        processedImageView.setImage(image);

        imagesBox.getChildren().addAll(originalImageView, processedImageView);

        ProgressBar filterProgressBar = new ProgressBar(0);
        filterProgressBar.setVisible(false);
        filterProgressBar.setMaxWidth(100);
        filterProgressBar.setPrefHeight(15);

        Button blackAndWhiteButton = createFilterButton("Blanco y Negro", image, processedImageView, file.getName(), () -> ApplyFilters.applyBlackAndWhite(image), filterProgressBar);
        Button invertColorsButton = createFilterButton("Invertir Colores", image, processedImageView, file.getName(), () -> ApplyFilters.applyInvertColors(image), filterProgressBar);
        Button increaseBrightnessButton = createFilterButton("Aumentar Brillo", image, processedImageView, file.getName(), () -> ApplyFilters.applyBrightness(image, 1.2), filterProgressBar);

        Button saveButton = createSaveButton(file, processedImageView);

        VBox imageWithControlsBox = new VBox(10);
        imageWithControlsBox.getChildren().addAll(imagesBox, filterProgressBar, blackAndWhiteButton, invertColorsButton, increaseBrightnessButton, saveButton);

        Tab tab = new Tab(file.getName());
        tab.setClosable(true);
        tab.setContent(imageWithControlsBox);
        tabPane.getTabs().add(tab);
    }

    private Button createFilterButton(String filterName, Image originalImage, ImageView processedImageView, String fileName, FilterTask task, ProgressBar filterProgressBar) {
        Button filterButton = new Button(filterName);

        filterButton.setOnAction(event -> {
            filterButton.setDisable(true);
            filterProgressBar.setVisible(true);

            Task<Image> filterTask = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    updateProgress(0, 100);
                    Thread.sleep(500);
                    for (int i = 0; i <= 100; i++) {
                        updateProgress(i, 100);
                        Thread.sleep(20);
                    }
                    return task.apply();
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    Image filteredImage = getValue();
                    Platform.runLater(() -> {
                        processedImageView.setImage(filteredImage);
                        history.getItems().add("[" + fileName + "] Filtro aplicado: " + filterName);
                        showPopup("Éxito", "El filtro se aplicó correctamente.");
                        filterButton.setDisable(false);
                        filterProgressBar.setVisible(false);
                    });
                }

                @Override
                protected void failed() {
                    super.failed();
                    Platform.runLater(() -> {
                        history.getItems().add("Error al aplicar filtro: " + filterName + " en [" + fileName + "]");
                        showPopup("Error", "Ocurrió un error al aplicar el filtro.");
                        filterButton.setDisable(false);
                        filterProgressBar.setVisible(false);
                    });
                }
            };

            filterProgressBar.progressProperty().bind(filterTask.progressProperty());
            new Thread(filterTask).start();
        });

        return filterButton;
    }

    private Button createSaveButton(File originalFile, ImageView processedImageView) {
        Button saveButton = new Button("Guardar");
    
        saveButton.setOnAction(event -> {
            Image imageToSave = processedImageView.getImage();
            if (imageToSave != null) {
                try {
                    if (imageToSave.getWidth() > 0 && imageToSave.getHeight() > 0) {
                        FileManager.saveImage(originalFile, imageToSave);
                        history.getItems().add("Imagen guardada correctamente.");
                    } else {
                        history.getItems().add("Error: La imagen procesada tiene un tamaño inválido.");
                    }
                } catch (Exception e) {
                    history.getItems().add("Error al guardar la imagen: " + e.getMessage());
                }
            } else {
                history.getItems().add("Error: No hay imagen procesada para guardar.");
            }
        });
    
        return saveButton;
    }

    private void showPopup(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @FunctionalInterface
    private interface FilterTask {
        Image apply() throws Exception;
    }
}
