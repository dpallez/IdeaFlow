package org.ideaflow.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.ideaflow.api.AlgorithmRecipe;

/** Loads the built-in conformance recipes without coupling nodes to named algorithms. */
public final class RecipeCatalog {
    private static final String ROOT = "/org/ideaflow/recipes/";

    private RecipeCatalog() {}

    public static List<AlgorithmRecipe> builtIns() {
        try (InputStream stream = RecipeCatalog.class.getResourceAsStream(ROOT + "index.txt")) {
            if (stream == null) throw new IllegalStateException("Built-in recipe index is missing.");
            final List<AlgorithmRecipe> result = new ArrayList<>();
            for (String line : new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList()) {
                final String name = line.trim();
                if (!name.isEmpty() && !name.startsWith("#")) result.add(load(name));
            }
            return List.copyOf(result);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the built-in recipe index.", exception);
        }
    }

    public static AlgorithmRecipe load(final String name) {
        final Properties properties = new Properties();
        try (InputStream stream = RecipeCatalog.class.getResourceAsStream(ROOT + name + ".properties")) {
            if (stream == null) throw new IllegalArgumentException("Unknown recipe: " + name);
            properties.load(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read recipe: " + name, exception);
        }
        final Map<String, String> strategies = new LinkedHashMap<>();
        properties.stringPropertyNames().stream()
                .filter(key -> key.startsWith("strategy."))
                .sorted()
                .forEach(key -> strategies.put(key.substring("strategy.".length()), properties.getProperty(key)));
        return new AlgorithmRecipe(
                required(properties, "id"),
                required(properties, "version"),
                required(properties, "family"),
                csv(required(properties, "stages")),
                strategies,
                csv(properties.getProperty("requirements", "")));
    }

    private static String required(final Properties properties, final String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Recipe property is missing: " + key);
        return value.trim();
    }

    private static List<String> csv(final String value) {
        return value.isBlank() ? List.of() : java.util.Arrays.stream(value.split(","))
                .map(String::trim).filter(part -> !part.isEmpty()).toList();
    }
}
