package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

import java.util.Objects;
import java.util.function.BinaryOperator;

/**
 * Class simulating a line drawn by the LcdController
 *
 * Matthieu De Beule (Sciper: 269623)
 */
public final class LcdImageLine {
    private static final int NORMAL_PALETTE = 0b11_10_01_00;
    private static final int NUMBER_OF_COLORS = 4;

    private final BitVector msb, lsb, opacity;

    /**
     * Constructs an LcdImageLine using the given BitVectors (which must all be
     * of the same size).
     * @param msb the Line's MSBs
     * @param lsb the Line's LSBs
     * @param opacity the Line's opacity vector
     */
    public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity){

        Preconditions.checkArgument(msb.size() == lsb.size()
                && msb.size() == opacity.size());

        this.msb = msb;
        this.lsb = lsb;
        this.opacity = opacity;
    }

    /**
     * Get the size of the line
     * @return the size of the LcdImageLine
     */
    public int size(){
        return msb.size();
    }

    /**
     * Get the MSBs of the line
     * @return msb
     */
    public BitVector msb(){
        return msb;
    }

    /**
     * Get the LSBs of the line
     * @return lsb
     */
    public BitVector lsb(){
        return lsb;
    }

    /**
     * Get the opacities of the line
     * @return opacity
     */
    public BitVector opacity(){
        return opacity;
    }

    /**
     * Shifts the Line by a given value
     * @param shift the given value
     * @return a shifted copy of the image
     */
    public LcdImageLine shift(int shift){

        return shift == 0 ? this :
                new LcdImageLine(msb.shift(shift), lsb.shift(shift), opacity.shift(shift));
    }

    /**
     * Performs a wrapped extraction on the image
     * @param startIndex where to start the extraction
     * @param size the length of the extraction
     * @return a copy of the line using a wrapped extraction
     */
    public LcdImageLine extractWrapped(int startIndex, int size) {

        return new LcdImageLine(msb.extractWrapped(startIndex, size),
                                lsb.extractWrapped(startIndex, size),
                                opacity.extractWrapped(startIndex, size));

    }

    /**
     * Transform the colors of the line according to a given palette mapping
     * current colors encoded in a byte, where 11_10_01_00 does not change
     * anything (since color 3, encoded 11 is mapped to 3, 2 to 2 etc)
     * @param palette byte encoding the transformation
     * @return
     */
    public LcdImageLine mapColors(int palette){
        Preconditions.checkBits8(palette);

        if (palette == NORMAL_PALETTE){
            return this;
        }

        BitVector newMSB = new BitVector(this.size());
        BitVector newLSB = new BitVector(this.size());

        BitVector mask = this.lsb().not().and(this.msb().not());

        //We iterate on the colors, if we want to set the bits corresponding to
        //some color to 1 according to the corresponding bit in the
        //palette, we do a logical disjunction with the mask
        for (int i = 0; i < NUMBER_OF_COLORS; ++i) {

            switch (i) {
            case 0: {
                //color 0, MSB 0 and LSB 0
            } break;
            case 1:{
                //color 1, MSB 0 and LSB 1
                mask = this.lsb().and(this.msb().not());
            } break;
            case 2: {
                //color 2, MSB 1 and LSB 0
                mask = this.msb().and(this.lsb().not());
            } break;
            case 3: {
                //color 3, MSB 1 and LSB 1
                mask = this.msb().and(this.lsb());
            } break;
            }
            newLSB = Bits.test(palette, i * 2) ? newLSB.or(mask) : newLSB;
            newMSB = Bits.test(palette, i * 2 + 1) ? newMSB.or(mask) : newMSB;

        }
        return new LcdImageLine(newMSB, newLSB, this.opacity());

    }

    /**
     * Combine this line with another, which will be put "above" it using the
     * given opacity vector
     * @param above :  the line we want to put above this line
     * @param opacity used to combine the lines
     * @return the combination of this line with another
     */
    public LcdImageLine below(LcdImageLine above, BitVector opacity){

        Preconditions.checkArgument(above.size() == this.size());
        BitVector aboveMSB = above.msb();
        BitVector aboveLSB = above.lsb();
        //basically a multiplexer where opacity is the selector and above and
        //this are the inputs
        return new LcdImageLine(aboveMSB.and(opacity).or(this.msb().and(opacity.not())),
                aboveLSB.and(opacity).or(this.lsb().and(opacity.not())),
                this.opacity().or(opacity));

    }

    /**
     * Combine this line with another, which will be put "above" it using the
     * opacity of the other line.
     * @param above : the line we want to put above this line
     * @return the combination of this line with another
     */
    public LcdImageLine below(LcdImageLine above){

        return below(above, above.opacity());

    }

    /**
     * Join two LcdImageLines of the same length starting from the given index
     * @param that the LcdImageLine to join this LcdImageLine with
     * @return the joined LcdImageLine
     */
    public LcdImageLine join(LcdImageLine that, int index) {

        Preconditions.checkArgument(that.size() == this.size());
        if (index < 0 || index > msb().size() * Integer.SIZE){
            throw new IndexOutOfBoundsException();
        }

        BitVector notMask = new BitVector(size(), true).shift(index);
        BitVector mask = notMask.not();

        BinaryOperator<BitVector> joinVector = (x, y) ->
                x.and(mask).or(y.and(notMask));

        return new LcdImageLine(
                joinVector.apply(this.msb(), that.msb()),
                joinVector.apply(this.lsb(), that.lsb()),
                joinVector.apply(this.opacity(), that.opacity())
        );
    }

    @Override
    public boolean equals(Object that){
        if(!(that instanceof LcdImageLine)){
            return false;
        }
        LcdImageLine thatLine = (LcdImageLine) that;
        return (thatLine.msb().equals(msb()) && thatLine.lsb().equals(lsb())
        && thatLine.opacity().equals(opacity()));
    }
    @Override
    public int hashCode(){
        return Objects.hash(msb().hashCode(), lsb().hashCode(), opacity().hashCode());
    }

    /**
     * Builder allowing the construction of a line step by step
     */
    public final static class Builder {

        private final BitVector.Builder buildMsb;
        private final BitVector.Builder buildLsb;

        /**
         * Constructs the Builder using the given size
         * @param size the given size of the final LcdImage
         */
        public Builder(int size) {
            //Preconditions.checkArgument(size % 32 == 0);

            buildMsb = new BitVector.Builder(size);
            buildLsb = new BitVector.Builder(size);
        }

        /**
         * Sets the given msb and lsb bytes and the given index
         * @param index where the bytes will be set
         * @param msb the msb of the LcdImageLine
         * @param lsb the lsb of the LcdImageLine
         * @return the modified builder
         */
        public Builder setBytes(int index, int msb, int lsb) {
            buildMsb.setByte(index, msb);
            buildLsb.setByte(index, lsb);
            return this;
        }

        /**
         * Builds the LcdImageLine
         * @return a final built version of the LcdImageLine
         */
        public LcdImageLine build() {
            BitVector msb = buildMsb.build();
            BitVector lsb = buildLsb.build();
            BitVector opacity = msb .or(lsb);

            return new LcdImageLine(msb, lsb, opacity);
        }
    }



}
