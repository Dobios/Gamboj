package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.Byte.toUnsignedInt;

/**
 * Class simulating Read Only Memory
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class Rom {
    private final byte[] rom;

    /**
     * Constructs the read only memory part of the emulator
     * @param data, this will have the same size and content as data
     */
    public Rom(byte[] data) {
        Objects.requireNonNull(data);
        rom = Arrays.copyOf(data, data.length);
    }

    /**
     * @return byte size of the ROM
     */
    public int size() {
        return rom.length;
    }

    /**
     * Returns ROM byte at the given index
     * @param index of the wanted byte
     * @return the byte at the given index as a 16bit int
     * (between 0 and FF)
     */
    public int read(int index) {
        if (index < 0 || index >= rom.length) {
            throw new IndexOutOfBoundsException();
        } else {
            return toUnsignedInt(rom[index]);
        }
    }
}