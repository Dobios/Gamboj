package ch.epfl.gameboj.bits;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.cpu.Alu;

import java.util.Arrays;

/**
 * Class representing a bit vector of bit size >= the size of an int.
 * This class offers generalized versions the standard bit operations.
 *
 * @author Matthieu De Beule (Sciper: 269623)
 * @author Andrew Dobis (Sciper: 272002)
 */
public final class  BitVector {
    private static final int FULL_INT = 0xFFFF_FFFF;
    private static final int NUMBER_OF_BYTES = 4;
    private static final int VECTOR_COMP_SIZE = Integer.SIZE;

    private final int[] bitVector;


    /**
     * Construct a BitVector filled with an initial value
     * @param size of the BitVector
     * @param value every bit of the BitVector will have
     */
    public BitVector(int size, boolean value) {

        Preconditions.checkArgument(size % VECTOR_COMP_SIZE == 0 && size >= 0);
        bitVector = new int[size / VECTOR_COMP_SIZE];

        if (value)
            Arrays.fill(bitVector, FULL_INT);
    }

    /**
     * Construct a BitVector initialized with all values at 0
     * @param size of the BitVector
     */
    public BitVector(int size) {
        this(size, false);
    }

    /**
     * Private constructor for BitVector allowing to construct a BitVector
     * from an integer array.
     * @param data the integer array used to construct the BitVector
     */
    private BitVector(int[] data) {
        bitVector = data;
    }

    /**
     * Getter allowing access to the values stored in bitVector
     * @return a copy of the attribute bitVector.
     */
    public int[] getData() {
        int[] data = Arrays.copyOf(bitVector, bitVector.length);
        return data;
    }

    /**
     * Getter allowing access to the number of ints stored in the BitVector
     * @return the length of the int array bitvector
     */
    public int length() {
        return bitVector.length;
    }

    /**
     * Get BitVector size
     * @return the size of the BitVector
     */
    public int size(){
        return bitVector.length * VECTOR_COMP_SIZE;
    }

    /**
     * Determine the value of the bit at the given index
     * @param index of the bit to test
     * @return value of the tested bit
     */
    public boolean testBit(int index) {
        Preconditions.checkArgument(index >= 0 && index < size());
        return Bits.test(bitVector[index / VECTOR_COMP_SIZE], index % VECTOR_COMP_SIZE);
    }

    /**
     * Calculate complement of the BitVector
     * @return complement of BitVector
     */
    public BitVector not() {

        int [] notBitVector = new int[bitVector.length];
        for(int i = 0; i < bitVector.length; i++){
            notBitVector[i] = ~bitVector[i];
        }
        return new BitVector(notBitVector);
    }

    /**
     * Logical conjunction of this BitVector and another
     * @return the conjunction of the two BitVectors
     */
    public BitVector and(BitVector that) {
        Preconditions.checkArgument( this.size() == that.size());

        int[] result = new int[bitVector.length];
        int[] thatData = that.bitVector;
        for(int i = 0; i < bitVector.length; ++i) {
            result[i] = bitVector[i] & thatData[i];
        }
        return new BitVector(result);
    }

    /**
     * Logical disjunction of this BitVector and another
     * @return the disjunction of the two BitVectors
     */
    public BitVector or(BitVector that) {
        Preconditions.checkArgument( this.size() == that.size());

        int[] result = new int[bitVector.length];
        int[] thatData = that.getData();
        for(int i = 0; i < bitVector.length; ++i) {
            result[i] = bitVector[i] | thatData[i];
        }
        return new BitVector(result);
    }

    /**
     * Extracts the 32bit element from the bitVector represented as a zero
     * extension or a wrapped extension.
     * @param index the index at which the extraction will take place.
     * @param type boolean defining the type extraction to make,
     *             if true then the extraction will be wrapped,
     *             else the extraction will be zero extended.
     * @return a 32bit element of the final extraction.
     */
    private int extractElement(int index, boolean type) {
        int firstPart = Math.floorDiv(index, VECTOR_COMP_SIZE);
        int extractStart = Math.floorMod(index, VECTOR_COMP_SIZE);

        //wrapped extraction
        if (type){
            //multiple of 32
            if(extractStart == 0){
                return bitVector[Math.floorMod(index/VECTOR_COMP_SIZE, length())];
            }
            //not a multiple of 32
            return (bitVector[Math.floorMod(firstPart + 1, bitVector.length)]
                    << VECTOR_COMP_SIZE - extractStart)
                    | Bits.extract(bitVector[Math.floorMod(firstPart, bitVector.length)],
                    extractStart, VECTOR_COMP_SIZE - extractStart);
        }


        //zero extended extraction
        if (extractStart == 0){
            if (firstPart >= 0 && firstPart < length()) {
                return bitVector[index / VECTOR_COMP_SIZE];
            } else {
                return 0;
            }
        }

        if(firstPart < -1 || firstPart >= length()) {
            return 0;
        } else if(firstPart == -1) {
            return bitVector[0] << VECTOR_COMP_SIZE - extractStart;
        } else if (firstPart + 1 < length()) {
            return (bitVector[firstPart + 1] << VECTOR_COMP_SIZE - extractStart)
                    | Bits.extract(bitVector[firstPart], extractStart,
                    VECTOR_COMP_SIZE - extractStart);
        } else if (firstPart + 1 == length()){
            return Bits.extract(bitVector[firstPart], extractStart,
                    VECTOR_COMP_SIZE - extractStart);
        }
        return 0;
    }

