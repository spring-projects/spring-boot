/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.usage;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;

/**
 * Central service to build and (optionally) cache the usage report.
 */
/**
 * Experimental API – subject to change. Service that assembles and optionally caches
 * the structured usage report exposed via the actuator endpoint.
 */
public class UsageReportService {

    private final UsageReportProperties properties;

    private final UsageReportGenerator generator = new UsageReportGenerator();

    private final ObjectProvider<UsageReportCustomizer> customizers;
    private final ObjectProvider<UsagePolicy> policies;

    private volatile Cached cached;

    public UsageReportService(UsageReportProperties properties) {
        this(properties, null, null);
    }

    public UsageReportService(UsageReportProperties properties, ObjectProvider<UsageReportCustomizer> customizers,
            ObjectProvider<UsagePolicy> policies) {
        this.properties = properties;
        this.customizers = customizers;
        this.policies = policies;
    }

    public synchronized Map<String, Object> currentStructuredReport(ConditionEvaluationReport report, Map<String, String> beanOrigins, boolean force) {
        Duration ttl = this.properties.getCacheTtl();
        Instant now = Instant.now();
        if (!force && ttl != null && !ttl.isZero()) {
            Cached c = this.cached;
            if (c != null && now.isBefore(c.expiresAt) && c.report != null) {
                return c.report;
            }
        }
        UsageReportGenerator.UsageReport usage = this.generator.build(report, beanOrigins, this.properties);
        Map<String, Object> structured = this.generator.toStructuredMap(usage, this.properties.isIncludeSkippedDetails(), this.properties.isFailOnUnused());
        if (this.properties.isIncludeOrigins()) {
            enrichCoordinatesAndSuggestions(structured, beanOrigins);
        } else {
            structured.remove("beanOrigins");
            @SuppressWarnings("unchecked") Map<String,Object> suggestions = (Map<String,Object>) structured.get("suggestions");
            if (suggestions != null) {
                Object featuresObj = suggestions.get("schemaFeatures");
                java.util.List<String> features = new java.util.ArrayList<>();
                if (featuresObj instanceof Iterable<?> it) {
                    for (Object o : it) {
                        if (o instanceof String s && !s.equals("bean-origins") && !s.equals("coordinates")) {
                            features.add(s);
                        }
                    }
                }
                if (features.isEmpty()) {
                    features.add("starter-usage");
                    features.add("suggestions-basic");
                }
                suggestions.put("schemaFeatures", features);
            }
        }
        // Features that do not require origins
        applyConfidence(structured);
        detectUnusedJars(structured, beanOrigins);
    if (this.customizers != null) {
            this.customizers.orderedStream().forEach(c -> {
                try { c.customize(structured, this.properties); } catch (RuntimeException ignored) {}
            });
        }
    evaluatePolicies(structured);
        if (ttl != null && !ttl.isZero()) {
            this.cached = new Cached(structured, now.plus(ttl));
        }
        return structured;
    }

    private void enrichCoordinatesAndSuggestions(Map<String, Object> structured, Map<String, String> beanOrigins) {
        @SuppressWarnings("unchecked") Map<String, Object> origins = (Map<String, Object>) structured.get("beanOrigins");
        Map<String, String> coordCache = new HashMap<>();
        origins.forEach((bean, value) -> {
            @SuppressWarnings("unchecked") Map<String, Object> entry = (Map<String, Object>) value;
            String loc = (String) entry.get("location");
            String coord = coordinateFor(loc, coordCache);
            if (coord != null) {
                entry.put("coordinate", coord);
            }
            if (loc != null) {
                entry.put("sanitizedLocation", sanitizeLocation(loc));
            }
        });
        // Add enriched suggestions (removeDependencies with coordinates if possible)
        @SuppressWarnings("unchecked") Map<String, Object> suggestions = (Map<String, Object>) structured.get("suggestions");
        if (suggestions != null) {
            Object removeStarters = suggestions.get("removeStarters");
            if (removeStarters instanceof Iterable<?> it) {
                java.util.List<String> removeDependencies = new java.util.ArrayList<>();
                for (Object starter : it) {
                    if (starter instanceof String s) {
                        // Heuristic: transform org.springframework.boot:spring-boot-starter-foo
                        // to same coordinate unless bean origins surfaced a more specific coordinate.
                        removeDependencies.add(s);
                    }
                }
                suggestions.put("removeDependencies", removeDependencies);
            }
            suggestions.putIfAbsent("schemaFeatures", java.util.List.of("starter-usage","bean-origins","suggestions-basic","coordinates"));
        }
    }

    private void applyConfidence(Map<String,Object> structured) {
        if (!this.properties.isIncludeConfidence()) return;
        @SuppressWarnings("unchecked") Map<String,Object> suggestions = (Map<String,Object>) structured.get("suggestions");
        if (suggestions == null) return;
        java.util.Map<String,Number> confidence = new java.util.LinkedHashMap<>();
        Object removeStarters = suggestions.get("removeStarters");
        if (removeStarters instanceof java.util.Collection<?> col) {
            confidence.put("removeStarters", col.isEmpty() ? 0.0 : 0.9);
        }
        Object removeDeps = suggestions.get("removeDependencies");
        if (removeDeps instanceof java.util.Collection<?> col) {
            confidence.put("removeDependencies", col.isEmpty() ? 0.0 : 0.9);
        }
        if (!confidence.isEmpty()) {
            suggestions.put("confidence", confidence);
            @SuppressWarnings("unchecked") java.util.List<String> features = (java.util.List<String>) suggestions.get("schemaFeatures");
            if (features != null && !features.contains("suggestions-confidence")) {
                java.util.List<String> updated = new java.util.ArrayList<>(features);
                updated.add("suggestions-confidence");
                suggestions.put("schemaFeatures", updated);
            }
        }
    }

