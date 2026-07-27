package org.ideaflow.core;

/** Shared naming and decoding rules for binary-encoded numerical variables. */
public final class BinaryEncoding {
    private BinaryEncoding() { }

    public static String geneName(final String logicalName, final int bit) {
        return logicalName + "_bit" + bit;
    }

    public static double decode(final long storedCode, final int bits, final double lower, final double upper) {
        if (bits < 1 || bits > 52) throw new IllegalArgumentException("Bits must be between 1 and 52.");
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower >= upper) {
            throw new IllegalArgumentException("Decoding bounds must be finite and increasing.");
        }
        final long denominator = (1L << bits) - 1L;
        if (storedCode < 0 || storedCode > denominator) throw new IllegalArgumentException("Code exceeds bit range.");
        return lower + (upper - lower) * ((double)storedCode / denominator);
    }
}
