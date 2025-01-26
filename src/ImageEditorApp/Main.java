package ImageEditorApp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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
    private static final int MAX_THREADS = 4; // Número máximo imágenes concurrentemente
    private ExecutorService executorService; // Pool de hilos

    private static final double FIXED_IMAGE_WIDTH = 200;
    private static final double FIXED_IMAGE_HEIGHT = 200;

    // Inicio aplicaión
    @Override
    public void start(Stage primaryStage) {
        executorService = Executors.newFixedThreadPool(MAX_THREADS); // Inicializa pool de hilos
        SplashScreen.show(primaryStage, () -> initializeMainApplication(primaryStage));
    }

    // Inicializa interfaz
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

    // Boton para procesar/abrir imagen
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

    // Boton para procesar/abrir carpeta completa
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
                                    int[] processedFiles = {0};
    
                                    for (File file : imageFiles) {
                                        executorService.submit(() -> {
                                            try {
                                                Thread.sleep(3000);
                                                Image image = new Image(file.toURI().toString());
                                                Platform.runLater(() -> createImageProcessingView(file, image));
                                                synchronized (processedFiles) {
                                                    processedFiles[0]++;
                                                    updateProgress(processedFiles[0], totalFiles);
                                                }
                                            } catch (Exception e) {
                                                Platform.runLater(() -> history.getItems().add("Error procesando archivo: " + file.getName()));
                                            }
                                        });
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
                    batchProcessingService.start();
                } else {
                    history.getItems().add("No se encontraron imágenes en la carpeta seleccionada.");
                }
            } else {
                history.getItems().add("No se seleccionó ninguna carpeta.");
            }
        });
    
        return batchProcessButton;
    }

    // Vista para procesar imagen cargada
    private void createImageProcessingView(File file, Image image) {
        final Image originalImage = image;
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

        Button restoreButton = new Button("Restaurar");
        restoreButton.setOnAction(event -> {
            processedImageView.setImage(originalImage); // Restaurar la imagen original
        });

        GridPane imageWithControlsGrid = new GridPane();
        imageWithControlsGrid.setHgap(20); // Espaciado horizontal entre celdas
        imageWithControlsGrid.setVgap(10); // Espaciado vertical entre celdas
        imageWithControlsGrid.setStyle("-fx-alignment: center;"); // Centrar elementos

        imageWithControlsGrid.add(imagesBox, 0, 0, 1, 4); //Disposición

        imageWithControlsGrid.add(filterProgressBar, 0, 4, 1, 1); // En la columna izq.

        imageWithControlsGrid.add(restoreButton, 1, 0); // Primera fila de la dcha.
        imageWithControlsGrid.add(blackAndWhiteButton, 1, 1);
        imageWithControlsGrid.add(invertColorsButton, 1, 2);
        imageWithControlsGrid.add(increaseBrightnessButton, 1, 3);
        imageWithControlsGrid.add(saveButton, 1, 4);

        GridPane.setMargin(restoreButton, new Insets(0, 20, 0, 0)); // Margen dcho.
        GridPane.setMargin(blackAndWhiteButton, new Insets(0, 20, 0, 0));
        GridPane.setMargin(invertColorsButton, new Insets(0, 20, 0, 0));
        GridPane.setMargin(increaseBrightnessButton, new Insets(0, 20, 0, 0));
        GridPane.setMargin(saveButton, new Insets(0, 20, 0, 0));


        // Ajustar las celdas
        GridPane.setHgrow(imagesBox, Priority.ALWAYS);
        GridPane.setVgrow(imagesBox, Priority.ALWAYS);
        GridPane.setHgrow(filterProgressBar, Priority.ALWAYS);

        Tab tab = new Tab(file.getName());
        tab.setClosable(true);
        tab.setContent(imageWithControlsGrid);
        tabPane.getTabs().add(tab);

    }

    // Boton para aplicar filtro a imagen procesada
    private Button createFilterButton(String filterName, Image originalImage, ImageView processedImageView, String fileName, FilterTask task, ProgressBar filterProgressBar) {
        Button filterButton = new Button(filterName);

        filterButton.setOnAction(event -> {
            filterButton.setDisable(true);
            filterProgressBar.setVisible(true);

            Task<Image> filterTask = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    updateProgress(0, 100);
                    Thread.sleep(300);
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

    // Boton guardado imagen procesada
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

    // Cuadro mensaje popup
    private void showPopup(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Detiene servicios
    @Override
    public void stop() {
        executorService.shutdown();
    }

    // Metodo inicio aplicacion javafx
    public static void main(String[] args) {
        launch(args);
    }

    // Interfaz aplica filtro
    @FunctionalInterface
    private interface FilterTask {
        Image apply() throws Exception;
    }
}
