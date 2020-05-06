package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Component;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Class simulating the address and data buses interconnecting the GameBoy's
 * components.
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public final class Bus {
    private final ArrayList<Component> attachedComp = new ArrayList<>();

    /**
     * Attaches the given component to the bus
     * @param component to attach to the bus
     */
    public void attach(Component component) {
        Objects.requireNonNull(component);
        attachedComp.add(component);
    }

    /**
     * Returns the value stored at the given address if at least one of the
     * components attached to the bus contain a value at the address.
     * @param address we want to read
     * @return value stored at address or 0xFF if no component has this value
     */
    public int read(int address) {
        int value = 0xFF;

       Preconditions.checkBits16(address);

        for (Component component: attachedComp) {
            int compValue =  component.read(address);
            if (compValue!= Component.NO_DATA) {
                return compValue;
            }
        }
        return value;
    }

    /**
     * Writes the parameter data at the given address of each component attached
     * to the bus.
     * @param address, where to write the data
     * @param data we want to write
     */
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        Preconditions.checkBits16(address);
        for (Component component: attachedComp) {
            component.write(address, data);
        }
    }
}
