package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class simulating an entire image drawn by the LcdController
 *
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class LcdImage {
    private final int width;
    private final int height;
    private final List<LcdImageLine> lines;

    /**
     * Constructs an LcdImage
     * @param width the width of the image
     * @param height the height of the image
     * @param lines a list containing all of the LcdImage's lines.
     */
    public LcdImage(int width, int height, List<LcdImageLine> lines) {
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);

        this.width = width;
        this.height = height;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    /**
     * Getter allowing access to the LcdImage's width
     * @return the width of the image.
     */
    public int width() {
        return width;
    }

    /**
     * Getter allowing access to the LcdImage's height
     * @return the height of the image.
     */
    public int height() {
        return height;
    }

    /**
     * Getter allowing access to the color of a pixel at given coordinates
     * @param x the x coordinate of the pixel
     * @param y the y coordinate of the pixel
     * @return an int between 0 and 3 representing the pixel's color.
     */
    public int get(int x, int y) {
        Objects.checkIndex(x, width);
        Objects.checkIndex(y, height);

        LcdImageLine line = lines.get(y);

        int msb = line.msb().testBit(x) ? 0b10 : 0;
        int lsb = line.lsb().testBit(x) ? 0b01 : 0;
        return msb | lsb;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, lines);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof LcdImage) {
            LcdImage thatLcd = (LcdImage) that;

            return (width == (thatLcd.width) &&
                    height == thatLcd.height &&
                    lines.equals(thatLcd.lines));
        }
        return false;
    }

    /**
     * A builder allowing to construct an LcdImage in multiple steps.
     */
    public final static class Builder {

        private final List<LcdImageLine> build;
        private final int buildH;
        private final int buildW;

        /**
         * Builds an empty builder using the given width and height of the image
         * @param width the width of the image
         * @param height the height of the image
         */
        public Builder(int width, int height) {
            Preconditions.checkArgument(width > 0);
            Preconditions.checkArgument(height > 0);

            build = new ArrayList<>();
            buildH = height;
            buildW = width;
            LcdImageLine line = new LcdImageLine.Builder(width).build();
            for (int i = 0; i < height; i++){
                build.add(i, line);
            }
        }

        /**
         * Sets the given line at the given index in the image
         * @param index the given index
         * @param line the given line to set
         * @return the modified version of the builder
         */
        public Builder setLine(int index, LcdImageLine line) {
            build.set(index, line);
            return this;
        }

        /**
         * Builds an LcdImage from the Builder
         * @return a final version of the Image.
         */
        public LcdImage build() {
            return new LcdImage(buildW, buildH, build);
        }
    }


}
