package org.ideaflow.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

public final class DifferentialEvolution {
    public enum Mutation { RAND_1, BEST_1, CURRENT_TO_BEST_1, CURRENT_TO_PBEST_1 }
    public enum Crossover { BINOMIAL, EXPONENTIAL }
    public enum Repair { CLAMP, REFLECT, RANDOM }

    private DifferentialEvolution() { }

    public static double[][] propose(final double[][] population, final double[] fitness,
            final Mutation mutation, final Crossover crossover, final double differentialWeight,
            final double crossoverRate, final double pBestRate, final double[] lower, final double[] upper,
            final Repair repair, final RandomGenerator random) {
        final double[] weights = new double[population.length];
        final double[] rates = new double[population.length];
        java.util.Arrays.fill(weights, differentialWeight);
        java.util.Arrays.fill(rates, crossoverRate);
        return propose(population, new double[0][], fitness, mutation, crossover, weights, rates, pBestRate, lower, upper, repair, random);
    }

    public static double[][] propose(final double[][] population, final double[] fitness,
            final Mutation mutation, final Crossover crossover, final double[] differentialWeights,
            final double[] crossoverRates, final double pBestRate, final double[] lower, final double[] upper,
            final Repair repair, final RandomGenerator random) {
        return propose(population, new double[0][], fitness, mutation, crossover, differentialWeights,
                crossoverRates, pBestRate, lower, upper, repair, random);
    }

    public static double[][] propose(final double[][] population, final double[][] archive, final double[] fitness,
            final Mutation mutation, final Crossover crossover, final double[] differentialWeights,
            final double[] crossoverRates, final double pBestRate, final double[] lower, final double[] upper,
            final Repair repair, final RandomGenerator random) {
        validate(population, fitness, differentialWeights, crossoverRates, lower, upper);
        for (double[] archived : archive) if (archived.length != population[0].length) {
            throw new IllegalArgumentException("Archive rows must match the decision dimension.");
        }
        final int best = bestIndex(fitness);
        final List<Integer> ranked = new ArrayList<>();
        for (int i = 0; i < population.length; i++) ranked.add(i);
        ranked.sort(java.util.Comparator.comparingDouble(i -> fitness[i]));
        final int pBestCount = Math.max(2, Math.min(population.length, (int) Math.ceil(pBestRate * population.length)));
        final double[][] trials = new double[population.length][];
        for (int target = 0; target < population.length; target++) {
            final double differentialWeight = differentialWeights[target];
            final double crossoverRate = crossoverRates[target];
            final List<Integer> donors = donors(population.length, target, 3, random);
            final int pBest = ranked.get(random.nextInt(pBestCount));
            final double[] secondDifferenceDonor = mutation == Mutation.CURRENT_TO_PBEST_1
                    ? archiveDonor(population, archive, target, donors.get(0), random) : null;
            final double[] mutant = new double[population[target].length];
            for (int d = 0; d < mutant.length; d++) {
                mutant[d] = switch (mutation) {
                    case RAND_1 -> population[donors.get(0)][d]
                            + differentialWeight * (population[donors.get(1)][d] - population[donors.get(2)][d]);
                    case BEST_1 -> population[best][d]
                            + differentialWeight * (population[donors.get(0)][d] - population[donors.get(1)][d]);
                    case CURRENT_TO_BEST_1 -> population[target][d]
                            + differentialWeight * (population[best][d] - population[target][d])
                            + differentialWeight * (population[donors.get(0)][d] - population[donors.get(1)][d]);
                    case CURRENT_TO_PBEST_1 -> population[target][d]
                            + differentialWeight * (population[pBest][d] - population[target][d])
                            + differentialWeight * (population[donors.get(0)][d] - secondDifferenceDonor[d]);
                };
                mutant[d] = repair(mutant[d], lower[d], upper[d], repair, random);
            }
            trials[target] = crossover(population[target], mutant, crossoverRate, crossover, random);
        }
        return trials;
    }

    private static double[] archiveDonor(final double[][] population, final double[][] archive,
            final int target, final int firstDonor, final RandomGenerator random) {
        if (archive.length == 0) {
            final List<Integer> choices = donors(population.length, target, Math.min(3, population.length - 1), random);
            for (int choice : choices) if (choice != firstDonor) return population[choice];
            return population[choices.get(0)];
        }
        final int combined = population.length + archive.length;
        while (true) {
            final int choice = random.nextInt(combined);
            if (choice < population.length && (choice == target || choice == firstDonor)) continue;
            return choice < population.length ? population[choice] : archive[choice - population.length];
        }
    }

    private static double[] crossover(final double[] target, final double[] mutant, final double rate,
            final Crossover crossover, final RandomGenerator random) {
        final double[] trial = target.clone();
        final int forced = random.nextInt(target.length);
        if (crossover == Crossover.BINOMIAL) {
            for (int d = 0; d < target.length; d++) {
                if (d == forced || random.nextDouble() <= rate) trial[d] = mutant[d];
            }
        } else {
            int length = 0;
            int index = forced;
            do {
                trial[index] = mutant[index];
                index = (index + 1) % target.length;
                length++;
            } while (length < target.length && random.nextDouble() <= rate);
        }
        return trial;
    }

    private static double repair(final double value, final double lower, final double upper,
            final Repair repair, final RandomGenerator random) {
        if (value >= lower && value <= upper) return value;
        return switch (repair) {
            case CLAMP -> Math.max(lower, Math.min(upper, value));
            case RANDOM -> random.nextDouble(lower, Math.nextUp(upper));
            case REFLECT -> {
                final double width = upper - lower;
                if (width == 0.0) yield lower;
                double offset = (value - lower) % (2.0 * width);
                if (offset < 0) offset += 2.0 * width;
                yield offset <= width ? lower + offset : upper - (offset - width);
            }
        };
    }

    private static List<Integer> donors(final int size, final int target, final int count,
            final RandomGenerator random) {
        final List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) if (i != target) indices.add(i);
        Collections.shuffle(indices, new java.util.Random(random.nextLong()));
        return indices.subList(0, count);
    }

    private static int bestIndex(final double[] fitness) {
        int best = 0;
        for (int i = 1; i < fitness.length; i++) if (fitness[i] < fitness[best]) best = i;
        return best;
    }

    private static void validate(final double[][] population, final double[] fitness, final double[] weights,
            final double[] rates, final double[] lower, final double[] upper) {
        if (population.length < 4 || population.length != fitness.length || weights.length != population.length
                || rates.length != population.length) {
            throw new IllegalArgumentException("DE requires at least four equally evaluated individuals.");
        }
        final int dimensions = population[0].length;
        if (dimensions == 0 || lower.length != dimensions || upper.length != dimensions) {
            throw new IllegalArgumentException("Bounds must match the decision dimension.");
        }
        for (int index = 0; index < weights.length; index++) if (!(weights[index] > 0.0 && Double.isFinite(weights[index]))
                || rates[index] < 0.0 || rates[index] > 1.0) throw new IllegalArgumentException("Invalid F or CR.");
        for (double[] row : population) if (row.length != dimensions) {
            throw new IllegalArgumentException("Population rows must have equal dimensions.");
        }
    }
}
