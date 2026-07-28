package org.ideaflow.core;

import java.util.Arrays;
import java.util.List;

/** Canonical SHADE success-history memory update, independent of KNIME row storage. */
public final class ShadeMemory {
    public record State(double[] f, double[] cr, int index) {
        public State {
            if (f == null || cr == null || f.length == 0 || f.length != cr.length) {
                throw new IllegalArgumentException("SHADE memory arrays must have the same positive length.");
            }
            f = f.clone();
            cr = cr.clone();
            index = Math.floorMod(index, f.length);
        }

        @Override public double[] f() { return f.clone(); }
        @Override public double[] cr() { return cr.clone(); }
    }

    public record Success(double f, double cr, double improvement) {
        public Success {
            if (!Double.isFinite(f) || f <= 0.0 || !Double.isFinite(cr)
                    || cr < 0.0 || cr > 1.0 || !Double.isFinite(improvement)
                    || improvement <= 0.0) {
                throw new IllegalArgumentException("Invalid successful SHADE parameters.");
            }
        }
    }

    private ShadeMemory() { }

    public static State initial(final int size, final double initialF, final double initialCr) {
        if (size < 1) throw new IllegalArgumentException("SHADE memory size must be positive.");
        final double[] f = new double[size];
        final double[] cr = new double[size];
        Arrays.fill(f, initialF);
        Arrays.fill(cr, initialCr);
        return new State(f, cr, 0);
    }

    public static State update(final State current, final List<Success> successes) {
        if (successes == null || successes.isEmpty()) return current;
        double improvementSum = 0.0;
        double fNumerator = 0.0;
        double fDenominator = 0.0;
        double crNumerator = 0.0;
        for (Success success : successes) {
            final double weight = success.improvement();
            improvementSum += weight;
            fNumerator += weight * success.f() * success.f();
            fDenominator += weight * success.f();
            crNumerator += weight * success.cr();
        }
        if (!(improvementSum > 0.0) || !(fDenominator > 0.0)) return current;
        final double[] f = current.f();
        final double[] cr = current.cr();
        f[current.index()] = fNumerator / fDenominator;
        cr[current.index()] = crNumerator / improvementSum;
        return new State(f, cr, (current.index() + 1) % f.length);
    }
}
