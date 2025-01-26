package ImageEditorApp;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;

public class FileManager {

    // Directorio por defecto
    private static final File defaultSaveDirectory = new File(System.getProperty("user.home"), "ProcessedImages");

    // Guarda imagen procesada
    public static void saveImage(File originalFile, Image image) throws Exception {
        
        if (!defaultSaveDirectory.exists()) {
            defaultSaveDirectory.mkdir();
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Imagen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG", "*.png"));
        fileChooser.setInitialFileName(originalFile.getName().replaceFirst("\\.\\w+$", "") + "_processed.png");
        fileChooser.setInitialDirectory(defaultSaveDirectory);

        File saveFile = fileChooser.showSaveDialog(null);
        if (saveFile == null) {
            saveFile = new File(defaultSaveDirectory, originalFile.getName().replaceFirst("\\.\\w+$", "") + "_processed.png");
        }

        saveFile = getUniqueFile(saveFile);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new FileOutputStream(saveFile));
    }

    // Verifica existencia del nombre
    public static File getUniqueFile(File file) {
        String name = file.getName();
        String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
        File uniqueFile = file;
        int counter = 1;
        while (uniqueFile.exists()) {
            uniqueFile = new File(file.getParent(), baseName + "_" + counter++ + extension);
        }
        return uniqueFile;
    }
}
