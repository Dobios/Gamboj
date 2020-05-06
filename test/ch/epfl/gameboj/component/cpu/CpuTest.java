package ch.epfl.gameboj.component.cpu;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.RandomGenerator;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static ch.epfl.gameboj.component.cpu.Opcode.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static ch.epfl.gameboj.component.cpu.S4CpuTest.assertEquals;
import static ch.epfl.gameboj.component.cpu.S4CpuTest.stateAfter;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class CpuTest {
    /*private Bus connect(Cpu cpu, Ram ram) {
        RamController rc = new RamController(ram, 0);
        Bus b = new Bus();
        cpu.attachTo(b);
        rc.attachTo(b);
        return b;
    }

    private void cycleCpu(Cpu cpu, long cycles) {
        for (long c = 0; c < cycles; ++c)
            cpu.cycle(c);
    }

    @Test void nopDoesNothing() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        b.write(0, Opcode.NOP.encoding);
        cycleCpu(c, Opcode.NOP.cycles);
        //  PC SP A F B C D E H L
        assertArrayEquals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, c._testGetPcSpAFBCDEHL());
    }

    @Test void ADD_A_R8Works_noFlags_AZero() {
        for (int i = 1; i <= 0xFF; i++) {
            Cpu c = new Cpu();
            Ram r = new Ram(10);
            Bus b = connect(c, r);
            c.setAllRegs(0, 0, 0, i, 0, 0, 0, 0);
            b.write(0, Opcode.ADD_A_C.encoding);
            cycleCpu(c, Opcode.ADD_A_C.cycles);
            //System.out.println(Arrays.toString(c._testGetPcSpAFBCDEHL()));
            //System.out.println(Arrays.toString(new int[] { 1, 0, i, 0, i, 0, 0, 0, 0, 0 }));

            //  PC SP  A  F  B  C  D  E  H  L
            assertArrayEquals(new int[] { 1, 0, i, 0, 0, i, 0, 0, 0, 0 }, c._testGetPcSpAFBCDEHL());
        }
    }

    @Test void ADD_A_R8Works_basicFlags() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        c.setAllRegs(0, 0, 0, 0, 0, 0, 0, 0);
        b.write(0, Opcode.ADD_A_B.encoding);
        cycleCpu(c, Opcode.ADD_A_B.cycles);

        int[] testZ = c._testGetPcSpAFBCDEHL();

        c = new Cpu();
        r = new Ram(10);
        b = connect(c, r);
        c.setAllRegs(0b1000_0000, 0, 0b1000_0000, 0, 0, 0, 0, 0);
        b.write(0, Opcode.ADD_A_B.encoding);
        cycleCpu(c, Opcode.ADD_A_B.cycles);

        int[] testC = c._testGetPcSpAFBCDEHL();

        c = new Cpu();
        r = new Ram(10);
        b = connect(c, r);
        c.setAllRegs(0b0000_1000, 0, 0b0000_1000, 0, 0, 0, 0, 0);
        b.write(0, Opcode.ADD_A_B.encoding);
        cycleCpu(c, Opcode.ADD_A_B.cycles);

        int[] testH = c._testGetPcSpAFBCDEHL();

        assertArrayEquals(new int[][] {
                //PC SP  A  F ZNHC0000  B  C  D  E  H  L
                { 1, 0, 0, 0b10000000, 0, 0, 0, 0, 0, 0 },
                { 1, 0, 0, 0b10010000, 0b1000_0000, 0, 0, 0, 0, 0 },
                { 1, 0, 0b0001_0000, 0b00100000, 0b0000_1000, 0, 0, 0, 0, 0 }

        }, new int[][] { testZ, testC, testH });
    }

    @Disabled @Test void ROTCAWritesCorrectFlags() {
        Cpu c = new Cpu();
        Ram r = new Ram(10);
        Bus b = connect(c, r);
        c.setAllRegs(0, 0, 0, 0, 0, 0, 0, 0);
        b.write(0, Opcode.RLC_A.encoding);
        cycleCpu(c, Opcode.RLC_A.cycles);

        int[] testZ = c._testGetPcSpAFBCDEHL();

        c = new Cpu();
        r = new Ram(10);
        b = connect(c, r);
        c.setAllRegs(0b1000_0000, 0, 0b1000_0000, 0, 0, 0, 0, 0);
        b.write(0, Opcode.RL_B.encoding);
        cycleCpu(c, Opcode.RL_B.cycles);

        int[] testC = c._testGetPcSpAFBCDEHL();

        c = new Cpu();
        r = new Ram(10);
        b = connect(c, r);
        c.setAllRegs(0b0000_1000, 0, 0b0000_1000, 0, 0, 0, 0, 0);
        b.write(0, Opcode.RL_C.encoding);
        cycleCpu(c, Opcode.RL_C.cycles);

        int[] testH = c._testGetPcSpAFBCDEHL();

        assertArrayEquals(new int[][] {
                //PC SP  A  F ZNHC0000  B  C  D  E  H  L
                { 1, 0, 0, 0b1000000, 0, 0, 0, 0, 0, 0 },
                { 1, 0, 0, 0b10010000, 0b1000_0000, 0, 0, 0, 0, 0 },
                { 1, 0, 0b0001_0000, 0b00100000, 0b0000_1000, 0, 0, 0, 0, 0 }

        }, new int[][] { testZ, testC, testH });
    }

    @Test void RLCAWorks() throws IOException {
        Bus bus = new Bus();
        Ram ram = new Ram(0xFFFF);
        Cpu cpu = new Cpu();
        RamController rC = new RamController(ram, 0);
        int value = RandomGenerator.randomInt(0b11111111, 0b0);

        cpu.attachTo(bus);
        rC.attachTo(bus);
        bus.write(0, Opcode.LD_A_N8.encoding);

        System.out.println(Integer.toBinaryString(value));
        System.out.println(value);

        bus.write(1, value);
        bus.write(2, Opcode.RLCA.encoding);

        int exp = Alu.unpackValue(Alu.rotate(Alu.RotDir.LEFT, value));
        int pack = Alu.rotate(Alu.RotDir.LEFT, value);
        int F = Bits.extract(Alu.unpackFlags(pack), 4, 1);

        System.out.println(Integer.toBinaryString(exp));
        System.out.println(exp);

        cycleCpu(cpu, (long) (Opcode.RLCA.cycles + LD_A_N8.cycles));
        assertArrayEquals(
                new int[] { cpu._testGetPcSpAFBCDEHL()[0], 0, exp, F << 4, 0, 0,
                        0, 0, 0, 0 }, cpu._testGetPcSpAFBCDEHL());
    }

    @Test void FibTest() {
        byte[] fib = new byte[] { (byte) 0x31, (byte) 0xFF, (byte) 0xFF,
                (byte) 0x3E, (byte) 0x0B, (byte) 0xCD, (byte) 0x0A, (byte) 0x00,
                (byte) 0x76, (byte) 0x00, (byte) 0xFE, (byte) 0x02, (byte) 0xD8,
                (byte) 0xC5, (byte) 0x3D, (byte) 0x47, (byte) 0xCD, (byte) 0x0A,
                (byte) 0x00, (byte) 0x4F, (byte) 0x78, (byte) 0x3D, (byte) 0xCD,
                (byte) 0x0A, (byte) 0x00, (byte) 0x81, (byte) 0xC1,
                (byte) 0xC9, };

        Ram ram = new Ram(0xFFFF);
        Cpu cpu = new Cpu();
        Bus bus = connect(cpu, ram);

        for (int i = 0; i < fib.length; ++i) {
            bus.write(i, Byte.toUnsignedInt(fib[i]));
        }

        cpu.cycle(60000);
        int i = 0;
        //while PC != 8
        while (cpu._testGetPcSpAFBCDEHL()[0] != 8) {
            cpu.cycle(i);
            ++i;
        }
        cpu.cycle(i);
        assertArrayEquals(
                new int[] { 8, cpu._testGetPcSpAFBCDEHL()[1], 89, 0, 0, 0, 0, 0,
                        0, 0 }, cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void JP_CC_N16_Works() {

        Assembler asm = new Assembler();

        asm.emit(SCF);
        asm.emit(JP_C_N16, 6);
        asm.emit(ADD_A_N8, 100);
        asm.emit(ADD_A_N8, 2);
        asm.emit(ADD_A_N8, 1);

        Assertions.assertEquals(3, stateAfter(asm).a());
    }*/
}