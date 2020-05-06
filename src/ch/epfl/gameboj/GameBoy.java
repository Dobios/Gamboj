package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

import java.util.Objects;

import static ch.epfl.gameboj.AddressMap.*;

/**
 * Class representing a GameBoy and attaching each one of its components
 * to a common bus.
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public class GameBoy {
    public static final long CYCLES_PER_SECOND = 0x100000; //2^20
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND * 1e-9;

    private final Bus compsBus = new Bus();
    private final Ram workRam;
    private final RamController wRCont;
    private final RamController eRCont;
    private final Cpu cpu;
    private final BootRomController bootRomController;
    private final Timer timer;
    private final LcdController lcdCont;
    private final Joypad joypad;

    private long cycle;

    /**
     * Constructs a GameBoy
     * @param cartridge the given cartridge containing the ROM file.
     */
    public GameBoy(Cartridge cartridge) {
        Objects.requireNonNull(cartridge);
        cycle = 0;

        workRam = new Ram(WORK_RAM_SIZE );
        wRCont = new RamController(workRam, WORK_RAM_START, WORK_RAM_END);
        wRCont.attachTo(compsBus);

        eRCont = new RamController(workRam, ECHO_RAM_START, ECHO_RAM_END);
        eRCont.attachTo(compsBus);

        cpu = new Cpu();
        cpu.attachTo(compsBus);

        bootRomController = new BootRomController(cartridge);
        bootRomController.attachTo(compsBus);

        timer = new Timer(cpu);
        timer.attachTo(compsBus);

        lcdCont = new LcdController(cpu);
        lcdCont.attachTo(compsBus);

        joypad = new Joypad(cpu);
        joypad.attachTo(compsBus);
    }

    /**
     * Returns the bus containing all attached components of the GameBoy.
     * @return compsBus, Bus containing all attached components
     */
    public Bus bus() {
        return compsBus;
    }

    /**
     * Returns the timer associated to this GameBoy
     * @return the timer contained in this GameBoy
     */
    public Timer timer() {
        return timer;
    }

    /**
     * Returns this GameBoy's cpu
     * @return cpu
     */
    public Cpu cpu(){
        return cpu;
    }

    /**
     * Returns the cycle associated to this GameBoy's cpu
     * @return cycle
     */
    public long cycles(){
        return cycle;
    }

    /**
     * Returns the lcdController associated to this GameBoy's screen.
     * @return lcdCont
     */
    public LcdController lcdController() {
        return lcdCont;
    }

    /**
     * Returns the Joypad associated to this GameBoy
     * @return joypad
     */
    public Joypad joypad() {
        return joypad;
    }

    /**
     * Simulates the GameBoy until cycle - 1
     * @param cycle count where we stop running
     */
    public void runUntil(long cycle) {
        Preconditions.checkArgument(this.cycle <= cycle);

        while (this.cycle < cycle){
            timer.cycle(this.cycle);
            lcdCont.cycle(this.cycle);
            cpu.cycle(this.cycle);

            ++this.cycle;
        }
    }
}
