package org.springframework.boot.autoconfigure.usage;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Heuristic starter usage analyzer (experimental).
 */
class StarterUsageAnalyzer {

    private static final Pattern STARTER_PATTERN = Pattern.compile("spring-boot-starter-.*");

    private final Map<String, StarterInfo> starters = new ConcurrentHashMap<>();

    private static final Set<String> PROTECTED_STARTERS = Set.of(
            "spring-boot-starter",
            "spring-boot-starter-logging",
            "spring-boot-starter-json"
    );

    private final Map<String, String> keywordToAutoConfigToken;

    StarterUsageAnalyzer() {
        Map<String, String> map = new HashMap<>();
        map.put("web", "WebMvc");
        map.put("webflux", "WebFlux");
        map.put("actuator", "Actuator");
        map.put("data-jpa", "Jpa");
        map.put("data-jdbc", "Jdbc");
        map.put("data-redis", "Redis");
        map.put("data-mongodb", "Mongo");
        map.put("data-cassandra", "Cassandra");
        map.put("security", "SecurityAutoConfiguration");
        map.put("oauth2-client", "OAuth2Client");
        map.put("oauth2-resource-server", "OAuth2ResourceServer");
        map.put("oauth2-authorization-server", "AuthorizationServer");
        map.put("graphql", "GraphQl");
        map.put("kafka", "Kafka");
        map.put("amqp", "Rabbit");
        map.put("batch", "Batch");
        map.put("integration", "IntegrationAutoConfiguration");
        map.put("mail", "MailSender");
        map.put("thymeleaf", "Thymeleaf");
        map.put("mustache", "Mustache");
        map.put("freemarker", "FreeMarker");
        map.put("quartz", "Quartz");
        map.put("rsocket", "RSocket");
        this.keywordToAutoConfigToken = Collections.unmodifiableMap(map);
    }

    void scanClasspath() {
        String cp = System.getProperty("java.class.path", "");
        if (cp.isEmpty()) return;
        String[] entries = cp.split(java.io.File.pathSeparator);
        for (String entry : entries) {
            if (entry.endsWith(".jar")) {
                try (JarFile jar = new JarFile(entry)) {
                    JarEntry pomProps = findPomProperties(jar);
                    if (pomProps != null) {
                        Properties props = new Properties();
                        try (InputStream is = jar.getInputStream(pomProps)) { props.load(is); }
                        String artifactId = props.getProperty("artifactId");
                        if (artifactId != null && STARTER_PATTERN.matcher(artifactId).matches()
                                && !"spring-boot-starter".equals(artifactId)) {
                            String groupId = props.getProperty("groupId", "");
                            String version = props.getProperty("version", "");
                            starters.putIfAbsent(artifactId, new StarterInfo(groupId + ":" + artifactId + ":" + version));
                        }
                    }
                } catch (Exception ignored) { }
            }
        }
    }

    private JarEntry findPomProperties(JarFile jar) {
        return jar.stream().filter(e -> e.getName().startsWith("META-INF/maven/") && e.getName().endsWith("/pom.properties")).findFirst().orElse(null);
    }

    StarterUsageResult classify(List<String> appliedAutoConfigurations) {
        if (starters.isEmpty()) {
            scanClasspath();
        }
        Set<String> applied = new HashSet<>(appliedAutoConfigurations);
        starters.forEach((starter, info) -> {
            String keyword = starter.substring("spring-boot-starter-".length());
            String token = keywordToAutoConfigToken.get(keyword);
            boolean used;
            if (token != null) {
                used = applied.stream().anyMatch(ac -> ac.contains(token));
            } else {
                used = applied.stream().anyMatch(ac -> simpleName(ac).toLowerCase().contains(keyword.replace("-", "")));
            }
            info.used = used;
        });
        List<String> declared = starters.keySet().stream().sorted().collect(Collectors.toList());
        List<String> used = starters.entrySet().stream().filter(en -> en.getValue().used).map(Map.Entry::getKey).sorted().collect(Collectors.toList());
        List<String> unused = starters.entrySet().stream()
                .filter(en -> !en.getValue().used && !PROTECTED_STARTERS.contains(en.getKey()))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        return new StarterUsageResult(declared, used, unused);
    }

    private String simpleName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot > -1 ? fqcn.substring(dot + 1) : fqcn;
    }

    private static class StarterInfo {
        final String coordinate;
        boolean used;
        StarterInfo(String coordinate) { this.coordinate = coordinate; }
    }

    static class StarterUsageResult {
        final List<String> declaredStarters;
        final List<String> usedStarters;
        final List<String> unusedStarters;
        StarterUsageResult(List<String> d, List<String> u, List<String> un) {
            this.declaredStarters = d; this.usedStarters = u; this.unusedStarters = un;
        }
    }
}