    private void detectUnusedJars(Map<String,Object> structured, Map<String,String> beanOrigins) {
        if (!this.properties.isDetectUnusedJars()) return;
        @SuppressWarnings("unchecked") Map<String,Object> suggestions = (Map<String,Object>) structured.get("suggestions");
        if (suggestions == null) return;
        java.util.Set<String> usedSanitized = new java.util.HashSet<>();
        Object originsObj = structured.get("beanOrigins");
        if (originsObj instanceof Map<?,?> originsMap) {
            for (Object v : originsMap.values()) {
                if (v instanceof Map<?,?> m) {
                    Object sanitized = m.get("sanitizedLocation");
                    if (sanitized instanceof String s) usedSanitized.add(s);
                }
            }
        }
        java.util.List<String> candidates = new java.util.ArrayList<>();
        String cp = System.getProperty("java.class.path", "");
        java.util.Set<String> excludePrefixes = java.util.Set.of("spring-boot-", "spring-core-", "spring-beans-", "jackson-", "micrometer-", "reactor-", "logback-", "slf4j-");
        for (String entry : cp.split(java.io.File.pathSeparator)) {
            if (!entry.endsWith(".jar")) continue;
            String name = entry.substring(entry.lastIndexOf(java.io.File.separatorChar) + 1);
            if (usedSanitized.contains(name)) continue;
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("test") || lower.contains("junit") || lower.contains("assertj")) continue; // skip obvious test jars
            boolean excludedByPrefix = excludePrefixes.stream().anyMatch(lower::startsWith);
            if (excludedByPrefix) continue; // Common core libs usually always needed; heuristic noise reduction
            if (candidates.size() < 20) {
                candidates.add(name);
            } else {
                break;
            }
        }
        suggestions.put("unusedJars", candidates);
        @SuppressWarnings("unchecked") java.util.List<String> features = (java.util.List<String>) suggestions.get("schemaFeatures");
        if (features != null && !features.contains("unused-jars")) {
            java.util.List<String> updated = new java.util.ArrayList<>(features);
            updated.add("unused-jars");
            suggestions.put("schemaFeatures", updated);
        }
    }

    private String sanitizeLocation(String loc) {
        // Strip local absolute paths, retain jar file name only.
        int bang = loc.indexOf('!');
        String base = (bang > 0 ? loc.substring(0, bang) : loc);
        if (base.startsWith("file:")) {
            base = base.substring(5);
        }
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0 && slash < base.length() - 1) {
            return base.substring(slash + 1);
        }
        return base;
    }

    private String coordinateFor(String location, Map<String, String> cache) {
        if (location == null) return null;
        return cache.computeIfAbsent(location, UsageReportService::resolveCoordinateFromLocation);
    }

    private static String resolveCoordinateFromLocation(String location) {
        try {
            if (!location.startsWith("file:")) {
                return null;
            }
            String path = location.substring("file:".length());
            if (path.endsWith("/")) {
                return null; // directory (likely exploded classes) – skip
            }
            java.nio.file.Path p = java.nio.file.Paths.get(java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8));
            if (!java.nio.file.Files.isRegularFile(p)) return null;
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(p.toFile())) {
                return jar.stream()
                        .filter(e -> e.getName().startsWith("META-INF/maven/") && e.getName().endsWith("/pom.properties"))
                        .findFirst()
                        .map(entry -> {
                            try {
                                java.util.Properties props = new java.util.Properties();
                                props.load(jar.getInputStream(entry));
                                String g = props.getProperty("groupId");
                                String a = props.getProperty("artifactId");
                                String v = props.getProperty("version");
                                if (g != null && a != null && v != null) {
                                    return g + ":" + a + ":" + v;
                                }
                            } catch (Exception ignored) {}
                            return null;
                        }).orElse(null);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private record Cached(Map<String, Object> report, Instant expiresAt) {
    }

    private void evaluatePolicies(Map<String,Object> structured) {
        if (this.policies == null) return;
        java.util.List<String> warnings = new java.util.ArrayList<>();
        java.util.List<String> violations = new java.util.ArrayList<>();
        this.policies.orderedStream().forEach(p -> {
            try {
                UsagePolicy.PolicyResult result = p.evaluate(structured, this.properties);
                if (result != null) {
                    if (result.warnings() != null) warnings.addAll(result.warnings());
                    if (result.violations() != null) violations.addAll(result.violations());
                }
            } catch (RuntimeException ignored) {}
        });
        if (!warnings.isEmpty() || !violations.isEmpty()) {
            java.util.Map<String,Object> policies = new java.util.LinkedHashMap<>();
            if (!warnings.isEmpty()) policies.put("warnings", warnings);
            if (!violations.isEmpty()) policies.put("violations", violations);
            structured.put("policies", policies);
            @SuppressWarnings("unchecked") Map<String,Object> suggestions = (Map<String,Object>) structured.get("suggestions");
            if (suggestions != null) {
                Object featuresObj = suggestions.get("schemaFeatures");
                java.util.List<String> features = new java.util.ArrayList<>();
                if (featuresObj instanceof Iterable<?> it) {
                    for (Object o : it) { if (o instanceof String s) features.add(s); }
                }
                if (!features.contains("policies-basic")) {
                    features.add("policies-basic");
                    suggestions.put("schemaFeatures", features);
                }
            }
        }
    }
}
