package org.ideaflow.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.ideaflow.api.Candidate;
import org.ideaflow.api.ObjectiveDefinition;

public final class ParetoArchive {
    private ParetoArchive() { }

    public static List<Candidate> update(final List<Candidate> archive, final List<Candidate> candidates,
            final List<ObjectiveDefinition> objectives, final int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Archive capacity must be positive.");
        final List<Candidate> combined = new ArrayList<>();
        if (archive != null) combined.addAll(archive);
        if (candidates != null) combined.addAll(candidates);
        final List<Integer> first = FastNonDominatedSort.sort(combined, objectives).stream()
                .findFirst().orElse(List.of());
        final List<Candidate> nondominated = new ArrayList<>();
        for (int index : first) {
            final Candidate candidate = combined.get(index);
            final boolean duplicate = nondominated.stream().anyMatch(existing ->
                    java.util.Arrays.equals(existing.objectives(), candidate.objectives()));
            if (!duplicate) nondominated.add(candidate);
        }
        if (nondominated.size() <= capacity) return List.copyOf(nondominated);
        final List<List<Integer>> oneFront = List.of(java.util.stream.IntStream.range(0, nondominated.size())
                .boxed().toList());
        final double[] crowding = CrowdingDistance.compute(nondominated, oneFront, objectives);
        final List<Integer> order = java.util.stream.IntStream.range(0, nondominated.size()).boxed()
                .sorted(Comparator.<Integer>comparingDouble(i -> crowding[i]).reversed()
                        .thenComparing(i -> nondominated.get(i).id()))
                .toList();
        return order.stream().limit(capacity).map(nondominated::get).toList();
    }
}
