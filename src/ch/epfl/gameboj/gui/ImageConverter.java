package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;

import javafx.scene.image.*;

/**
 * Adapter converting the GameBoy's Images into javaFX images
 *
 * @author Matthieu De Beule (Sciper: 269623)
 */
public class ImageConverter {
    private static final int[] COLOR_MAP = new int[] {
            0xFF_FF_FF_FF, 0xFF_D3_D3_D3, 0xFF_A9_A9_A9, 0xFF_00_00_00
    };

    /**
     * Coverts the given LcdImage into a javaFX Image
     * @param lcdimage the given image to be converted
     * @return a usable javaFX image
     */
    public static Image convert(LcdImage lcdimage){
        int width = LcdController.LCD_WIDTH;
        int height = LcdController.LCD_HEIGHT;

        WritableImage writtenImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writtenImage.getPixelWriter();

        for (int y = 0; y < height; ++y)
            for (int x = 0; x < width; ++x)
                pixelWriter.setArgb(x, y, COLOR_MAP[lcdimage.get(x, y)]);

        return writtenImage;
    }

}
