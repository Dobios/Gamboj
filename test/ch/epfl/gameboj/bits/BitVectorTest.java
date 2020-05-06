package ch.epfl.gameboj.bits;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BitVectorTest {

    @Disabled
    @Test
    void extractZeroExtendedWorks() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(-17, 32);
        int[] vector = new int[] { 0b11111111111111100000000000000000 };
        assertArrayEquals(vector, v2.getData());

        BitVector.Builder test1 = new BitVector.Builder(Integer.SIZE * 3);
        /*test1.set32(0, 0b00111001011010101001_001111011011);
        test1.set32(1, 0b01010011101011100001_011000011101);
        test1.set32(2, 0b10010011101010000001_110100010111);*/
        BitVector test = test1.build();

        int[] exp = {0b001111011011_00000000000000000000, 0b011000011101_00111001011010101001};
        BitVector act = test.extractZeroExtended(-20, Integer.SIZE * 2);
        System.out.println(test.toString());
        System.out.println(act.toString());
        assertArrayEquals(exp, act.getData());

        int[] exp1 = {0, 0, 0, 0, 0, 0, 0, 0};
        BitVector act1 = new BitVector(Integer.SIZE * 8, false);
        assertArrayEquals(exp1, act1.extractZeroExtended(100, Integer.SIZE * 8).getData());
    }

    @Disabled
    @Test
    void extractWrappedWorks() {
        BitVector.Builder test1 = new BitVector.Builder(Integer.SIZE * 3);
        /*test1.set32(0, 0b00111001011010101001_001111011011);
        test1.set32(1, 0b01010011101011100001_011000011101);
        test1.set32(2, 0b10010011101010000001_110100010111);*/
        BitVector test = test1.build();
        BitVector v3 = test.extractWrapped(-16, 32);
        int[] vector = new int[] { 0b10010011110110111001001110101000};
        System.out.println(Integer.toBinaryString(0b10010011110110111001001110101000));
        assertArrayEquals(vector, v3.getData());

        int[] exp1 = {0b001111011011_10010011101010000001,
                      0b011000011101_00111001011010101001,
                      0b110100010111_01010011101011100001 };
        BitVector act1 = test.extractWrapped(-20, Integer.SIZE * 3);
        for(int i = 0; i< exp1.length ; i++){
            System.out.println(Integer.toBinaryString(exp1[i]));
            System.out.println(Integer.toBinaryString(act1.getData()[i]));
            System.out.println();
        }

        assertArrayEquals(exp1, act1.getData());


    }

    @Test
    void shiftWorks() {
        BitVector.Builder test1 = new BitVector.Builder(Integer.SIZE * 3);
       /* test1.set32(0, 0xAAAAAAAA);
        test1.set32(1, 0xFFFFFFFF);
        test1.set32(2, 0);*/
        BitVector test = test1.build();

        int[] exp = {0xAAA00000,0xFFFAAAAA,0x000FFFFF};
        BitVector act = test.shift(20);
        System.out.println(act.toString());

        int[] exp1 = {0xFFFFFAAA, 0x00000FFF, 0x0};
        BitVector act1 = test.shift(-20);
        System.out.println(act1.toString());

        assertArrayEquals(exp, act.getData());
        assertArrayEquals(exp1, act1.getData());
    }


    @Test
    void andWorksOnKnownValues() {
        BitVector.Builder test1 = new BitVector.Builder(Integer.SIZE * 3);
        BitVector test2 = new BitVector(Integer.SIZE * 3, true);
       /* test1.set32(0, 0xAAAAAAAA);
        test1.set32(1, 0xFFFFFFFF);
        test1.set32(2, 0);*/
        BitVector actual = test1.build().and(test2);
        int[] expected1 = {0xAAAAAAAA, 0xFFFFFFFF, 0};
        assertArrayEquals(expected1, actual.getData());

        BitVector zero = new BitVector(Integer.SIZE * 8, true);
        BitVector one = new BitVector(Integer.SIZE * 8, false);
        int[] none = {0,0,0,0,0,0,0,0};
        assertArrayEquals(none, zero.and(one).getData());

        assertThrows(IllegalArgumentException.class, () -> zero.and(test2));
    }

    @Test
    void orWorksOnKnownValues() {
        BitVector.Builder test1 = new BitVector.Builder(Integer.SIZE * 3);
        BitVector test2 = new BitVector(Integer.SIZE * 3, false);
        /*test1.set32(0, 0xAAAAAAAA);
        test1.set32(1, 0xFFFFFFFF);
        test1.set32(2, 0);*/

        BitVector zero = new BitVector(Integer.SIZE * 8, true);
        BitVector one = new BitVector(Integer.SIZE * 8, false);

        int[] expected1 = {0xAAAAAAAA, 0xFFFFFFFF, 0x0};
        assertArrayEquals(expected1, test1.build().or(test2).getData());

        int[] all = {0xFFFFFFFF,0xFFFFFFFF ,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF};
        assertArrayEquals(all, zero.or(one).getData());

        assertThrows(IllegalArgumentException.class, () -> zero.or(test2));
    }

    @Test
    void notWorksOnKnownValues() {
        BitVector.Builder test1 = new BitVector.Builder(Integer.SIZE * 3);
       /* test1.set32(0, 0b10101010101010101010101010101010);
        test1.set32(1, 0xFFFFFFFF);
        test1.set32(2, 0);*/
        BitVector test = test1.build();
        System.out.println(test.toString());
        System.out.println(test.not());
        System.out.println(Integer.toBinaryString(test.not().getData()[2]));

        BitVector zero = new BitVector(Integer.SIZE * 8, false);
        BitVector one = new BitVector(Integer.SIZE * 8, true);

        int[] expected1 = {0b01010101010101010101010101010101, 0x0, 0xFFFFFFFF};
        assertEquals(expected1[0], test.not().getData()[0]);
        assertEquals(expected1[1], test.not().getData()[1]);
        assertEquals(expected1[2], test.not().getData()[2]);

        int[] all = {0xFFFFFFFF,0xFFFFFFFF ,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF};
        assertArrayEquals(all, zero.not().getData());


        int[] none = {0,0,0,0,0,0,0,0};
        assertArrayEquals(one.not().getData(), none);
    }

    @Test
    void bitVectorBuildsWell() {
        BitVector test = new BitVector(256);
        BitVector test2 = new BitVector(256, true);
        BitVector test3 = new BitVector(0);

        int[] expected0 = {0,0,0,0,0,0,0,0};
        int[] expected1 = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
        int[] expectedNull = new int[0];

        assertArrayEquals(expected0, test.getData());
        assertArrayEquals(expected1, test2.getData());
        assertArrayEquals(expectedNull, test3.getData());
    }
}
