package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.*;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.Ram;

import java.util.Arrays;
import java.util.Objects;

/**
 * Controller for the GameBoy's Liquid Cristal Display Screen
 *
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public class LcdController implements Component, Clocked {
    public static final int LCD_WIDTH = 160;
    public static final int LCD_HEIGHT = 144;
    private static final int BG_SIZE = 256;

    private static final int MODE_2_CYCLES = 20;
    private static final int MODE_3_CYCLES = 43;
    private static final int H_BLANK_CYCLES = 51;
    private static final int DRAW_CYCLES = 114;
    private static final int IMAGE_CYCLES = 17556;

    private static final int TILE_SIZE = 8;
    private static final int NUMBER_OF_TILES = 32;
    private static final int TILE_SOURCE_DIFF = 0x80;
    private static final int TILE_INDEX_BOUND = 0x100;
    private static final int TILE_LENGTH = 16;

    private static final int LY_UPPER_BOUND = 153;
    private static final int WX_CORRECT = 7;
    private static final int X_COMPENSATION = 8;
    private static final int Y_COMPENSATION = 16;

    private static final int SPRITE_BYTES = 4;
    private static final int SPRITE_CHAR_BYTE = 3;
    private static final int MAX_SPRITES = 10;
    private static final int NUMBER_OF_SPRITES = 40;
    private static final int BYTES_TO_COPY = 160;

    private final RegisterFile<Reg> regs;
    private final Ram videoRam;
    private final Ram oam;

    private final Cpu cpu;
    private Bus bus;
    private long nextNonIdleCycle;
    private long cycle;
    private int winY = 0;
    private int copyStatus;
    private int lcdOnCycle = 0;

    private LcdImage currentImage;
    private LcdImage.Builder nextImageBuilder;

    private Mode nextMode = Mode.MODE_2;

    /**
     * An enumeration representing all of the LcdController's registers.
     */
    public enum Reg implements Register {
        LCDC,   //Controller Configuration
        STAT,   //Controller state
        SCY,    //Y coordinate of the displayed BG
        SCX,    //X coordinate of the displayed BG
        LY,     //Number of the line that is being drawn
        LYC,    //Number of the line to be compared
        DMA,    //Address of the memory copy source
        BGP,    //Palette of the BG and Window
        OBP0,   //Palette 0 of the sprites
        OBP1,   //Palette 1 of the sprites
        WY,     //Y coordinate of the window's upper-left corner
        WX;     //X coordinate of the window's upper-left corner

        /**
         * Returns the register associated to a given address
         *
         * @param address the given address where the register will be found
         * @return the 8bit register associated to the given address
         */
        private static Reg getReg(int address) {
            Preconditions.checkBits16(address);
            return Reg.values()[address - AddressMap.REGS_LCDC_START];
        }

    }

    /**
     * Represents the bits of the LCDC register
     */
    private enum LCDCBits implements Bit {
        BG,             //Enable BG
        OBJ,            //Enable Sprites
        OBJ_SIZE,       //Height of Sprites (8 or 16 pixels)
        BG_AREA,        //Origin of the BG tiles
        TILE_SOURCE,    //Origin of the image tiles
        WIN,            //Enable Window
        WIN_AREA,       //Origin of the window tiles
        LCD_STATUS      //Enable LCD Screen
    }

    /**
     * Represents the bits of the STAT register
     */
    private enum STATBits implements Bit {
        MODE0,          //Mode (bit 0)
        MODE1,          //Mode (bit 1)
        LYC_EQ_LY,      //True iff LYC = LY
        INT_MODE0,      //Enable Mode 0 Interruption
        INT_MODE1,      //Enable Mode 1 Interruption
        INT_MODE2,      //Enable Mode 2 Interruption
        INT_LYC         //Enable LYC = LY Interruption
    }

    /**
     * Represents the bits of the Sprite characteristics byte
     */
    private enum SpriteCharBits implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3,
        PALETTE,    //Palette used for the sprite(OBP0 or OBP1)
        FLIP_H,     //Horizontally flips the sprite
        FLIP_V,     //Vertically flips the sprite
        BEHIND_BG   //Plane on which the sprite is located
    }

    /**
     * Represents the different modes of the LCD.
     */
    private enum Mode {
        MODE_0, MODE_1, MODE_2, MODE_3
    }

    /**
     * Constructs an LcdController by associating it to the given cpu.
     *
     * @param cpu the given cpu.
     */
    public LcdController(Cpu cpu) {
        this.cpu = cpu;
        regs = new RegisterFile<>(Reg.values());
        videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        currentImage = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT).build();
        nextNonIdleCycle = Long.MAX_VALUE;
        oam = new Ram(AddressMap.OAM_RAM_SIZE);
        copyStatus = BYTES_TO_COPY;

    }

    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        Component.super.attachTo(bus);
    }

    @Override
    public void cycle(long cycle) {
        this.cycle = cycle;
        boolean lcdOn = regs.testBit(Reg.LCDC, LCDCBits.LCD_STATUS);

        ++lcdOnCycle;

        assert (nextNonIdleCycle
                >= cycle) : "Cycle bigger than nextNonIdleCycle";

        //Quick Copy
        if (copyStatus < BYTES_TO_COPY) {
            oam.write(copyStatus,
                    bus.read(Bits.make16(regs.get(Reg.DMA), copyStatus)));
            copyStatus++;
        }

        //Regular Cycle
        if (nextNonIdleCycle == cycle && lcdOn) {
            reallyCycle();
        }

        //LCD is turning on
        else if (nextNonIdleCycle == Long.MAX_VALUE && lcdOn) {
            nextMode = Mode.MODE_2;
            setMode(Mode.MODE_2);
            nextNonIdleCycle = cycle;
            lcdOnCycle = 0;
            reallyCycle();
        }
    }

    /**
     * Controls the actions done during the lcdc's different modes
     */
    private void reallyCycle() {
        setMode(nextMode);
        int line = regs.get(Reg.LY);
        int currentFrame = lcdOnCycle % IMAGE_CYCLES;
        if (currentFrame == 0)
            line = 0;

        if (line < LCD_HEIGHT) {
            switch (currentMode()) {
            case MODE_0:
                    nextNonIdleCycle += H_BLANK_CYCLES;

                    if (regs.testBit(Reg.STAT, STATBits.INT_MODE0))
                        cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);

                    setLY(true, line + 1);
                    nextMode = Mode.MODE_2;

                break;

            case MODE_2:
                    nextNonIdleCycle += MODE_2_CYCLES;

                    if (regs.testBit(Reg.STAT, STATBits.INT_MODE2))
                        cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);

                    if (regs.get(Reg.LY) == 0) {
                        nextImageBuilder = new LcdImage.Builder(LCD_WIDTH,
                                LCD_HEIGHT);
                        winY = 0;
                    }
                    nextMode = Mode.MODE_3;

                break;

            case MODE_3:
                    nextNonIdleCycle += MODE_3_CYCLES;
                    nextImageBuilder.setLine(line, computeLine(line));

                    nextMode = Mode.MODE_0;

                break;
            }
        } else {
            nextNonIdleCycle += DRAW_CYCLES;

            //Mode 1
            if (line == LCD_HEIGHT) {

                if (regs.testBit(Reg.STAT, STATBits.INT_MODE1))
                    cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);

                nextMode = Mode.MODE_1;
                setMode(Mode.MODE_1);

                currentImage = nextImageBuilder.build();
                nextImageBuilder = null;

                cpu.requestInterrupt(Cpu.Interrupt.VBLANK);
            }

            if (line < LY_UPPER_BOUND) {
                setLY(true, line + 1);
            } else {
                setLY(true, 0);
                nextMode = Mode.MODE_2;
            }
        }
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);

        //Object Attribute Memory
        if (AddressMap.OAM_START <= address && address < AddressMap.OAM_END) {
            return oam.read(address - AddressMap.OAM_START);
        }

        //Video Ram
        if (AddressMap.VIDEO_RAM_START <= address
                && address < AddressMap.VIDEO_RAM_END) {
            return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        }
        //LCD registers
        else if (AddressMap.REGS_LCDC_START <= address
                && address < AddressMap.REGS_LCDC_END) {
            return regs.get(Reg.getReg(address));
        }
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        //Object Attribute Memory
        if (AddressMap.OAM_START <= address && address < AddressMap.OAM_END) {
            oam.write(address - AddressMap.OAM_START, data);
        }

        //Video Ram
        if (AddressMap.VIDEO_RAM_START <= address
                && address < AddressMap.VIDEO_RAM_END) {
            videoRam.write(address - AddressMap.VIDEO_RAM_START, data);
        }
        //LCD registers
        if (AddressMap.REGS_LCDC_START <= address
                && address < AddressMap.REGS_LCDC_END) {
            Reg r = Reg.getReg(address);

            switch (r) {
            case DMA :
                regs.set(r, data);
                copyStatus = 0;
                break;
            case LY:
                break;
            case STAT:
                int lsbs = Bits.clip(3, regs.get(r));
                int val = (Bits.extract(data, 3, 5) << 3) | lsbs;
                regs.set(r, val);
                break;
            case LYC:
                setLY(false, data);
                break;
            //Screen turns off
            case LCDC:
                regs.set(Reg.LCDC, data);
                if (!regs.testBit(Reg.LCDC, LCDCBits.LCD_STATUS)) {
                    setMode(Mode.MODE_0);
                    setLY(true, 0);
                    nextNonIdleCycle = Long.MAX_VALUE;
                }
                break;
            default:
                regs.set(r, data);
            }
        }
    }

    /**
     * Modifies the different bits in the LYC register.
     */
    private void setLY(boolean ly, int data) {
        Reg r = ly ? Reg.LY : Reg.LYC;
        regs.set(r, data);
        regs.setBit(Reg.STAT, STATBits.LYC_EQ_LY,
                regs.get(Reg.LY) == regs.get(Reg.LYC));

        if (regs.testBit(Reg.STAT, STATBits.INT_LYC)
                && (regs.testBit(Reg.STAT, STATBits.LYC_EQ_LY)))
                cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);
    }

    /**
     * Sets the current mode to the given mode
     * @param mode to be set
     */
    private void setMode(Mode mode) {
        regs.setBit(Reg.STAT, STATBits.MODE0, Bits.test(mode.ordinal(), 0));
        regs.setBit(Reg.STAT, STATBits.MODE1, Bits.test(mode.ordinal(), 1));
    }

    /**
     * @return the current mode
     */
    private Mode currentMode() {
        return Mode.values()[Bits.clip(2, regs.get(Reg.STAT))];
    }

    /**
     * Computes the current image's line at the given lineIndex
     *
     * @param lineNumber the given LineIndex
     * @return the computed line
     */
    private LcdImageLine computeLine(int lineNumber) {
        Objects.checkIndex(lineNumber, LCD_HEIGHT);
        //--------------------------- Draw BG ----------------------------------
        LcdImageLine line = new LcdImageLine.Builder(BG_SIZE).build();
        int lineIndex = (lineNumber + regs.get(Reg.SCY)) % BG_SIZE;

        if (regs.testBit(Reg.LCDC, LCDCBits.BG)) {
            line = constructLine(false, lineIndex);
            line = line.extractWrapped(regs.get(Reg.SCX), LCD_WIDTH)
                    .mapColors(regs.get(Reg.BGP));
        }
        //-------------------------- Draw WIN ----------------------------------
        int wX = Math.max(regs.get(Reg.WX) - (WX_CORRECT), 0);
        int wY = regs.get(Reg.WY);
        LcdImageLine auxLine = new LcdImageLine.Builder(LCD_WIDTH).build();


        if (regs.testBit(Reg.LCDC, LCDCBits.WIN)) {

            if (wX < LCD_WIDTH && lineNumber >= wY) {
                auxLine = constructLine(true, winY).shift(wX)
                        .mapColors(regs.get(Reg.BGP));

                ++winY;
            }
        }
        //----------------------------------------------------------------------
        //---------------------------- Sprites ---------------------------------
        LcdImageLine foregroundLine = new LcdImageLine.Builder(LCD_WIDTH)
                .build();
        LcdImageLine backgroundLine = new LcdImageLine.Builder(LCD_WIDTH)
                .build();

        if (regs.testBit(Reg.LCDC, LCDCBits.OBJ)) {

            int[] intersectingSprites = spritesIntersectingLine(lineNumber);


            for (int i = 0; i < intersectingSprites.length; i++) {

                if (Bits.test(oam.read(intersectingSprites[i] * SPRITE_BYTES
                        + SPRITE_CHAR_BYTE), SpriteCharBits.BEHIND_BG)) {
                    backgroundLine = singleSpriteLine(intersectingSprites[i],
                            lineNumber).below(backgroundLine);
                } else {
                    foregroundLine = singleSpriteLine(intersectingSprites[i],
                            lineNumber).below(foregroundLine);
                }
            }
        }
        //----------------------------------------------------------------------

        //Join BG Sprites
        if (regs.testBit(Reg.LCDC, LCDCBits.OBJ))
            line = backgroundLine.below(line,
                    line.opacity().or(backgroundLine.opacity().not()));

        //Join window
        if(regs.testBit(Reg.LCDC, LCDCBits.WIN)
                && wX < LCD_WIDTH && lineNumber >= wY)
            line = line.join(auxLine, wX);

        //Join FG Sprites
        if (regs.testBit(Reg.LCDC, LCDCBits.OBJ))
            line = line.below(foregroundLine);

        return line;
    }

    /**
     * Compute a line with a given sprite in it
     *
     * @param spriteIndex index of the sprite
     * @param lineIndex   index of the line
     * @return the line containing the sprite
     */
    private LcdImageLine singleSpriteLine(int spriteIndex, int lineIndex) {
        int x = oam.read(spriteIndex * SPRITE_BYTES + 1) - X_COMPENSATION;
        int y = oam.read(spriteIndex * SPRITE_BYTES) - Y_COMPENSATION;
        int palette = Bits.test(oam.read(spriteIndex * SPRITE_BYTES + SPRITE_CHAR_BYTE),
                SpriteCharBits.PALETTE) ? regs.get(Reg.OBP1) : regs.get(Reg.OBP0);

        int spriteHeight = regs.testBit(Reg.LCDC, LCDCBits.OBJ_SIZE) ?
                TILE_SIZE * 2 : TILE_SIZE;

        int spriteCharacteristics = oam.read(
                SPRITE_CHAR_BYTE + spriteIndex * SPRITE_BYTES);
        boolean verticalFlip = Bits
                .test(spriteCharacteristics, SpriteCharBits.FLIP_V);
        boolean horizontalFlip = Bits
                .test(spriteCharacteristics, SpriteCharBits.FLIP_H);

        int relevantLineInSprite = verticalFlip ?
                spriteHeight - 1 - (lineIndex - y) :
                lineIndex - y;

        int regularLsb = read(AddressMap.VIDEO_RAM_START + (Byte.SIZE * 2) *
                oam.read(2 + spriteIndex * SPRITE_BYTES)
                + relevantLineInSprite * 2);
        int regularMsb = read(AddressMap.VIDEO_RAM_START + (Byte.SIZE * 2) *
                oam.read(2 + spriteIndex * SPRITE_BYTES)
                + relevantLineInSprite * 2 + 1);

        int spriteLsb = horizontalFlip ? regularLsb : Bits.reverse8(regularLsb);
        int spriteMsb = horizontalFlip ? regularMsb : Bits.reverse8(regularMsb);

        LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(
                LCD_WIDTH);
        spriteLineBuilder.setBytes(0, spriteMsb, spriteLsb);
        return spriteLineBuilder.build().shift(x).mapColors(palette);
    }

    /**
     * Constructs the line (either background or window) at the given lineIndex
     *
     * @param window    selects whether to compute a window line or a bg line
     * @param lineIndex the index at which to compute the line
     * @return the constructed version of the line.
     */
    private LcdImageLine constructLine(boolean window, int lineIndex) {

        int bgArea = AddressMap.BG_DISPLAY_DATA[
                regs.testBit(Reg.LCDC, LCDCBits.BG_AREA) ? 1 : 0];
        int winArea = AddressMap.BG_DISPLAY_DATA[
                regs.testBit(Reg.LCDC, LCDCBits.WIN_AREA) ? 1 : 0];
        int tileArea = AddressMap.TILE_SOURCE[
                regs.testBit(Reg.LCDC, LCDCBits.TILE_SOURCE) ? 1 : 0];

        int length = window ? LCD_WIDTH : BG_SIZE;
        int area = window ? winArea : bgArea;

        int tileLineIndex = lineIndex / TILE_SIZE;
        int tileLine = lineIndex % TILE_SIZE;

        LcdImageLine.Builder construct = new LcdImageLine.Builder(length);

        for (int i = 0; i < (length / Byte.SIZE); ++i) {
            int tileIndex = read(tileLineIndex * NUMBER_OF_TILES + area + i);
            tileIndex = (regs.testBit(Reg.LCDC, LCDCBits.TILE_SOURCE)) ? tileIndex :
                    (tileIndex + TILE_SOURCE_DIFF) % TILE_INDEX_BOUND;

            int byteAddress = tileArea + tileIndex * TILE_LENGTH + tileLine * 2;

            int tileLsb = read(byteAddress);
            int tileMsb = read(byteAddress + 1);

            construct = construct.setBytes(i, Bits.reverse8(tileMsb),
                    Bits.reverse8(tileLsb));
        }

        return construct.build();
    }

    /**
     * Calculates which sprites are found on each line
     *
     * @param line the line on which we want to find the sprites
     * @return an array containing the indexes of each sprite in the correct
     * order, so ordered by x and index in memory.
     */
    private int[] spritesIntersectingLine(int line) {
        int[] sprites = new int[MAX_SPRITES];
        int count = 0;

        /* Each element contains the following information:
         *   - in the MSBs --> x coordinate of the sprite
         *   - in the LSBs --> sprite's index
         */
        int size = regs.testBit(Reg.LCDC, LCDCBits.OBJ_SIZE) ?
                TILE_SIZE * 2 : TILE_SIZE;

        for (int i = 0; i < NUMBER_OF_SPRITES && count < MAX_SPRITES; ++i) {
            int y = oam.read(i * SPRITE_BYTES) - Y_COMPENSATION;
            int x = oam.read(i * SPRITE_BYTES + 1);

            if (line >= y && line < y + size) {
                sprites[count] = Bits.make16(x, i);
                ++count;
            }
        }
        Arrays.sort(sprites, 0, count);
        int[] orderedIndexes = new int[count];

        for (int i = 0; i < count; ++i)
            orderedIndexes[i] = Bits.clip(Byte.SIZE, sprites[i]);

        return orderedIndexes;
    }

    /**
     * Returns the current Image that is displayed on the screen
     *
     * @return a blank image if currentImage is null
     * else returns the current Image.
     */
    public LcdImage currentImage() {
        return currentImage;
    }
}
