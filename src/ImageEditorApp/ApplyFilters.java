package ImageEditorApp;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ApplyFilters {

    // Filtro de blanco y negro
    public static Image applyBlackAndWhite(Image inputImage) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();

        WritableImage outputImage = new WritableImage(width, height);
        PixelReader pixelReader = inputImage.getPixelReader();
        var pixelWriter = outputImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                double gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
                pixelWriter.setColor(x, y, Color.gray(gray));
            }
        }
        return outputImage;
    }

    // Filtro de inversión de color
    public static Image applyInvertColors(Image inputImage) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();

        WritableImage outputImage = new WritableImage(width, height);
        PixelReader pixelReader = inputImage.getPixelReader();
        var pixelWriter = outputImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                Color invertedColor = Color.color(1.0 - color.getRed(), 1.0 - color.getGreen(), 1.0 - color.getBlue());
                pixelWriter.setColor(x, y, invertedColor);
            }
        }
        return outputImage;
    }

    // Filtro de aumento de brillo
    public static Image applyBrightness(Image inputImage, double factor) {
        int width = (int) inputImage.getWidth();
        int height = (int) inputImage.getHeight();

        WritableImage outputImage = new WritableImage(width, height);
        PixelReader pixelReader = inputImage.getPixelReader();
        var pixelWriter = outputImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                double red = Math.min(color.getRed() * factor, 1.0); // Limitar el valor a 1.0
                double green = Math.min(color.getGreen() * factor, 1.0);
                double blue = Math.min(color.getBlue() * factor, 1.0);
                pixelWriter.setColor(x, y, Color.color(red, green, blue));
            }
        }
        return outputImage;
    }
}

