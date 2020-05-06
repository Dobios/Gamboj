package ch.epfl.gameboj.component.cpu;

import org.junit.jupiter.api.Test;

import static ch.epfl.gameboj.component.cpu.Alu.RotDir.LEFT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AluTest {
    //eR = expectedResult
    //aR = actualResult

    @Test
    void maskWorksWithNonTrivialFlags() {
        int expectedResult = 0x70;
        int actualResult = Alu.maskZNHC(false, true, true, true);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    void unpackValueWorksWithNonTrivialInput() {
        int eR = 0xFF;
        int aR = Alu.unpackValue(0xFF70);

        assertEquals(eR, aR);
    }

   /* @Test
    void unpackValueThrowsExceptionOnFailure() {
        int value = 0b100000000000000001;

        assertThrows(IllegalArgumentException.class, () ->
                Alu.unpackValue(value));
    }*/

    @Test
    void unpackFlagsWorksWithNonTrivialInput() {
        int eR = 0x70;
        int aR = Alu.unpackFlags(0xFF70);

        assertEquals(eR, aR);
    }

    @Test
    void addReturnsRightPack() {
        int eR = 0x2500;
        int aR = Alu.add(0x10, 0x15);

        assertEquals(eR, aR);
    }

    @Test
    void addWorksWithIdenticalValues() {
        int eR = 0x1020;
        int aR = Alu.add(0x08, 0x08);

        assertEquals(eR, aR);
    }

    @Test
    void addWorksWhenGivenBoolean() {
        int eR = 0x00B0;
        int aR = Alu.add(0x80, 0x7F, true);

        assertEquals(eR, aR);
    }

    @Test
    void addThrowsExceptionOnFailure() {
        int l = 0b1000000001;
        int r = 0b1000000011;

        assertThrows(IllegalArgumentException.class, () -> Alu.add(l, r));
    }

    @Test
    void subReturnsRightPack() {
        int eR = 0x9050;
        int aR = Alu.sub(0x10, 0x80);

        assertEquals(eR, aR);
    }

    @Test
    void subWorksWithIdenticalValues() {
        int eR = 0x00C0;
        int aR = Alu.sub(0x10, 0x10);

        assertEquals(eR, aR);
    }

    @Test
    void subWorksWhenGivenBoolean() {
        int eR = 0xFF70;
        int aR = Alu.sub(0x01, 0x01, true);

        assertEquals(eR, aR);
    }

    @Test
    void subThrowsExceptionOnFailure() {
        int l = 0b100000001;
        int r = 0b100000011;

        assertThrows(IllegalArgumentException.class, () -> Alu.sub(l, r));
    }

    @Test
    void bcdAdjustWorksWithNoFlags() {
        int eR = 0x7300;
        int aR = Alu.bcdAdjust(0x6D, false, false, false);

        assertEquals(eR, aR);
    }

    @Test
    void bcdAdjustWorksWithNonTrivialInput() {
        int eR = 0x0940;
        int aR = Alu.bcdAdjust(0x0F, true, true, false);

        assertEquals(eR, aR);
    }

    @Test
    void bcdAdjustThrowsExceptionOnFailure() {
        int v = 0b1000000001;

        assertThrows(IllegalArgumentException.class, () ->
                Alu.bcdAdjust(v, false, false, false));
    }

    @Test
    void andReturnsCorrectResult() {
        int eR = 0x0320;
        int aR = Alu.and(0x53, 0xA7);

        assertEquals(eR, aR);
    }

    @Test
    void andThrowsExceptionOnFailure() {
        int l = Integer.MAX_VALUE;
        int r = Integer.MAX_VALUE - 1;

        assertThrows(IllegalArgumentException.class, () -> Alu.add(l, r));
    }

    @Test
    void orReturnsCorrectResult() {
        int eR = 0xF700;
        int aR = Alu.or(0x53, 0xA7);

        assertEquals(eR, aR);
    }

    @Test
    void orThrowsExceptionOnFailure() {
        int l = Integer.MAX_VALUE;
        int r = Integer.MAX_VALUE - 1;

        assertThrows(IllegalArgumentException.class, () -> Alu.or(l, r));
    }

    @Test
    void xorReturnsCorrectResult() {
        int eR = 0xF400;
        int aR = Alu.xor(0x53, 0xA7);

        assertEquals(eR, aR);
    }

    @Test
    void xorThrowsExceptionOnFailure() {
        int l = Integer.MAX_VALUE;
        int r = Integer.MAX_VALUE - 1;

        assertThrows(IllegalArgumentException.class, () -> Alu.xor(l, r));
    }

    @Test
    void shiftLeftReturnsCorrectResult() {
        int eR = 0x0090;
        int aR = Alu.shiftLeft(0x80);

        assertEquals(eR, aR);
    }

    @Test
    void shiftLeftThrowsExceptionOnFailure() {
        int v = 0b1000000001;

        assertThrows(IllegalArgumentException.class, () -> Alu.shiftLeft(v));
    }

    @Test
    void shiftRightLReturnsCorrectResult() {
        int eR = 0x4000;
        int aR = Alu.shiftRightL(0x80);

        assertEquals(eR, aR);
    }

    @Test
    void shiftRightLThrowsExceptionOnFailure() {
        int v = 0b1000000001;

        assertThrows(IllegalArgumentException.class, () -> Alu.shiftRightL(v));
    }

    @Test
    void shiftRightAReturnsCorrectResult() {
        int eR = 0xC000;
        int aR = Alu.shiftRightA(0x80);

        assertEquals(eR, aR);
    }

    @Test
    void shiftRightAThrowsExceptionOnFailure() {
        int v = 0b1000000001;

        assertThrows(IllegalArgumentException.class, () -> Alu.shiftRightA(v));
    }

    @Test
    void rotateReturnsCorrectResult() {
        int eR = 0x0110;
        int aR = Alu.rotate(LEFT, 0x80);

        assertEquals(eR, aR);
    }

    @Test
    void rotateWorksWhenGivenFalse() {
        int eR = 0x0090;
        int aR = Alu.rotate(LEFT, 0x80, false);

        assertEquals(eR, aR);
    }

    @Test
    void rotateWorksWhenGivenTrue() {
        int eR = 0x0100;
        int aR = Alu.rotate(LEFT, 0x00, true);

        assertEquals(eR, aR);
    }

    @Test
    void RotateThrowsExceptionOnFailure() {
        int v = 0b1000000001;

        assertThrows(IllegalArgumentException.class, () -> Alu.rotate(LEFT, v));
    }

    @Test
    void add16LReturnsCorrectResult() {
        int eR = 0x120030;
        int aR = Alu.add16L(0x11FF, 0x0001);

        assertEquals(eR, aR);
    }

    @Test
    void add16LThrowsExceptionOnFailure() {
        int l = 0b10000000000000001;
        int r = 0b10000000000000011;

        assertThrows(IllegalArgumentException.class, () -> Alu.add16L(l, r));
    }

    @Test
    void add16HReturnsCorrectResult() {
        int eR = 0x120000;
        int aR = Alu.add16H(0x11FF, 0x0001);

        assertEquals(eR, aR);
    }

    @Test
    void add16HThrowsExceptionOnFailure() {
        int l = 0b10000000000000001;
        int r = 0b10000000000000011;

        assertThrows(IllegalArgumentException.class, () -> Alu.add16H(l, r));
    }

    @Test
    void swapReturnsCorrectValue() {
        int value = 0b00001111;
        int eR =    0b1111000000000000;
        int aR = Alu.swap(value);

        assertEquals(eR, aR);
    }

    @Test
    void swapThrowsExceptionOnFailure() {
        int value = 0b111111111;

        assertThrows(IllegalArgumentException.class, () -> Alu.swap(value));
    }

  /*  @Test
    void testBitsWorksOnNonTrivialValue() {
        int value = 0b11010100;
        int bitIndex = 5;
        int[] eRs = {
                0b0010_0000, 0b0010_0000, 0b1010_0000, 0b0010_0000,
                0b1010_0000, 0b0010_0000, 0b1010_0000, 0b1010_0000
        };
        int aRs [] = {
                Alu.testBit(value, 0), Alu.testBit(value, 1),
                Alu.testBit(value, 2), Alu.testBit(value, 3),
                Alu.testBit(value, 4), Alu.testBit(value, 5),
                Alu.testBit(value, 6), Alu.testBit(value, 7),
        };

        assertArrayEquals(eRs, aRs);
    }*/

    @Test
    void testBitsThrowsIllegalArgumentExceptionOnFailure() {
        int v = 0b1001000000;

        assertThrows(IllegalArgumentException.class, () ->
                Alu.testBit(v,6));
    }

    @Test
    void testBitsThrowsIndexOutOfBoundsExceptionOnFailure() {
        int v = 0b10010000;
        int index = 8;

        assertThrows(IndexOutOfBoundsException.class, () ->
                Alu.testBit(v, index));
    }
}
