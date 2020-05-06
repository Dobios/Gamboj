package ch.epfl.gameboj.component;

/**
 * Interface representing a component driven by the system clock.
 */
public interface Clocked {

    /**
     * Makes the component evolve by executing all of the operations that must
     * be executed during the given cycle.
     * @param cycle the index of the given cycle.
     */
    public abstract void cycle(long cycle);

}