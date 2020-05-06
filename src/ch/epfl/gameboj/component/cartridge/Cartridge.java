package ch.epfl.gameboj.component.cartridge;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Class simulating a Cartridge containing a rom file.
 * @author Matthieu De Beule (Sciper: 269623)
 */
public final class Cartridge implements Component {
    private static final int CARTRIDGE_TYPES = 4;
    private static final int CARTRIDGE_TYPE_ADDRESS = 0x147;
    private static final int ROM_SIZE = 0x149;
    private static final int CARTRIDGE_NAME_SIZE = 15;
    private static final int CARTRIDGE_NAME_START = 308;

    private Component romController;
    private static MBC1 rom;

    /**
     * Constructs a cartridge from a memory controller and the associated ROM
     * @param component from which we construct the cartridge
     */
    private Cartridge(Component component){
        romController = component;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);

        return romController.read(address);
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        romController.write(address, data);
    }

    /**
     * Make a cartridge from a given file
     * @param romFile containing the data to be put into the cartridge's ROM
     * @return cartridge whose ROM contains the data in romFile
     * @throws IOException if there is an I/O error (including the file not
     * existing)
     */
    public static Cartridge ofFile(File romFile) throws IOException {

        byte[] data = Files.readAllBytes(romFile.toPath());
        Preconditions.checkArgument(0 <= data[CARTRIDGE_TYPE_ADDRESS]
                && data[CARTRIDGE_TYPE_ADDRESS] < CARTRIDGE_TYPES);
        int[] romSize = {0, 2048, 8192, 32768};

        if(data[CARTRIDGE_TYPE_ADDRESS] == 0)
            return new Cartridge(new MBC0(new Rom(data)));
        else {
            rom = new MBC1(new Rom(data), romSize[data[ROM_SIZE]]);
            return new Cartridge(rom);
        }
    }

    private String cartridgeName() {
        StringBuilder fileName = new StringBuilder(CARTRIDGE_NAME_SIZE);
        for (int i = 0; i < CARTRIDGE_NAME_SIZE; ++i)
            fileName.append((char)rom.read(CARTRIDGE_NAME_START));
        return fileName.toString();
    }

    public void saveState() throws IOException {
        rom.save(cartridgeName());
    }

    public void loadState() throws IOException {
        rom.load(cartridgeName());
    }
}

