package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

import java.util.Objects;

/**
 * Class simulating a BootRom controller.
 * @author Matthieu De Beule (Sciper: 269623)
 */
public class BootRomController implements Component {

    private final Cartridge cartridge;
    private final Rom bootRom;

    //this boolean will be set to false once 0xFF50 is written to
    private boolean bootRomActivated;

    /**
     * Make new controller of the boot ROM. Handles whether data is read from
     * the boot ROM or from the cartridge.
     * @param cartridge which will be connected to the bus
     *                  through this BootRomController
     */
    public BootRomController(Cartridge cartridge){
        Objects.requireNonNull(cartridge);
        bootRom = new Rom(BootRom.DATA);
        this.cartridge = cartridge;
        bootRomActivated = true;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);

        if (AddressMap.BOOT_ROM_START <= address &&
                address < AddressMap.BOOT_ROM_END)
            if (bootRomActivated) {
                return Byte.toUnsignedInt((byte)bootRom.read(address));
            }
        return cartridge.read(address);
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        if (address == AddressMap.REG_BOOT_ROM_DISABLE)
            bootRomActivated = false;
        cartridge.write(address, data);
    }
}
