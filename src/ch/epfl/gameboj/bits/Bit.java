package ch.epfl.gameboj.bits;

/**
 * Interface implemented by the enumerations that represent a set of bits.
 * @author Matthieu De Beule (Sciper: 269623)
 */
public interface Bit {

    /**
     * @return index of the Bit in the enum
     */
    public abstract int ordinal();

    /**
     * Alias for ordinal
     */
    public default int index(){
        return this.ordinal();
    }

    /**
     * @return bit's mask, a value where only the bit
     * with the same index equals 1, the others 0
     */
    public default int mask(){
        return Bits.mask(this.ordinal());
    }

}
