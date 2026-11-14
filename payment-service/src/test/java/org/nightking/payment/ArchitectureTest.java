package org.nightking.payment;

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

/** Enforces the clean-architecture rules from docs/architecture.md on this service's sources. */
class ArchitectureTest {
    private static final String SERVICE = "payment";
    private static final Path ROOT = Path.of("src/main/java/org/nightking", SERVICE);

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "entity", Set.of("entity"),
            "model", Set.of("model", "entity"),
            "gateway", Set.of("gateway", "entity"),
            "usecase", Set.of("usecase", "model", "entity", "gateway"),
            "presenter", Set.of("presenter", "usecase", "model"),
            "controller", Set.of("controller", "usecase", "model", "presenter"),
            "infrastructure", Set.of("infrastructure", "gateway", "entity", "model"));
    /** The application class and config/ are the composition root and may use every layer. */
    private static final String COMPOSITION_ROOT = "config";
    private static final Set<String> FRAMEWORK_FREE = Set.of("entity", "model", "gateway", "usecase");
    private static final Pattern IMPORT = Pattern.compile("^import (?:static )?([\\w.]+?)(?:\\.\\*)?;", Pattern.MULTILINE);

    record Source(Path file, String layer, List<String> imports) {}

    private static List<Source> sources() throws IOException {
        List<Source> sources = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Path relative = ROOT.relativize(file);
                String layer = relative.getNameCount() == 1 ? COMPOSITION_ROOT : relative.getName(0).toString();
                List<String> imports = new ArrayList<>();
                Matcher matcher = IMPORT.matcher(Files.readString(file));
                while (matcher.find()) imports.add(matcher.group(1));
                sources.add(new Source(file, layer, imports));
            }
        }
        return sources;
    }

    @Test
    void layersOnlyDependInward() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Source source : sources()) {
            if (source.layer().equals(COMPOSITION_ROOT)) continue;
            Set<String> allowed = ALLOWED.get(source.layer());
            if (allowed == null) {
                violations.add(source.file() + " is in unknown layer '" + source.layer() + "'");
                continue;
            }
            String prefix = "org.nightking." + SERVICE + ".";
            for (String imported : source.imports()) {
                if (!imported.startsWith(prefix)) continue;
                String targetLayer = imported.substring(prefix.length()).split("\\.")[0];
                if (!allowed.contains(targetLayer)) {
                    violations.add(source.file() + " (" + source.layer() + ") imports " + imported);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Layer violations:\n" + String.join("\n", violations));
    }

    @Test
    void doesNotImportOtherServices() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Source source : sources()) {
            for (String imported : source.imports()) {
                if (imported.startsWith("org.nightking.") && !imported.startsWith("org.nightking." + SERVICE + ".")) {
                    violations.add(source.file() + " imports " + imported);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Cross-service imports:\n" + String.join("\n", violations));
    }

    @Test
    void coreLayersDoNotDependOnFrameworks() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Source source : sources()) {
            if (!FRAMEWORK_FREE.contains(source.layer())) continue;
            for (String imported : source.imports()) {
                if (imported.startsWith("org.springframework") || imported.startsWith("jakarta.")
                        || imported.startsWith("com.fasterxml")) {
                    violations.add(source.file() + " (" + source.layer() + ") imports " + imported);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Framework imports in core layers:\n" + String.join("\n", violations));
    }
}
