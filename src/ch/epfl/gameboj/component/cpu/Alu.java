package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

import java.util.Objects;



/**
 * Class containing a collection of methods that help work with 8 and 16bit
 * values in a ZNHC-packed form
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public final class Alu {
    public static final int MAX_24BITS = 0xFFFFFF;
    public static final int MAX_16BITS = 0xFFFF;
    public static final int MAX_12BITS = 0xFFF;
    public static final int MAX_8BITS = 0xFF;
    public static final int MAX_4BITS = 0xF;

    /**
     * Default Constructor for the class Alu
     */
    private Alu() {}

    /**
     * Represents the values given to a Flag
     */
    public enum Flag implements Bit {
        UNUSED_3, UNUSED_2, UNUSED_1, UNUSED_0, C, H, N, Z
    }

    /**
     * The different possible rotation directions
     */
    public enum RotDir {
        LEFT, RIGHT
    }

    /**
     * Packs the given value with the given v flags
     * @param v the given value
     * @param z a given flag
     * @param n a given flag
     * @param h a given flag
     * @param c a given flag
     * @return a package containing the value followed by the given flags
     */
    private static int packValueZNHC(int v, boolean z, boolean n, boolean h,
            boolean c) {
        return (v << 8) + maskZNHC(z, n, h, c);
    }

    /**
     * Creates a int out of the given flags z, n, h and c.
     * @return an int representing the grouped flags in the form 0bZNHC0000
     */
    public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {

        int C = (c) ? Flag.C.mask() : 0;
        int H = (h) ? Flag.H.mask() : 0;
        int N = (n) ? Flag.N.mask() : 0;
        int Z = (z) ? Flag.Z.mask() : 0;

        return (C | H | N | Z);
    }

    /**
     * Extracts the value from the given value/flags package.
     * @param valueFlags which contains a 16bit value and 4 flags
     * @return the value
     */
    public static int unpackValue(int valueFlags) {
        Preconditions.checkArgument(valueFlags <= MAX_24BITS);

        return Bits.extract(valueFlags, 8, 24);
    }

    /**
     * Extracts the flags from the given value/flags package.
     * @param valueFlags which contains a 16bit value and 4 flags
     * @return the flags extracted from the given package
     */
    public static int unpackFlags(int valueFlags) {
        Preconditions.checkArgument(valueFlags <= MAX_24BITS);

        return Bits.clip(8, valueFlags) & 0xF0;
    }

    /**
     * add two numbers with an initial carry
     * @param l first number
     * @param r second number
     * @param c0 initial carry (as a boolean)
     * @return calculated sum packaged with the accompanying flags Z0HC (Z tells
     * us whether the result is 0, H whether there's a carry after the addition
     * of the 4 LSB, C whether there's a carry after the complete addition)
     */
    public static int add(int l, int r, boolean c0) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        int carry = (c0) ? 1 : 0;
        boolean H = Bits.clip(4, l) + Bits.clip(4, r) + carry > MAX_4BITS;
        boolean C = l + r + carry > MAX_8BITS;
        int value = Bits.clip(8, l + r + carry);
        boolean Z = value == 0;

        return packValueZNHC(value, Z, false, H, C);

    }

    /**
     * Special case of method add(int l, int r, boolean c0) where c0 is false
     * (no initial carry)
     * @param l first number
     * @param r second number
     * @return calculated sum packaged with the accompanying flags Z0HC (Z tells
     * us whether the result is 0, H whether there's a carry after the addition
     * of the 4 LSB, C whether there's a carry after the complete addition)
     */
    public static int add(int l, int r) {
        return add(l, r, false);
    }

    /**
     * Returns the sum of two given 16bit values with 00HC flags, where H and C
     * are computed regarding the 8 LSB
     * @param l first 16bit value
     * @param r second 16bit value
     * @return the summed value, packed with the 00HC flags of the LSB
     */
    public static int add16L(int l, int r) {
        Preconditions.checkBits16(l);
        Preconditions.checkBits16(r);

        boolean H = Bits.clip(4, l) + Bits.clip(4, r) > MAX_4BITS;
        boolean C = Bits.clip(8, l) + Bits.clip(8, r) > MAX_8BITS;
        int value = Bits.clip(16, l+r);

        return packValueZNHC(value, false, false, H, C);
    }
    /**
     * Returns the sum of two given 16bit values with 00HC flags, where H and C
     * are computed regarding the 8 MSB
     * @param l first 16bit value
     * @param r second 16bit value
     * @return the summed value, packed with the 00HC flags of the MSB
     */
    public static int add16H(int l, int r) {
        Preconditions.checkBits16(l);
        Preconditions.checkBits16(r);

        int sum = Bits.clip(16, l+r);
        boolean H = Bits.clip(12, l)
                + Bits.clip(12, r) > MAX_12BITS;
        boolean C = l+r > MAX_16BITS;

        return packValueZNHC(sum, false, false, H, C);
    }

    /**
     * Subtract two 8bit values (with an initial borrow) and package with Z1HC
     * @param l first 8bit value
     * @param r second 8bit value
     * @param b0 initial borrow
     * @return packed subtracted value with Z1HC flags
     */
    public static int sub(int l, int r, boolean b0) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        int borrow = (b0) ? 1 : 0;
        boolean H = Bits.clip(4, l) < Bits.clip(4, r) + borrow;
        boolean C = l < r + borrow;
        int value = Bits.clip(8 , l - r - borrow);
        boolean Z = (value == 0);

        return packValueZNHC(value, Z, true, H, C);
    }

    /**
     * Special case of sub(int l, int r, boolean b0), with b0 set to false
     * @param l first 8bit value
     * @param r second 8bit value
     * @return packed subtracted value with Z1HC flags
     */
    public static int sub(int l, int r) {
        return sub(l, r, false);
    }

    /**
     * Adjust an 8bit value to the BCD format
     * @param v 8bit value to be adjusted
     * @param n flag N
     * @param h flag H
     * @param c flag C
     * @return BCD-adjusted, ZNHC packed value
     */
    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
        Preconditions.checkBits8(v);

        boolean fixL = h || (!n && Bits.clip(4, v) > 9);
        boolean fixH = c || (!n && v > 0x99);
        int fixLInt = (fixL) ? 1 : 0;
        int fixHInt = (fixH) ? 1 : 0;
        int fix = 0x60 * fixHInt + 0x06 * fixLInt;
        int va = n ? (v - fix) : (v + fix);
        va = Bits.clip(8, va);

        return packValueZNHC(va, va == 0, n, false, fixH);
    }

    /**
     * Calculates the "and" bit to bit operation between l and r.
     * @param l an 8bit value
     * @param r an 8bit value
     * @return l & r packed with the flags Z000.
     */
    public static int and(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        int result = (l & r) ;
        boolean z = result == 0;

        return packValueZNHC(result, z, false, true, false);
    }

    /**
     * Calculates the "or" bit to bit operation between l and r.
     * @param l an 8bit value
     * @param r an 8bit value
     * @return l | f packed with the flags Z000.
     */
    public static int or(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        int result = (l | r);
        boolean z = result == 0;

        return packValueZNHC(result, z, false, false, false);
    }

    /**
     * Calculates the "xor" bit to bit operation between l and r.
     * @param l an 8bit value
     * @param r an 8bit value
     * @return l ^ r packed with the flags Z000.
     */
    public static int xor(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);

        int result = (l ^ r);
        boolean z = result == 0;

        return packValueZNHC(result, z, false, false, false);
    }

    /**
     * Shifts the bit value of v by 1 to the left.
     * @param v an 8bit value
     * @return shifted v (1 bit to the left and then clipped to 8bit)
     * packed with the flags Z00C
     */
    public static int shiftLeft(int v) {
        Preconditions.checkBits8(v);
        int result = Bits.clip(8, v << 1);
        boolean z = result == 0;
        boolean c = Bits.extract(v, 7, 1) == 1;

        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Shifts the bit value of v by 1 to the right ( arithmetically )
     * @param v un 8bit value
     * @return shifted v (arithmetically 1 bit to the right and clipped to 8bit)
     * packed with the flags Z00C
     */
    public static int shiftRightA(int v) {
        Preconditions.checkBits8(v);
        boolean c = (Bits.clip(1, v) == 1);
        int signBit = Bits.mask(7) & v;
        int shifted = v >> 1;
        int result = shifted | signBit;
        boolean z = (result == 0);

        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Shifts the bit value of v by 1 to the right ( logically )
     * @param v 8bit value
     * @return v shifted logically to the right by 1 packed with the flags Z00C
     */
    public static int shiftRightL(int v) {
        Preconditions.checkBits8(v);
        boolean c = Bits.clip(1, v) == 1;
        int result = v >>> 1;
        boolean z = result == 0;

        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Rotates the given 8bit value in the given direction
     * @param d direction in which to rotate
     * @param v 8bit value to rotate
     * @return rotated bit with flags Z00C where C is the bit that went from
     * one extreme to the other
     */
    public static int rotate(RotDir d, int v) {
        Preconditions.checkBits8(v);
        int rotated;
        int extremeHopper;
        if (d==RotDir.LEFT){
            extremeHopper = Bits.extract(v, 7, 1);
            rotated = (Bits.clip(7, v) << 1) + extremeHopper;
        } else {
            extremeHopper = Bits.clip(1, v);
            rotated = Bits.extract(v, 1, 7) + (extremeHopper << 7);
        }
        return packValueZNHC(rotated, rotated == 0,
                false, false, extremeHopper == 1);
    }

    /**
     * Rotates the given 8bit value and a carry in the given direction
     * @param d direction in which to rotate
     * @param v 8bit value to rotate
     * @param c carry to include in rotation
     * @return packaged 8bit plus Z00C flags with C being the MSB
     * after a rotation
     */
    public static int rotate(RotDir d, int v, boolean c) {
        Preconditions.checkBits8(v);
        int C = (c) ? 1 : 0;
        int rotated;
        int newCarry;
        if (d==RotDir.LEFT){
            rotated = Bits.clip(8,(v << 1) + C);
            newCarry = Bits.extract(v, 7, 1);
        } else {
            int vWithCarry = v + (C << 8);
            rotated = vWithCarry >> 1;
            newCarry = Bits.clip(1, v);
        }
        return packValueZNHC(rotated, rotated == 0,
                false, false, newCarry == 1);
    }

    /**
     * Swap 4 LSB with 4 MSB of an 8bit value
     * @param v value to swap
     * @return value with swapped MSB & LSB and Z000 flags
     */
    public static int swap(int v) {
        Preconditions.checkBits8(v);
        int swapped = Bits.rotate(8, v, 4);
        Preconditions.checkBits8(swapped);

        return packValueZNHC(swapped, swapped == 0, false, false,
                false);
    }

    /**
     * Test whether the bit at bitIndex is 0, return Z010 with Z the answer
     * @param v test bit at given index
     * @param bitIndex where the test is done
     * @return packed 0 with Z010 flags
     */
    public static int testBit(int v, int bitIndex) {
        Preconditions.checkBits8(v);
        Objects.checkIndex(bitIndex, 8);
        boolean Z = Bits.extract(v, bitIndex, 1) == 0;

        return packValueZNHC(0, Z, false, true, false);
    }
}
