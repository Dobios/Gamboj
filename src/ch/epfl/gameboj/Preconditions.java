package ch.epfl.gameboj;

/**
 * Interface used to impose preconditions on certain values.
 * @author Andrew Dobis (Sciper: 272002)
 * @author Matthieu De Beule (Sciper: 269623)
 */
public interface Preconditions {

    /**
     * Checks the truth value of the given argument
     * @param b argument to be checked
     */
    public static void checkArgument(boolean b){
        if (!b) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks if the parameter is within the memory bounds
     * @param v value to be checked for 8bit bounds
     * @return the parameter v
     */
    public static int checkBits8(int v) {

        checkArgument(v <= 0xFF && v >= 0);
        return v;
    }

    /**
     * Checks if the parameter is within the memory bounds
     * @param v value to be checked for 16bit bounds
     * @return the parameter v
     */
    public static int checkBits16(int v) {
        checkArgument(v <= 0xFFFF && v >= 0);
        return v;

    }
}