    /**
     * A generalized version of the extract method
     * @param startIndex index from which the extraction will take place.
     * @param size the number of bits that will be extracted
     * @param type boolean defining the type extraction to make,
     *             if true then the extraction will be wrapped,
     *             else the extraction will be zero extended
     * @return a bitVector extracted from bits.
     */
    private BitVector extract(int startIndex, int size, boolean type) {
        int length = Math.floorDiv(size, VECTOR_COMP_SIZE);
        int[] data = new int[length];
        for (int i = 0; i < length; ++i)
            data[i] = extractElement(startIndex + VECTOR_COMP_SIZE * i, type);

        return new BitVector(data);
    }

    /**
     * Extracts a vector of a given size from the 0 extension of the bitVector.
     * @param startIndex index from which the extraction will take place.
     * @param size the number of bits that will be extracted
     * @return a bitVector extracted from the 0 extension of bits.
     */
    public BitVector extractZeroExtended(int startIndex, int size) {
        Preconditions.checkArgument(size % VECTOR_COMP_SIZE == 0);
        return extract(startIndex, size, false);
    }

    /**
     * Extracts a vector of a given size from the wrapped extension of the
     * bitVector.
     * @param startIndex index from which the extraction will take place.
     * @param size the number of bits that will be extracted
     * @return a bitVector extracted from the wrapped extension of bits.
     */
    public BitVector extractWrapped(int startIndex, int size) {
        Preconditions.checkArgument(size % VECTOR_COMP_SIZE == 0);
        return extract(startIndex, size, true);
    }

    /**
     * Shifts the bits of the bitVector using the following standard:
     * - if the shift distance is positive, then it shifts towards the left
     * - if the shift distance is negative, then it shifts towards the right
     * @param shift the shift distance
     * @return a shifted copy of the bitVector bits
     */
    public BitVector shift(int shift) {
        return extractZeroExtended(-shift, size());
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int aBitVector : bitVector) {
            for (int j = 0; j < VECTOR_COMP_SIZE; ++j) {
                str.append(Bits.extract(aBitVector, j, 1));
            }
        }
        return str.reverse().toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bitVector);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof BitVector) {
            BitVector thatBv = (BitVector) that;

            return Arrays.equals(bitVector, thatBv.getData());
        }
        return false;
    }

    /**
     * Build a BitVector byte by byte with this builder
     */
    public final static class Builder {

        private int[] buildingBitVector;

        /**
         * Make builder of a given size
         * @param size of the BitVector to be built
         */
        public Builder(int size) {

            Preconditions.checkArgument(size % VECTOR_COMP_SIZE == 0
                    && size >= 0);


            buildingBitVector = new int[size/ VECTOR_COMP_SIZE];
        }

        /**
         * Set byte at given index with given value
         * @param index of the byte to be set
         * @param value of the byte to set
         * @return a modified version of the builder.
         */
        public Builder setByte(int index,  int value) {
            Preconditions.checkBits8(value);

            if (buildingBitVector == null){
                throw new IllegalStateException();
            }
            if (index > (buildingBitVector.length * NUMBER_OF_BYTES) || index < 0) {
                throw new IndexOutOfBoundsException();
            }

            buildingBitVector[index / NUMBER_OF_BYTES] =
                    (~(Alu.MAX_8BITS << (index % NUMBER_OF_BYTES) * Byte.SIZE)
                            & buildingBitVector[index / NUMBER_OF_BYTES])
                            | value << ((index % NUMBER_OF_BYTES) * Byte.SIZE);
            return this;
        }

        /**
         * Returns the BitVector that was built.
         * @return the BitVector that was built.
         */
        public BitVector build() {

            if (buildingBitVector == null){
                throw new IllegalStateException();
            }
            BitVector builtBitVector = new BitVector(buildingBitVector);
            buildingBitVector = null;

            return builtBitVector;
        }
    }
}
