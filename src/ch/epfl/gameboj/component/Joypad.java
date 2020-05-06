package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;

import java.util.Objects;

/**
 * @author Andrew Dobis (Sciper: 272002)
 * Simulates the GameBoy's Joypad
 */
public class Joypad implements Component {
    private final static int LINE0_BIT = 4;
    private final static int LINE1_BIT = 5;
    private final static int MASK = 0b1111 << 4;

    private final Cpu cpu;

    private int line0;
    private int line1;
    private int regP1;

    /**
     * Represents the different keys on the Joypad
     */
    public enum Key implements Bit {
        RIGHT, LEFT, UP, DOWN, A, B, SELECT, START;

        private int getLine() {
            return ordinal() > DOWN.ordinal() ? 1 : 0;
        }

        private int getColumn() {
            return ordinal() <= DOWN.ordinal() ? ordinal() : ordinal() - A.ordinal();
        }
    }

    /**
     * Constructs the Joypad
     * @param cpu the GameBoy's cpu
     */
    public Joypad(Cpu cpu) {
        Objects.requireNonNull(cpu);

        this.cpu = cpu;
        regP1 = 0;
        line0 = 0;
        line1 = 0;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);

        if(address == AddressMap.REG_P1)
            return Bits.complement8(regP1 & p1State());

        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        int lineBits = Bits.extract(regP1, LINE0_BIT, 2);
        int dataLineBits = Bits.extract(data, LINE0_BIT, 2);

        int invData = Bits.complement8(data);

        if(address == AddressMap.REG_P1) {
            if(lineBits != dataLineBits)
                cpu.requestInterrupt(Cpu.Interrupt.JOYPAD);
            regP1 = Bits.set(regP1, LINE0_BIT, Bits.test(invData, LINE0_BIT));
            regP1 = Bits.set(regP1, LINE1_BIT, Bits.test(invData, LINE1_BIT));
        }
    }

    /**
     * Checks if the given key is pressed or not
     * @param key the given key
     * @return true if the key is pressed, false otherwise
     */
    private boolean isPressed(Key key) {
        boolean atLine1 = key.getLine() == 1;
        int line = atLine1 ? line1 : line0;
        return Bits.test(line, key.ordinal()) &&
                Bits.test(regP1, atLine1 ? LINE1_BIT : LINE0_BIT);
    }

    /**
     * Simulates the pressing of a key
     * @param key the key that is being pressed
     */
    public void keyPressed(Key key) {

        if(key.getLine() == 1)
            line1 = Bits.set(line1, key.getColumn(), true);
        else if(key.getLine() == 0)
            line0 = Bits.set(line0, key.getColumn(), true);

        if(isPressed(key))
            cpu.requestInterrupt(Cpu.Interrupt.JOYPAD);

        regP1 |= line0 | line1;
    }

    /**
     * Simulates the releasing of a key
     * @param key the key that is being released
     */
    public void keyReleased(Key key) {

        if(key.getLine() == 1)
            line1 = Bits.set(line1, key.getColumn(), false);
        else if(key.getLine() == 0)
            line0 = Bits.set(line0, key.getColumn(), false);

        regP1 &= (MASK | line0 | line1);
    }

    /**
     * returns the actual state of the p1 register
     */
    private int p1State() {
        int result = MASK;
        if(Bits.test(regP1, LINE0_BIT)) {
            result |= line0;
        }
        if(Bits.test(regP1, LINE1_BIT)) {
            result |= line1;
        }
        return result;
    }
}
