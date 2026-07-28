package org.ideaflow.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ideaflow.api.Candidate;

/** Stateless migration operations for grouped populations. */
public final class IslandMigration {
  private IslandMigration() {}

  public static Map<String, List<Candidate>> ring(
      final Map<String, List<Candidate>> populations, final int migrantCount) {
    if (migrantCount < 1 || populations.size() < 2) {
      throw new IllegalArgumentException("Ring migration requires multiple islands and migrants.");
    }
    final List<String> ids = new ArrayList<>(populations.keySet());
    ids.sort(String::compareTo);
    final Map<String, List<Candidate>> result = new LinkedHashMap<>();
    for (String id : ids) result.put(id, new ArrayList<>(populations.get(id)));
    for (int i = 0; i < ids.size(); i++) {
      final String source = ids.get(i);
      final String destination = ids.get((i + 1) % ids.size());
      final List<Candidate> sourcePopulation = populations.get(source);
      final List<Candidate> target = result.get(destination);
      final int count = Math.min(migrantCount, Math.min(sourcePopulation.size(), target.size()));
      for (int j = 0; j < count; j++) target.set(target.size() - 1 - j, sourcePopulation.get(j));
    }
    return result.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
  }
}
