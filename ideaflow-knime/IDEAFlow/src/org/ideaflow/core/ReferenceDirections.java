package org.ideaflow.core;

import java.util.ArrayList;
import java.util.List;

/** Das-Dennis simplex-lattice reference directions used by NSGA-III-style niching. */
public final class ReferenceDirections {
    private ReferenceDirections() { }

    public static List<double[]> dasDennis(final int objectives, final int divisions) {
        if (objectives < 2 || divisions < 1) {
            throw new IllegalArgumentException("At least two objectives and one division are required.");
        }
        final List<double[]> result = new ArrayList<>();
        generate(result, new int[objectives], 0, divisions, divisions);
        return result.stream().map(values -> values.clone()).toList();
    }

    private static void generate(final List<double[]> result, final int[] current, final int index,
            final int remaining, final int divisions) {
        if (index == current.length - 1) {
            current[index] = remaining;
            final double[] direction = new double[current.length];
            for (int i = 0; i < current.length; i++) direction[i] = (double) current[i] / divisions;
            result.add(direction);
            return;
        }
        for (int value = 0; value <= remaining; value++) {
            current[index] = value;
            generate(result, current, index + 1, remaining - value, divisions);
        }
    }
}
