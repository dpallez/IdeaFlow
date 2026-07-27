package org.ideaflow.core;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/** Success-history memory shared by SHADE and L-SHADE recipes. */
public final class ShadeMemory {
    private final double[] fMemory;
    private final double[] crMemory;
    private int index;

    public ShadeMemory(final int size, final double initialF, final double initialCr) {
        if (size < 1) throw new IllegalArgumentException("Memory size must be positive.");
        fMemory = new double[size];
        crMemory = new double[size];
        Arrays.fill(fMemory, initialF);
        Arrays.fill(crMemory, initialCr);
    }

    public Sample sample(final RandomGenerator random) {
        final int slot = random.nextInt(fMemory.length);
        double f;
        do { f = fMemory[slot] + 0.1 * Math.tan(Math.PI * (random.nextDouble() - 0.5)); }
        while (f <= 0.0);
        f = Math.min(1.0, f);
        final double cr = Math.max(0.0, Math.min(1.0, crMemory[slot] + 0.1 * gaussian(random)));
        return new Sample(f, cr, slot);
    }

    public void update(final double[] successfulF, final double[] successfulCr, final double[] improvements) {
        if (successfulF.length == 0) return;
        if (successfulF.length != successfulCr.length || successfulF.length != improvements.length) {
            throw new IllegalArgumentException("Successful parameter arrays must have equal lengths.");
        }
        double weightSum = 0.0;
        for (double improvement : improvements) weightSum += Math.max(0.0, improvement);
        double fNumerator = 0.0;
        double fDenominator = 0.0;
        double crMean = 0.0;
        for (int i = 0; i < successfulF.length; i++) {
            final double weight = weightSum == 0.0 ? 1.0 / successfulF.length
                    : Math.max(0.0, improvements[i]) / weightSum;
            fNumerator += weight * successfulF[i] * successfulF[i];
            fDenominator += weight * successfulF[i];
            crMean += weight * successfulCr[i];
        }
        if (fDenominator > 0.0) fMemory[index] = fNumerator / fDenominator;
        crMemory[index] = crMean;
        index = (index + 1) % fMemory.length;
    }

    public double[] fMemory() { return fMemory.clone(); }
    public double[] crMemory() { return crMemory.clone(); }
    public int index() { return index; }

    public record Sample(double f, double cr, int memoryIndex) { }

    private static double gaussian(final RandomGenerator random) {
        final double u1 = Math.max(Double.MIN_NORMAL, random.nextDouble());
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * random.nextDouble());
    }
}
