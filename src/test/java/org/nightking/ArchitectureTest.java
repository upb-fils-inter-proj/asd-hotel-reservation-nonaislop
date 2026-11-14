package org.nightking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the rules in docs/architecture.md by reading the import statements of the main sources:
 * dependencies point inward inside a service, and services only reach each other from
 * infrastructure, through the other service's controller or model.
 */
class ArchitectureTest {
    private static final Path ROOT = Path.of("src/main/java/org/nightking");
    private static final Set<String> SERVICES = Set.of("hotelmanagement", "payment", "shop");
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "entity", Set.of("entity"),
            "model", Set.of("model", "entity"),
            "gateway", Set.of("gateway", "entity"),
            "usecase", Set.of("usecase", "model", "entity", "gateway"),
            "controller", Set.of("controller", "usecase", "model"),
            "infrastructure", Set.of("infrastructure", "gateway", "entity", "model"));
    private static final Pattern IMPORT = Pattern.compile("^import (org\\.nightking\\.[\\w.*]+);", Pattern.MULTILINE);

    @Test
    void layersAndServicesOnlyDependInAllowedDirections() throws IOException {
        List<String> violations = new ArrayList<>();

        for (String service : SERVICES) {
            try (Stream<Path> files = Files.walk(ROOT.resolve(service))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String layer = ROOT.resolve(service).relativize(file).getName(0).toString();
                    if (!ALLOWED.containsKey(layer)) {
                        violations.add(file + " is in unknown layer '" + layer + "'");
                        continue;
                    }
                    Matcher imports = IMPORT.matcher(Files.readString(file));
                    while (imports.find()) {
                        String[] target = imports.group(1).split("\\.");   // org, nightking, service, layer, ...
                        String targetService = target[2];
                        String targetLayer = target.length > 3 ? target[3] : "";
                        boolean ok;
                        if (targetService.equals(service)) {
                            ok = ALLOWED.get(layer).contains(targetLayer);
                        } else if (SERVICES.contains(targetService)) {
                            ok = layer.equals("infrastructure")
                                    && (targetLayer.equals("controller") || targetLayer.equals("model"));
                        } else {
                            ok = false;
                        }
                        if (!ok) violations.add(file + " (" + layer + ") imports " + imports.group(1));
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), "Dependency rule violations:\n" + String.join("\n", violations));
    }

    @Test
    void rootPackageHoldsOnlyMainAndTheServices() throws IOException {
        try (Stream<Path> entries = Files.list(ROOT)) {
            Set<String> names = Set.copyOf(entries.map(p -> p.getFileName().toString()).toList());
            assertEquals(Set.of("Main.java", "hotelmanagement", "payment", "shop"), names);
        }
    }
}
