package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;

import java.util.Objects;

/**
 * Class simulating the GameBoy's Timer.
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public final class Timer implements Component, Clocked{
    private static final int PRIMARY_COUNTER_INC = 4;

    private Cpu cpu;
    private int primaryCounter;
    private int TIMA; //secondary counter
    private int TMA;
    private int TAC;

    /**
     * Constructs the Timer associating it to the given cpu.
     * @param cpu the given cpu that will be associated to the Timer
     */
    public Timer(Cpu cpu) {
        Objects.requireNonNull(cpu);

        this.cpu = cpu;
        primaryCounter = 0;
        TIMA = 0;
        TMA = 0;
        TAC = 0;
    }

    @Override
    public void cycle(long cycle) {

        boolean s0 = state();
        primaryCounter = Bits.clip(Byte.SIZE * 2, primaryCounter + PRIMARY_COUNTER_INC);
        incIfChange(s0);
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);

        switch (address) {
        case AddressMap.REG_DIV :
            return Bits.extract(primaryCounter, Byte.SIZE, Byte.SIZE);
        case AddressMap.REG_TIMA :
            return TIMA;
        case AddressMap.REG_TMA :
            return TMA;
        case AddressMap.REG_TAC :
            return TAC;
        default:
            return NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {

        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        boolean s0 = state();
        switch (address) {
        case AddressMap.REG_DIV :
            primaryCounter = 0;
            incIfChange(s0);
            break;
        case AddressMap.REG_TIMA :
            TIMA = data;
            break;
        case AddressMap.REG_TMA :
            TMA = data;
            break;
        case AddressMap.REG_TAC :
            TAC = data;
            incIfChange(s0);
            break;
        default:
            break;
        }
    }

    /**
     * Returns the "state" of the CPU by computing the bit by bit and of the
     * bit at index 2 of the TAC timer and the index in the primary counter
     * associated to the the 2 LSBs of TAC.
     * @return the state of the CPU.
     */
    private boolean state(){

        boolean activated = Bits.test(TAC, 2);
        boolean[] whichBit = {
                Bits.test(primaryCounter, 9),
                Bits.test(primaryCounter, 3),
                Bits.test(primaryCounter, 5),
                Bits.test(primaryCounter, 7)
        };
        return activated && whichBit[Bits.clip(2, TAC)];
    }

    /**
     * Increments the secondary counter when the the given argument s0
     * differs from the current state of the CPU.
     * @param s0 the given argument representing the previous state of the
     * CPU.
     */
    private void incIfChange(boolean s0){

        if (s0 && !state()){
            if(TIMA >= 0xFF){
                cpu.requestInterrupt(Cpu.Interrupt.TIMER);
                TIMA = TMA;
            } else {
                TIMA++;
            }
        }
    }
}
