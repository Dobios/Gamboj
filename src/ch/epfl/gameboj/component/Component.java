package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

/**
 * Interface implemented by all the GameBoy's simulated components that are
 * connected to a bus.
 * @author Andrew Dobis (Sciper: 272002)
 */
public interface Component {
    public static final int NO_DATA = 0x100;

    /**
     * Returns the byte stored at the given address
     * @param address, the given location
     * @return byte stored or NO_DATA
     */
    public abstract int read(int address);

    /**
     * Stores the value given at address in the component
     * @param address, location of the value in the component
     * @param data to be written at address
     */
    public abstract void write(int address, int data);

    /**
     * Attaches the component to the given bus
     * @param bus to attach to
     */
    public default void attachTo(Bus bus) {
        bus.attach(this);
    }

}
