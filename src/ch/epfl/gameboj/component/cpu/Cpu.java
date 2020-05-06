package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.*;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

import java.util.Objects;

import static ch.epfl.gameboj.bits.Bits.*;
import static ch.epfl.gameboj.component.cpu.Alu.*;

/**
 * Class simulating the GameBoy's CPU.
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public final class Cpu implements Clocked, Component {
    public static final int OPCODE_PREFIX = 0xCB;

    private final RegisterFile<Reg> registerFile;
    private static final Opcode[] DIRECT_OPCODE_TABLE =
            buildOpcodeTable(Opcode.Kind.DIRECT);
    private static final Opcode[] PREFIXED_OPCODE_TABLE =
            buildOpcodeTable(Opcode.Kind.PREFIXED);

    private Bus aBus;
    private long nextNonIdleCycle;

    private int regPC;
    private int regSP;
    private int regIE;
    private int regIF;
    private boolean regIME;
    private Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);
    private RamController hrc = new RamController(highRam, 0);

    private final Reg16[] regs = {Reg16.BC, Reg16.DE, Reg16.HL, Reg16.AF};

    /**
     * Builds a CPU
     */
    public Cpu() {
        this.registerFile = new RegisterFile<>(Reg.values());
        regPC = 0;
        regSP = 0;
        regIE = 0;
        regIF = 0;
        regIME = false;
    }

    @Override
    public void cycle(long cycle) {

        if (nextNonIdleCycle < cycle) {
            return;
        }

        //Reaction to the HALT command
        if(nextNonIdleCycle == Long.MAX_VALUE) {
            if(Bits.clip(5, regIE & regIF) != 0) {
                nextNonIdleCycle = cycle;
                reallyCycle();
            }
        }
        if(nextNonIdleCycle == cycle) {
            reallyCycle();
        }
    }

    /**
     * Method called by cycle.
     * Runs the interruption handler and calls the dispatch.
     */
    private void reallyCycle() {

        int andIEIF = Bits.clip(5, regIE & regIF);

        //Deals with Interruptions
        if(regIME && andIEIF != 0) {
            int index = Integer.numberOfTrailingZeros(Integer.lowestOneBit(andIEIF));

            regIME = false;
            regIF = Bits.set(regIF, index, false);
            push16(regPC);
            regPC = AddressMap.INTERRUPTS[index];
            nextNonIdleCycle += 5;
        }
        //Calls the dispatch function run the current command found int opcode
        else {
            int valuePC = read8(regPC);
            if (valuePC == OPCODE_PREFIX){
                dispatch(PREFIXED_OPCODE_TABLE[read8AfterOpcode()]);
            } else {
                dispatch(DIRECT_OPCODE_TABLE[valuePC]);
            }
        }
    }

    /**
     * Enumeration containing all of the simulator's 8 bit registers.
     */
    private enum Reg implements Register {
        A, F, B, C, D, E, H, L
    }

    /**
     * Enumeration containing all of the simulator's 16 bit registers.
     */
    private enum Reg16 {
        AF(Reg.A, Reg.F), BC(Reg.B, Reg.C), DE(Reg.D, Reg.E), HL(Reg.H, Reg.L);

        Reg highReg;
        Reg lowReg;

        Reg16(Reg h, Reg l) {
            this.highReg = h;
            this.lowReg = l;
        }
    }

    /**
     * Enumeration containing all the 5 possible interruptions caused by the
     * GameBoy different components.
     */
    public enum Interrupt implements Bit {
        VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
    }

    /**
     * Raises the given interruption by modifying the value of the bit found at
     * the related index in IF.
     * @param i the interruption that will be raised
     */
    public void requestInterrupt(Interrupt i) {
        regIF = Bits.set(regIF, i.index(), true);
    }

    /**
     * Used to give the tests access to the registers.
     * @return an int[] containing the registers
     */
    public int[] _testGetPcSpAFBCDEHL() {
        return new int[]{regPC, regSP, registerFile.get(Reg.A),
                registerFile.get(Reg.F), registerFile.get(Reg.B),
                registerFile.get(Reg.C), registerFile.get(Reg.D),
                registerFile.get(Reg.E), registerFile.get(Reg.H),
                registerFile.get(Reg.L)};
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);

        if (address == AddressMap.REG_IE) {
            return regIE;
        }
        else if(address == AddressMap.REG_IF){
            return regIF;
        }
        else if((AddressMap.HIGH_RAM_START <= address) &&
                (address < AddressMap.HIGH_RAM_END)) {

            return highRam.read(address - AddressMap.HIGH_RAM_START);
        }
        else {
            return NO_DATA;
        }

    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        Preconditions.checkBits16(address);

        if (address == AddressMap.REG_IE) {
            regIE = data;
        }
        if (address == AddressMap.REG_IF) {
            regIF = data;
        }
        if (AddressMap.HIGH_RAM_START <= address &&
                address < AddressMap.HIGH_RAM_END){

            highRam.write(address - AddressMap.HIGH_RAM_START, data);

        }
    }

    @Override
    public void attachTo(Bus bus) {
        this.aBus = bus;
        Component.super.attachTo(bus);
    }

    /**
     * Interprets the given opcode by running the associated command given to
     * the cpu.
     * @param opcode we want to interpret
     */
    private void dispatch(Opcode opcode) {

        int postPC = regPC + opcode.totalBytes;
        nextNonIdleCycle += opcode.cycles;

        switch (opcode.family) {
        case NOP: {
        } break;
        case LD_R8_HLR: {
            Reg r = extractReg(opcode, 3);
            registerFile.set(r, read8AtHl());
        } break;
        case LD_A_HLRU: {
            registerFile.set(Reg.A, read8AtHl());
            setReg16(Reg16.HL, Bits.clip(16,reg16(Reg16.HL) + extractHlIncrement(opcode)));
        } break;
        case LD_A_N8R: {
            registerFile.set(Reg.A , read8(AddressMap.REGS_START + read8AfterOpcode()));
        } break;
        case LD_A_CR: {
            registerFile.set(Reg.A, read8(AddressMap.REGS_START + registerFile.get(Reg.C)));
        } break;
        case LD_A_N16R: {
            registerFile.set(Reg.A, read8(read16AfterOpcode()));
        } break;
        case LD_A_BCR: {
            registerFile.set(Reg.A, read8(reg16(Reg16.BC)));
        } break;
        case LD_A_DER: {
            registerFile.set(Reg.A, read8(reg16(Reg16.DE)));
        } break;
        case LD_R8_N8: {
            Reg r = extractReg(opcode, 3);
            registerFile.set(r, read8AfterOpcode());
        } break;
        case LD_R16SP_N16: {
            Reg16 r = extractReg16(opcode);
            setReg16SP(r, read16AfterOpcode());
        } break;
        case POP_R16: {
            Reg16 r = extractReg16(opcode);
            setReg16(r, pop16());
        } break;
        case LD_HLR_R8: {
            Reg r = extractReg(opcode, 0);
            write8AtHl(registerFile.get(r));
        } break;
        case LD_HLRU_A: {
            write8AtHl(registerFile.get(Reg.A));
            setReg16(Reg16.HL, Bits.clip(16, reg16(Reg16.HL)
                    + extractHlIncrement(opcode)));
        } break;
        case LD_N8R_A: {
            write8(AddressMap.REGS_START + read8AfterOpcode(),
                    registerFile.get(Reg.A));
        } break;
        case LD_CR_A: {
            write8(AddressMap.REGS_START + registerFile.get(Reg.C),
                    registerFile.get(Reg.A));
        } break;
        case LD_N16R_A: {
            write8(read16AfterOpcode(), registerFile.get(Reg.A));
        } break;
        case LD_BCR_A: {
            write8(reg16(Reg16.BC), registerFile.get(Reg.A));
        } break;
        case LD_DER_A: {
            write8(reg16(Reg16.DE), registerFile.get(Reg.A));
        } break;
        case LD_HLR_N8: {
            write8AtHl(read8AfterOpcode());
        } break;
        case LD_N16R_SP: {
            write16(read16AfterOpcode(), regSP);
        } break;
        case LD_R8_R8: {
            Reg r = extractReg(opcode, 3);
            Reg s = extractReg(opcode, 0);
            registerFile.set(r, registerFile.get(s));
        } break;
        case LD_SP_HL: {
            regSP = reg16(Reg16.HL);
        } break;
        case PUSH_R16: {
            Reg16 r = extractReg16(opcode);
            push16(reg16(r));
        } break;

        // Add
        case ADD_A_R8: {
            Reg r = extractReg(opcode, 0);
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            setRegFlags(Reg.A, add(registerFile.get(Reg.A), registerFile.get(r),
                    (bit3 && c)));
        } break;
        case ADD_A_N8: {
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            setRegFlags(Reg.A, add(registerFile.get(Reg.A), read8AfterOpcode(),
                    (bit3 && c)));
        } break;
        case ADD_A_HLR: {
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            setRegFlags(Reg.A, add(registerFile.get(Reg.A), read8AtHl(),
                    (bit3 && c)));
        } break;
        case INC_R8: {
            Reg r = extractReg(opcode, 3);
            int incR = add(registerFile.get(r), 1);
            setRegFromAlu(r, incR);
            combineAluFlags(incR, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.CPU);
        } break;
        case INC_HLR: {
            int incHL = add(read8AtHl(), 1);
            write8AtHlAndCombineAluFlags(incHL, FlagSrc.ALU, FlagSrc.V0,
                    FlagSrc.ALU, FlagSrc.CPU);
        } break;
        case INC_R16SP: {
            Reg16 r = extractReg16(opcode);
            int rVal = extractReg16SPValue(opcode);
            int incR = Bits.clip(16, rVal + 1);
            setReg16SP(r, incR);
        } break;
        case ADD_HL_R16SP: {
            int rVal = extractReg16SPValue(opcode);
            int packHl = add16H(reg16(Reg16.HL), rVal);
            setReg16(Reg16.HL, unpackValue(packHl));
            combineAluFlags(packHl, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.ALU);

        } break;
        case LD_HLSP_S8: {
            int v = add16L(regSP, Bits.clip(16,
                    Bits.signExtend8(read8AfterOpcode())));
            int e = unpackValue(v);
            boolean b = test(opcode.encoding, 4);
            setReg16SP(b ? Reg16.HL: Reg16.AF, e);
            setFlags(v);
        } break;

        // Subtract
        case SUB_A_R8: {
            Reg r = extractReg(opcode, 0);
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            setRegFlags(Reg.A, sub(registerFile.get(Reg.A), registerFile.get(r),
                    (bit3 && c)));
        } break;
        case SUB_A_N8: {
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            setRegFlags(Reg.A, sub(registerFile.get(Reg.A), read8AfterOpcode(),
                    (bit3 && c)));
        } break;
        case SUB_A_HLR: {
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            setRegFlags(Reg.A, sub(registerFile.get(Reg.A), read8AtHl(),
                    (bit3 && c)));
        } break;
        case DEC_R8: {
            Reg r = extractReg(opcode, 3);
            int decR = sub(registerFile.get(r), 1);
            combineAluFlags(decR, FlagSrc.ALU, FlagSrc.V1,
                    FlagSrc.ALU, FlagSrc.CPU);
            setRegFromAlu(r, decR);
        } break;
        case DEC_HLR: {
            int decHL = sub(read8AtHl(), 1);
            write8AtHlAndCombineAluFlags(decHL, FlagSrc.ALU, FlagSrc.V1,
                    FlagSrc.ALU, FlagSrc.CPU);
        } break;
        case CP_A_R8: {
            Reg r = extractReg(opcode, 0);
            setFlags(sub(registerFile.get(Reg.A), registerFile.get(r)));
        } break;
        case CP_A_N8: {
            setFlags(sub(registerFile.get(Reg.A), read8AfterOpcode()));
        } break;
        case CP_A_HLR: {
            setFlags(sub(registerFile.get(Reg.A), read8AtHl()));
        } break;
        case DEC_R16SP: {
            Reg16 r = extractReg16(opcode);
            int rVal = extractReg16SPValue(opcode);
            int decR = Bits.clip(16,rVal - 1);
            setReg16SP(r, decR);
        } break;

        // And, or, xor, complement
        case AND_A_N8: {
            setRegFlags(Reg.A, and(registerFile.get(Reg.A),
                    read8AfterOpcode()));
        } break;
        case AND_A_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(Reg.A, and(registerFile.get(Reg.A),
                    registerFile.get(r)));
        } break;
        case AND_A_HLR: {
            setRegFlags(Reg.A, and(registerFile.get(Reg.A), read8AtHl()));
        } break;
        case OR_A_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(Reg.A, or(registerFile.get(Reg.A),registerFile.get(r)));
        } break;
        case OR_A_N8: {
            setRegFlags(Reg.A, or(registerFile.get(Reg.A), read8AfterOpcode()));
        } break;
        case OR_A_HLR: {
            setRegFlags(Reg.A, or(registerFile.get(Reg.A), read8AtHl()));
        } break;
        case XOR_A_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(Reg.A, xor(registerFile.get(Reg.A),
                    registerFile.get(r)));
        } break;
        case XOR_A_N8: {
            setRegFlags(Reg.A, xor(registerFile.get(Reg.A),
                    read8AfterOpcode()));
        } break;
        case XOR_A_HLR: {
            setRegFlags(Reg.A, xor(registerFile.get(Reg.A), read8AtHl()));
        } break;
        case CPL: {
            int notA = complement8(registerFile.get(Reg.A));
            combineAluFlags(0, FlagSrc.CPU, FlagSrc.V1,
                    FlagSrc.V1, FlagSrc.CPU);
            registerFile.set(Reg.A, notA);
        } break;

        // Rotate, shift
        case ROTCA: {
            int rot = rotate(extractRotDir(opcode), registerFile.get(Reg.A));
            combineAluFlags(rot, FlagSrc.V0, FlagSrc.V0,
                    FlagSrc.V0, FlagSrc.ALU);
            setRegFromAlu(Reg.A, rot);
        } break;
        case ROTA: {
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            int rot = rotate(extractRotDir(opcode), registerFile.get(Reg.A), c);
            combineAluFlags(rot, FlagSrc.V0, FlagSrc.V0,
                    FlagSrc.V0, FlagSrc.ALU);
            setRegFromAlu(Reg.A, rot);
        } break;
        case ROTC_R8: {
            Reg r = extractReg(opcode, 0);
            int rot = rotate(extractRotDir(opcode), registerFile.get(r));
            setRegFlags(r, rot);
        } break;
        case ROT_R8: {
            Reg r = extractReg(opcode, 0);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            int rot = rotate(extractRotDir(opcode), registerFile.get(r), c);
            setRegFlags(r, rot);
        } break;
        case ROTC_HLR: {
            int rotHL = rotate(extractRotDir(opcode), read8AtHl());
            write8AtHlAndSetFlags(rotHL);
        } break;
        case ROT_HLR: {
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            int rotHL = rotate(extractRotDir(opcode), read8AtHl(), c);
            write8AtHlAndSetFlags(rotHL);
        } break;
        case SWAP_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(r, swap(registerFile.get(r)));
        } break;
        case SWAP_HLR: {
            write8AtHlAndSetFlags(swap(read8AtHl()));
        } break;
        case SLA_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(r, Alu.shiftLeft(registerFile.get(r)));
        } break;
        case SRA_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(r, Alu.shiftRightA(registerFile.get(r)));
        } break;
        case SRL_R8: {
            Reg r = extractReg(opcode, 0);
            setRegFlags(r, Alu.shiftRightL(registerFile.get(r)));
        } break;
        case SLA_HLR: {
            write8AtHlAndSetFlags(Alu.shiftLeft(read8AtHl()));
        } break;
        case SRA_HLR: {
            write8AtHlAndSetFlags(Alu.shiftRightA(read8AtHl()));
        } break;
        case SRL_HLR: {
            write8AtHlAndSetFlags(Alu.shiftRightL(read8AtHl()));
        } break;

        // Bit test and set
        case BIT_U3_R8: {
            Reg r = extractReg(opcode, 0);
            int index = extractTestIndex(opcode);
            int tVal = testBit(registerFile.get(r), index);
            combineAluFlags(tVal, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1,
                    FlagSrc.CPU);
        } break;
        case BIT_U3_HLR: {
            int index = extractTestIndex(opcode);
            int tHl = testBit(read8AtHl(), index);
            combineAluFlags(tHl, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1,
                    FlagSrc.CPU);
        } break;
        case CHG_U3_R8: {
            Reg r = extractReg(opcode, 0);
            int n = Bits.extract(opcode.encoding, 3, 3);
            if(extractModifier(opcode)) {
                setRegFromAlu(r, or(registerFile.get(r),
                        Bits.clip(8, (1 << n))));
            } else {
                setRegFromAlu(r, and(registerFile.get(r),
                        Bits.clip(8,~(1 << n))));
            }
        } break;
        case CHG_U3_HLR: {
            int n = Bits.extract(opcode.encoding, 3, 3);
            if(extractModifier(opcode)) {
                write8AtHlAndCombineAluFlags(or(read8AtHl(),
                        Bits.clip(8, (1 << n))),
                        FlagSrc.CPU, FlagSrc.CPU, FlagSrc.CPU, FlagSrc.CPU);
            } else {
                write8AtHlAndCombineAluFlags(and(read8AtHl(),
                        Bits.clip(8,~(1 << n))),
                        FlagSrc.CPU, FlagSrc.CPU, FlagSrc.CPU, FlagSrc.CPU);
            }
        } break;

        // Misc. ALU
        case DAA: {
            int a = registerFile.get(Reg.A);
            boolean c = Bits.test(registerFile.get(Reg.F), 4);
            boolean h = Bits.test(registerFile.get(Reg.F), 5);
            boolean n = Bits.test(registerFile.get(Reg.F), 6);
            int packA = bcdAdjust(a, n, h, c);
            setRegFromAlu(Reg.A, packA);
            combineAluFlags(packA, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0,
                    FlagSrc.ALU);
        } break;
        case SCCF: {
            boolean bit3 = Bits.test(opcode.encoding, 3);
            boolean c = !(bit3 && Bits.test(registerFile.get(Reg.F), 4));
            FlagSrc cFlagSrc = c ? FlagSrc.V1 : FlagSrc.V0;

            combineAluFlags(0, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, cFlagSrc);
        } break;

        // Jumps
        case JP_HL: {
            postPC = reg16(Reg16.HL);
        } break;
        case JP_N16: {
            postPC = read16AfterOpcode();
        } break;
        case JP_CC_N16: {
            if (getCondition(opcode)) {
                postPC = read16AfterOpcode();
                nextNonIdleCycle += opcode.additionalCycles;
            }
        } break;
        case JR_E8: {
            postPC += Bits.clip(16, Bits.signExtend8(read8AfterOpcode()));
        } break;
        case JR_CC_E8: {
            if(getCondition(opcode)) {
                postPC += Bits.clip(16, Bits.signExtend8(read8AfterOpcode()));
                nextNonIdleCycle += opcode.additionalCycles;
            }
        } break;

        // Calls and returns
        case CALL_N16: {
            push16(postPC);
            postPC = read16AfterOpcode();
        } break;
        case CALL_CC_N16: {
            if(getCondition(opcode)) {
                push16(postPC);
                postPC = read16AfterOpcode();
                nextNonIdleCycle += opcode.additionalCycles;
            }
        } break;
        case RST_U3: {
            int n = Bits.extract(opcode.encoding, 3, 3);
            push16(postPC);
            postPC = AddressMap.RESETS[n];
        } break;
        case RET: {
            postPC = pop16();
        } break;
        case RET_CC: {
            if(getCondition(opcode)) {
                postPC = pop16();
                nextNonIdleCycle += opcode.additionalCycles;
            }
        } break;

        // Interrupts
        case EDI: {
            regIME =  Bits.test(opcode.encoding, 3);
        } break;
        case RETI: {
            regIME = true;
            postPC = pop16();
        } break;

        // Misc control
        case HALT: {
            nextNonIdleCycle = Long.MAX_VALUE;
        } break;
        case STOP: {
            throw new Error("STOP is not implemented");
        }
        }
        regPC = Bits.clip(16, postPC);
    }

    /**
     * Constructs a table containing all of the opcodes and their associated
     * commands.
     * @param kind the type of Opcode that will be in the table
     * @return table containing either the direct Opcodes the prefixed Opcodes.
     */
    private static Opcode[] buildOpcodeTable(Opcode.Kind kind) {
        Opcode[] opcodes = new Opcode[256];
        for (Opcode o: Opcode.values()) {
            if(o.kind == kind) {
                opcodes[o.encoding] = o;
            }
        }
        return opcodes;
    }

    /**
     * Reads 8bit value from the bus at the given address
     * @param address where we want to read the byte
     * @return 8bit value
     */
    private int read8(int address) {
        int value = aBus.read(address);
        Preconditions.checkBits8(value);

        return value;
    }

    /**
     * Reads 8bit value from the bus in the address contained in the
     * register pair HL
     * @return 8bit value
     */
    private int read8AtHl() {
        int index = reg16(Reg16.HL);

        return read8(index);
    }

    /**
     * Reads 8bit value from the bus at the address after the program counter,
     * i.e. address PC+1
     * @return 8bit value
     */
    private int read8AfterOpcode() {
        return read8(regPC + 1);
    }

    /**
     * Reads 16bit value from the bus at the given address
     * @param address where we want to read the 16bit value
     * @return 16bit value
     */
    private int read16(int address) {
        int value1 = Preconditions.checkBits8(read8(address + 1));
        int value2 = Preconditions.checkBits8(read8(address));

        return (value1 << 8) | value2;
    }

    /**
     * Reads 16bit value from the bus at the address after the program counter,
     * i.e. PC+1
     * @return 16bit value
     */
    private int read16AfterOpcode() {
        return read16(regPC + 1);
    }

    /**
     * Writes the value v at the given address in the bus
     * @param address to write at
     * @param v 8bit value to be written at address
     */
    private void write8(int address, int v) {
        this.aBus.write(address, v);
    }

    /**
     * Writes the given 16bit value v at the given address in the Bus.
     * @param address to write at
     * @param v 16bit value to be written at address
     */
    private void write16(int address, int v) {
        write8(address, Bits.clip(8, v));
        write8(address + 1, Bits.extract(v, 8, 8));
    }

    /**
     * Writes the given value at the register pair HL in the bus
     * @param v 8bit value to write at register pair HL
     */
    private void write8AtHl(int v) {
        int index = reg16(Reg16.HL);
        write8(index, v);
    }

    /**
     * Removes 2 from the register SP, then writes the given 16bit value v
     * in the bus at the address regSP.
     * @param v a 16bit value
     */
    private void push16(int v) {
        regSP -= 2;
        regSP = Bits.clip(16, regSP);
        write16(regSP, v);
    }

    /**
     * Reads the value found at regSP in the Bus and then increments regSP by 2.
     * @return the value found int the bus.
     */
    private int pop16() {
        int value = read16(regSP);
        regSP += 2;
        regSP = clip(16, regSP);

        return value;
    }

    /**
     * Reads the value found in the given Register pair.
     * @param r, the 16bit register pair
     * @return the value extracted from said register pair
     * @throws IllegalArgumentException when you pass a non-implemented reg16
     * to it
     */
    private int reg16(Reg16 r) {
        return make16(registerFile.get(r.highReg), registerFile.get(r.lowReg));
    }

    /**
     * Modifies the value of the given register pair, and sets the 4 LSBs to 0
     * if the given pair is AF
     * @param r the register pair
     * @param newV the new 16bit value
     */
    private void setReg16(Reg16 r, int newV) {
        Preconditions.checkBits16(newV);
        //int LSB = Bits.clip(8, newV);
        int MSB = Bits.extract(newV, 8, 8);
        int masked = r.lowReg == Reg.F ?
                Alu.maskZNHC(true, true, true,true) : 0b11111111;

        registerFile.set(r.highReg, MSB);
        registerFile.set(r.lowReg, newV & masked);
    }

    /**
     * Modifies the value of the given register pair, but modifies SP instead
     * if the given pair is AF
     * @param r the register pair
     * @param newV the new 16bit value
     */
    private void setReg16SP(Reg16 r, int newV) {
        Preconditions.checkBits16(newV);
        switch (r){
        case AF:
            regSP = newV;
            break;
        default:
            setReg16(r, newV);
        }
    }

    /**
     * Extracts and returns the identity of an 8bit registry from a given
     * opcode, at the given startBit
     * @param opcode from which we get the registry identity
     * @param startBit from where to start looking
     * @return the registry identity
     * @throws IllegalArgumentException if the encoded registry isn't valid
     * @throws IndexOutOfBoundsException if the index is bigger than 5 (need to
     * get a 3bit value after it, out of an 8bit value)
     */
    private Reg extractReg(Opcode opcode, int startBit) {

        Objects.checkIndex(startBit, 6);

        switch(Bits.extract(opcode.encoding, startBit, 3)){
        case 0b111:{
            return Reg.A;
        }
        case 0b000:{
            return Reg.B;
        }
        case 0b001:{
            return Reg.C;
        }
        case 0b010:{
            return Reg.D;
        }
        case 0b011:{
            return Reg.E;
        }
        case 0b100: {
            return Reg.H;
        }
        case 0b101:{
            return Reg.L;
        }
        default:{
            throw new IllegalArgumentException();
        }
        }
    }

    /**
     * Extracts and returns the identity of a 16bit registry from a given
     * opcode
     * @param opcode from which we get the registry identity
     * @return the registry identity
     */
    private Reg16 extractReg16(Opcode opcode) {
        return regs[Bits.extract(opcode.encoding, 4, 2)];
    }

    /**
     * Returns the value stored in the 16bit register given by the opcode,
     * where we get SP when we would get AF.
     * @param opcode from which we get the 16bit register
     * @return the value of the 16bit register (getting SP instead of AF)
     */
    private int extractReg16SPValue(Opcode opcode) {
        Reg16 r = extractReg16(opcode);
        if (r == Reg16.AF) {
            return regSP;
        } else {
            return reg16(r);
        }
    }

    /**
     * Used to encode the incrementation or decrementation of HL.
     * @param opcode the command given via an opcode.
     * @return 1 or -1 depending on which command is given .
     */
    private int extractHlIncrement(Opcode opcode) {
        return Bits.test(opcode.encoding, 4) ? -1 : 1;
    }

    /**
     * Used to obtain the Rotation Direction found in the given opcode.
     * @param opcode the command given via an opcode
     * @return the corresponding Rotation Direction
     */
    private static RotDir extractRotDir(Opcode opcode) {
        boolean d = Bits.test(opcode.encoding, 3);

        return d ? RotDir.RIGHT : RotDir.LEFT;
    }

    /**
     * Used to obtain the index at which we must test a value.
     * @param opcode containing the index
     * @return the 3 bit value representing the test index
     */
    private static int extractTestIndex(Opcode opcode) {
        return Bits.extract(opcode.encoding, 3, 3);
    }

    /**
     * Used to obtain the value used in RES and SET
     * @param opcode containing the value
     * @return true if the bit = 1, false otherwise
     */
    private static boolean extractModifier(Opcode opcode) {
        return Bits.test(opcode.encoding, 6);
    }

    /**
     * Tests the condition stored in the given opcode
     * @param opcode, the given opcode containing the condition
     * @return true of the condition is true, false otherwise.
     */
    private boolean getCondition(Opcode opcode) {
        boolean z = Bits.test(registerFile.get(Reg.F), 7);
        boolean c = Bits.test(registerFile.get(Reg.F), 4);

        boolean[] conditions = new boolean[] { !z, z, !c, c };

        return conditions[Bits.extract(opcode.encoding, 3, 2)];
    }

    /**
     * Gets value from a value/flags package and puts it in the given register
     * @param r register
     * @param vf value/flags package
     */
    private void setRegFromAlu(Reg r, int vf){
        registerFile.set(r, Alu.unpackValue(vf));
    }

    /**
     * Gets flags from package and puts them in the register F
     * @param valueFlags value/flags package
     */
    private void setFlags(int valueFlags){
        registerFile.set(Reg.F, Alu.unpackFlags(valueFlags));
    }

    /**
     * Gets value and flags from a value/flags package and puts the value in
     * the given register and flags in the register F
     * @param r given register
     * @param vf value/flags package
     */
    private void setRegFlags(Reg r, int vf){
        setRegFromAlu(r, vf);
        setFlags(vf);
    }

    /**
     * Extracts 8bit value from the given value/flags package, and writes it
     * on the bus at the address in the register pair HL, and extracts the
     * flags from the value/flags package and puts them in the register F
     * @param vf value/flags package to be written (in address from HL and in F)
     */
    private void write8AtHlAndSetFlags(int vf){
        setFlags(vf);
        write8AtHl(Alu.unpackValue(vf));
    }

    /**
     * Writes the value given in the package and sets the flags depending on
     * the given parameters.
     * @param vf value/flags package
     * @param z FlagSrc for the Z flag
     * @param n FlagSrc for the N flag
     * @param h FlagSrc for the H flag
     * @param c FlagSrc for the C flag
     */
    private void write8AtHlAndCombineAluFlags(int vf, FlagSrc z, FlagSrc n,
            FlagSrc h, FlagSrc c) {
        combineAluFlags(vf, z, n, h, c);
        write8AtHl(Alu.unpackValue(vf));
    }

    /**
     * Combine the flags contained in the register F with those in the v/f
     * package, depending on the four FlagSrc given, and puts the result in the
     * register F
     * @param vf value/flags package
     * @param z FlagSrc for the Z flag
     * @param n FlagSrc for the N flag
     * @param h FlagSrc for the H flag
     * @param c FlagSrc for the C flag
     */
    private void combineAluFlags(int vf, FlagSrc z, FlagSrc n,
            FlagSrc h, FlagSrc c){

        int maskALU = maskZNHC(z == FlagSrc.ALU, n == FlagSrc.ALU,
                h == FlagSrc.ALU, c == FlagSrc.ALU);

        int maskCPU = maskZNHC(z == FlagSrc.CPU, n == FlagSrc.CPU,
                h == FlagSrc.CPU, c == FlagSrc.CPU);

        int maskV1  = maskZNHC(z == FlagSrc.V1, n == FlagSrc.V1,
                h == FlagSrc.V1, c == FlagSrc.V1);

        registerFile.set(Reg.F, (unpackFlags(vf) & maskALU) |
                (registerFile.get(Reg.F) & maskCPU) | maskV1 );
    }

    /**
     * Enum to know what to do for a given flag:
     *      V0: force the flag to 0
     *      V1: force the flag to 1
     *      ALU: use the flag given by the ALU (in the v/f package)
     *      CPU: use the flags from the processor (in the register F)
     */
    private enum FlagSrc {
        V0, V1, ALU, CPU
    }
}