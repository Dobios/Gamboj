package ch.epfl.gameboj.component.cartridge;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * Class representing the controller of a memory bank of type 0.
 * @author Matthieu De Beule (Sciper: 269623)
 */

public final class MBC0 implements Component{

    private Rom rom;
    public static final int ROM_SIZE = 0x8000;

    /**
     * Construct a memory controller of type 0, i.e. having only a 32 768 byte
     * ROM attached
     * @param rom to be controlled with this memory controller
     */
    public MBC0(Rom rom) {
        if (rom == null) {
            throw new NullPointerException();
        }
        Preconditions.checkArgument(rom.size() == ROM_SIZE);
        this.rom = rom;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (address < ROM_SIZE) {
            return rom.read(address);
        }
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        //ROM can't be written to, do nothing.
    }
}
