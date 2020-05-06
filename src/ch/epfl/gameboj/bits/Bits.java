package ch.epfl.gameboj.bits;
import ch.epfl.gameboj.Preconditions;

import java.util.Objects;

/**
 * Class offering multiple static bit manipulation methods.
 * @author Matthieu De Beule (Sciper: 269623)
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class Bits {
    //Table containing the inverse of every 8 bit value given as the index.
    private final static int[] REVERSE8 = new int[] {
            0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0,
            0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0,
            0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8,
            0x18, 0x98, 0x58, 0xD8, 0x38, 0xB8, 0x78, 0xF8,
            0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4,
            0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
            0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC,
            0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC,
            0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2,
            0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2,
            0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA,
            0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA,
            0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6,
            0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6,
            0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E, 0xEE,
            0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
            0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1,
            0x11, 0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1,
            0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9,
            0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9,
            0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5,
            0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5,
            0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED,
            0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD,
            0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3,
            0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
            0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB,
            0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB,
            0x07, 0x87, 0x47, 0xC7, 0x27, 0xA7, 0x67, 0xE7,
            0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7,
            0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF,
            0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF,
    };

    /**
     * Default constructor.
     */
    private Bits() {}

    /**
     * Make a mask of the given index
     * @param index where there should be a 1 in the mask
     * @return int with a 1 at the given index
     */
    public static int mask(int index){
        Objects.checkIndex(index, Integer.SIZE);

        return 1 << index;
    }

    /**
     * Returns true iff the bit at the given index is 1
     * @param bits number in which we'll check for a 1 in index
     * @param index where we're checking for a 1
     * @return whether there is a 1 in the index position
     * @throws IndexOutOfBoundsException if index is not a valid index
     */
    public static boolean test(int bits, int index){
        Objects.checkIndex(index, Integer.SIZE);

        int mask = mask(index);
        return (mask & bits) == mask;
    }

    /**
     * Returns true iff there's a 1 at given bit
     * @param bits number in which we'll check for a 1
     * @param bit where we're checking for a 1
     * @return whether there is a 1 in the same position as the bit
     */
    public static boolean test(int bits, Bit bit){
        return test(bits, bit.index());
    }

    /**
     * Return bits changed only at the given index to the given newValue
     * @param bits the given bit string modified by the set
     * @param index the index at which we want to modify the bit
     * @param newValue New value of the bit at the given index
     * @return bit different from bits only at the index, with given new value
     * @throws IndexOutOfBoundsException if index is not a valid index
     */
    public static int set(int bits, int index, boolean newValue) {
        Objects.checkIndex(index, Integer.SIZE);
        if (newValue) {
            return mask(index) | bits;
        } else {
            return bits & ~mask(index);
        }
    }

    /**
     * Only keep the size LSB, others being set to 0
     * @param size how many bits to return
     * @param bits of which we want to clip of the most significant bits
     * @return value where the size LSB are equal to the size LSB of bits,
     * others being set to 0
     */
    public static int clip(int size, int bits) {

        Preconditions.checkArgument(size >= 0 && size <= Integer.SIZE);

        if (size == Integer.SIZE){
            return bits;
        }
        return (mask(size) - 1) & bits;
    }

    /**
     * Extracts a range of bits starting at index start (included)
     * and ending at start+size (excluded)
     * @param bits, from which we will extract our result
     * @param start the index at which the extraction is started
     * @param size the size of the bit string that is extracted
     * @throws IndexOutOfBoundsException if start and size do not represent a
     * valid interval.
     * @return value equal to a range of bits from start, of size size
     */
    public static int extract(int bits, int start, int size) {
        Objects.checkFromIndexSize(start, size, Integer.SIZE);
        int movedBits = bits >>> start;

        return clip(size, movedBits);
    }

    /**
     * Returns the size LSB of bits but with a rotation by a factor of distance.
     * @param size the size of the bit string extracted for the rotation
     * @param bits the bit string used for the rotation
     * @param distance the distance of the rotation
     * @return a rotated version of the size LSB of bits
     */
    public static int rotate(int size, int bits, int distance) {
        Preconditions.checkArgument(size <= Integer.SIZE && size > 0);
        Preconditions.checkArgument(clip(size, bits) == bits);

        //accommodate for negative output of java's %
        int modDistance = Math.floorMod(distance, size);
        int rotatedLeftBits = (bits << modDistance);
        int rotatedRightBits = (bits >>> (size - modDistance));

        return clip(size,rotatedLeftBits | rotatedRightBits);
    }

    /**
     * Extend the sign of the given 8bit integer
     * (https://en.wikipedia.org/wiki/Sign_extension)
     * @param b given integer that must be extended
     * @return the same integer, but extended
     */
    public static int signExtend8(int b) {
        Preconditions.checkBits8(b);

        return (int) (byte) b;
    }

    /**
     * Returns the same int but with the 8 LSB swapped
     * (0&7, 1&6, 2&5, 3&4)
     * @param b 8bit value we want to reverse
     * @return int with reversed 8 LSB
     */
    public static int reverse8(int b) {
        Preconditions.checkBits8(b);

        return REVERSE8[b];
    }

    /**
     * Return the complement of b
     * @param b 8bit value we want to get the complement of
     * @return b with 8 LSB inverted
     */
    public static int complement8(int b) {
        Preconditions.checkBits8(b);

        return b ^ 0xFF;
    }

    /**
     * Make a 16bit value out of two 8bit values
     * @param highB 8bit value that is to be the 8 MSB
     * @param lowB 8bit value that is to be the 8 LSB
     * @return 16bit value where the 8 MSB are highB and the 8 LSB are lowB
     */
    public static int make16(int highB, int lowB) {
        Preconditions.checkBits8(highB);
        Preconditions.checkBits8(lowB);

        return (highB << 8) | (lowB);
    }
}
