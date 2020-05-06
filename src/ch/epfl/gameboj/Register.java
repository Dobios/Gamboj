package ch.epfl.gameboj;

/**
 * Interface implemented by all of the GameBoy's registers.
 * @author Andrew Dobis (Sciper: 272002)
 */
public interface Register {

    /**
     * @return the index of the bit in the associated enumeration.
     */
    abstract public int ordinal();

    /**
     * default version of the ordinal method
     * @return same value as ordinal method.
     */
    public default int index() {
        return ordinal();
    }

}
