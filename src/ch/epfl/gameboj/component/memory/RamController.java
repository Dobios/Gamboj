package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

import java.util.Objects;

/**
 * Represents a component that controls access to the RAM.
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class RamController implements Component {
    private final Ram contRam;
    private final int startAddress;
    private final int endAddress;

    /**
     * Constructs a controller for the given ram, accessible between
     * startAddress(included) and endAddress(excluded)
     * @param ram we want to manage with this controller
     * @param startAddress from where ram should be accessible (included)
     * @param endAddress to where ram should be accessible (excluded)
     */
    public RamController(Ram ram, int startAddress, int endAddress) {
        Objects.requireNonNull(ram);
        Preconditions.checkArgument(endAddress >= startAddress);
        Preconditions.checkArgument(endAddress - startAddress <= ram.size());
        this.contRam = ram;
        this.startAddress = Preconditions.checkBits16(startAddress);
        this.endAddress = Preconditions.checkBits16(endAddress);
    }

    /**
     * Constructs a controller that has access to the entirety of the given ram.
     * @param ram we want to manage with this controller
     * @param startAddress from where ram should be accessible (included)
     */
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, ram.size() + startAddress);
    }

    /**
     * Reads the byte located at the given address in the ram controlled by
     * the ramController.
     * @param address, the given location
     * @return the byte located at address in contRam
     */
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (startAddress <= address && address < endAddress) {
            return this.contRam.read(address - startAddress);
        }
        return NO_DATA;
    }

    /**
     * Writes the given data at the given address
     * @param address where to write data
     * @param data to be written in the RAM
     */
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        if (address >= startAddress && endAddress > address ) {
            this.contRam.write(address - startAddress, data);
        }
    }

}
