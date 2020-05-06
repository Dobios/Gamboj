package ch.epfl.gameboj.bits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class S7BitsVectorTest {

    @Test
    void constructorFailsForInvalidSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new BitVector(-1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new BitVector(30, false));
        assertThrows(IllegalArgumentException.class,
                () -> new BitVector(-231, true));
    }

    @Test
    void constructorWorksOnNonTrivialBitVector() {
        int[] vector1 = new int[] { 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
        BitVector v1 = new BitVector(96, true);
        assertArrayEquals(vector1, v1.getData());

        int[] vector2 = new int[] {0x0, 0x0};
        BitVector v2 = new BitVector(64);
        assertArrayEquals(vector2, v2.getData());
    }

    @Test
    void sizeWorksOnNonTrivialBitVector() {
        BitVector v1 = new BitVector(128, false);
        assertEquals(128, v1.size());
        BitVector v2 = new BitVector(96, true);
        assertEquals(96, v2.size());
        BitVector v3 = new BitVector(32);
        assertEquals(32, v3.size());
    }

    @Test
    void setByteWorksOnNonTrivialBitVector() {
        BitVector.Builder v1 = new BitVector.Builder(32);
        v1.setByte(0, 0b1111_0000);
        v1.setByte(1, 0b1010_1010);
        v1.setByte(3, 0b1100_1100);
        BitVector v = v1.build();
        System.out.println(v.toString());
        int[] vector1 = new int[] { 0b1100_1100_0000_0000_1010_1010_1111_0000 };
        assertArrayEquals(vector1, v.getData());


        BitVector.Builder v2 = new BitVector.Builder(64);
        v2.setByte(0, 0b1101_0011);
        v2.setByte(2, 0b0110_1110);
        v2.setByte(3, 0b0100_1111);
        v2.setByte(5, 0b1010_1010);
        v2.setByte(6, 0b0001_0111);
        v2.setByte(7, 0b1100_0011);
        BitVector v3 = v2.build();
        int[] vector2 = new int[] { 0b0100_1111_0110_1110_0000_0000_1101_0011,
                0b1100_0011_0001_0111_1010_1010_0000_0000 };
        assertArrayEquals(vector2, v3.getData());
    }

    @Test
    void testBitWorksOnNonTrivialBitVector() {
        BitVector.Builder v = new BitVector.Builder(64);
        v.setByte(1, 0b010_0000);
        v.setByte(4, 0b1101_1010);
        BitVector v1 = v.build();
        assertTrue(v1.testBit(13));
        assertFalse(v1.testBit(4));
        assertTrue(v1.testBit(33));
        assertFalse(v1.testBit(37));
    }

    @Test
    void testBitFailsForInvalidIndex() {
        BitVector v = new BitVector(64);
        assertThrows(IllegalArgumentException.class, () -> v.testBit(64));
        assertThrows(IllegalArgumentException.class, () -> v.testBit(-5));
        assertThrows(IllegalArgumentException.class, () -> v.testBit(78));
        assertThrows(IllegalArgumentException.class, () -> v.testBit(-32));
    }

    @Test
    void extractZeroExtendedWorks() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(-17, 32);
        int[] vector1 = new int[] { 0b1111_1111_1111_1110_0000_0000_0000_0000 };
        assertArrayEquals(vector1, v2.getData());

        BitVector v3 = new BitVector(64, true);
        BitVector v4 = v3.extractZeroExtended(11, 64);
        int[] vector2 = new int[] { 0b1111_1111_1111_1111_1111_1111_1111_1111,
                0b0000_0000_0001_1111_1111_1111_1111_1111 };
        assertArrayEquals(vector2, v4.getData());
    }

    @Test
    void extractZeroExtendedWorksWithIndexMultipleOf32() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(32, 32);
        int[] vector1 = new int[] { 0b0 };
        assertArrayEquals(vector1, v2.getData());

        BitVector v3 = new BitVector(96, true);
        BitVector v4 = v3.extractZeroExtended(-64, 96);
        int[] vector2 = new int[] { 0b0, 0b0, 0b1111_1111_1111_1111_1111_1111_1111_1111 };
        assertArrayEquals(vector2, v4.getData());
    }

    @Test
    void extractWrappedWorks() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractZeroExtended(-17, 32);
        BitVector v3 = v2.extractWrapped(-6, 32);
        int[] vector1 = new int[] { 0b1111_1111_1000_0000_0000_0000_0011_1111 };
        assertArrayEquals(vector1, v3.getData());

        BitVector v4 = new BitVector(128, true);
        BitVector v5 = v4.extractZeroExtended(93, 96);
        BitVector v6 = v5.extractWrapped(33, 64);
        int[] vector2 = new int[] { 0b0000_0000_0000_0000_0000_0000_0000_0011,
                0b1000_0000_0000_0000_0000_0000_0000_0000 };
        assertArrayEquals(vector2, v6.getData());
    }

    @Test
    void extractWrappedWorksWithIndexMultipleOf32() {
        BitVector v1 = new BitVector(32, true);
        BitVector v2 = v1.extractWrapped(-32, 32);
        int[] vector1 = new int[] { 0b1111_1111_1111_1111_1111_1111_1111_1111 };
        assertArrayEquals(vector1, v2.getData());

        BitVector v3 = new BitVector(96, true);
        BitVector v4 = v3.extractWrapped(64, 64);
        int[] vector2 = new int[] { 0b1111_1111_1111_1111_1111_1111_1111_1111,
                0b1111_1111_1111_1111_1111_1111_1111_1111 };
        assertArrayEquals(vector2, v4.getData());
    }

    @Test void shiftLeftWorks() {
        BitVector.Builder v = new BitVector.Builder(32);
        v.setByte(0, 0b1111_0000);
        v.setByte(1, 0b1010_1010);
        v.setByte(3, 0b1100_1100);
        BitVector v1 = v.build();
        int[] vector = new int[] { 0b10000000000101010101111000000000 };
        assertArrayEquals(vector, (v1.shift(5)).getData());
    }

    @Test void shiftRightWorks() {
        BitVector.Builder v = new BitVector.Builder(64);
        v.setByte(0, 0b1111_0000);
        v.setByte(1, 0b1010_1010);
        v.setByte(3, 0b1100_1100);
        BitVector v1 = v.build();
        int[] vector = new int[] { 0b1100_1100_0000_0000_1010_1010_1111, 0b0 };
        assertArrayEquals(vector, (v1.shift(-4)).getData());
    }
}
