package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class RegisterFile<E extends Register> {
    private final byte[] allRegs;

    /**
     * Constructs the RegisterFile using the size of the given array of E.
     * @param allRegs array whose size we use to make the RegisterFile
     */
    public RegisterFile(E[] allRegs)  {

        this.allRegs = new byte[allRegs.length];
    }

    /**
     * Gets the 8bit value contained in the given register
     * @param reg the given register
     * @return an int between 0 and 0xFF.
     */
    public int get(E reg) {
        return Byte.toUnsignedInt(allRegs[reg.index()]);
    }

    /**
     * Sets the value of an 8bit register.
     * @param reg register to be changed
     * @param newValue we want to set in the register
     */
    public void set(E reg, int newValue) {
        Preconditions.checkBits8(newValue);
        allRegs[reg.index()] = (byte)newValue;
    }

    /**
     * Tests the value of the given bit
     * @param reg we want to test the value of a bit in
     * @param b bit to be tested
     * @return true iff b = 1
     */
    public boolean testBit(E reg, Bit b) {
        return Bits.test(this.get(reg), b);
    }

    /**
     * Modifies the value stored in the register such that the given bit takes
     * the given value.
     * @param reg register to be changed
     * @param bit bit we want to change
     * @param newValue value to be set at bit
     */
    public void setBit(E reg, Bit bit, boolean newValue) {
        this.set(reg, Bits.set(get(reg), bit.index(), newValue));
    }



}
