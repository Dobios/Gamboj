package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;

import static java.lang.Byte.toUnsignedInt;

/**
 * Class representing the GameBoy's Random Access Memory.
 * @author Matthieu De Beule (Sciper: 269623)
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class Ram {

    private final int size;
    private final byte[] ram;

    /**
     * Construct new Ram object of a given size (in bytes)
     * @param size Size of the new RAM object
     */
    public Ram(int size) {
        Preconditions.checkArgument(size >= 0);
        this.size = size;
        ram = new byte[size];
    }

    /**
     * @return byte size of RAM
     */
    public int size(){
        return this.size;
    }

    /**
     * Returns RAM byte at the given index
     * @param index of the wanted byte
     * @return the RAM byte as an int (between 0 and FF)
     */
    public int read(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        } else {
            return toUnsignedInt(this.ram[index]);
        }
    }

    /**
     * Modifies RAM content at given index
     * @param index of the byte that has to be overwritten
     * @param value of the byte that will be written (between 0 and FF)
     */
    public void write(int index, int value){
        if (index < 0 || index >= size){
            throw new IndexOutOfBoundsException();
        } else Preconditions.checkBits8(value);

        ram[index] = (byte) value;
    }
}