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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.Properties;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.usage.UsagePolicy;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration that, when enabled, produces a runtime usage report describing
 * applied & skipped auto-configurations and bean origins. This is an experimental
 * first phase implementation; output format may evolve.
 */
@AutoConfiguration
@EnableConfigurationProperties(UsageReportProperties.class)
@ConditionalOnProperty(prefix = "spring.boot.usage.report", name = "enabled", havingValue = "true")
public class UsageAnalysisAutoConfiguration {

    @Bean
    static BeanOriginTrackingPostProcessor beanOriginTrackingPostProcessor() {
        return new BeanOriginTrackingPostProcessor();
    }

    @Bean
    UsageReportService usageReportService(UsageReportProperties props, ObjectProvider<UsageReportCustomizer> customizers,
            ObjectProvider<UsagePolicy> policies) {
        return new UsageReportService(props, customizers, policies);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> usageReportListener(UsageReportProperties props,
            BeanOriginTrackingPostProcessor origins, UsageReportService service) {
        return event -> {
            try {
                writeReport(event, props, origins, service);
            }
            catch (Exception ex) {
                if (props.isFailOnError()) {
                    throw new IllegalStateException("Failed to write usage report", ex);
                }
                // If policies require failure and violation occurred, rethrow regardless of failOnError
                if (props.getPolicies().isFailOnViolation()) {
                    throw new IllegalStateException("Policy violation prevented startup", ex);
                }
            }
        };
    }

    private void writeReport(ApplicationReadyEvent event, UsageReportProperties props,
            BeanOriginTrackingPostProcessor origins, UsageReportService service) throws IOException {
        Path dir = props.getOutputDir();
        Files.createDirectories(dir);
        ConditionEvaluationReport report = ConditionEvaluationReport.get(event.getApplicationContext().getBeanFactory());
    String json = buildJson(report, origins.getBeanOrigins(), props, service);
        Files.writeString(dir.resolve("usage-report.json"), json, StandardCharsets.UTF_8);
        if (props.isMarkdownSummary()) {
            Files.writeString(dir.resolve("usage-summary.md"), buildMarkdown(report, origins.getBeanOrigins()),
                    StandardCharsets.UTF_8);
        }
    }

    private String buildJson(ConditionEvaluationReport report, Map<String, String> beanOrigins, UsageReportProperties props, UsageReportService service) {
        Map<String, Object> structured = service.currentStructuredReport(report, beanOrigins, false);
        UsageReportGenerator generator = new UsageReportGenerator();
        UsageReportGenerator.UsageReport usage = generator.build(report, beanOrigins, props); // for failOnUnused check
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : structured.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('\n').append("  \"").append(escape(e.getKey())).append("\": ").append(render(e.getValue(), 1));
        }
        sb.append('\n').append('}');
    if (props.isFailOnUnused() && !usage.starterUsage().unusedStarters.isEmpty()) {
            throw new IllegalStateException("Unused starters detected: " + usage.starterUsage().unusedStarters);
        }
        // Fail on policy violations if requested
        if (props.getPolicies().isFailOnViolation()) {
            Object policies = structured.get("policies");
            if (policies instanceof Map<?,?> m) {
                Object violations = m.get("violations");
                if (violations instanceof java.util.Collection<?> col && !col.isEmpty()) {
                    throw new IllegalStateException("Policy violations detected: " + col);
                }
            }
        }
        return sb.toString();
    }

    private String render(Object value, int indent) {
        if (value == null) return "null";
        if (value instanceof String s) {
            return '"' + escape(s) + '"';
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(',');
                first = false;
                sb.append('\n').append("  ".repeat(indent + 1)).append(render(o, indent + 1));
            }
            if (!list.isEmpty()) sb.append('\n').append("  ".repeat(indent));
            return sb.append(']').toString();
        }
        if (value instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?,?> en : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('\n').append("  ".repeat(indent + 1)).append('"').append(escape(en.getKey().toString())).append("\": ").append(render(en.getValue(), indent + 1));
            }
            if (!map.isEmpty()) sb.append('\n').append("  ".repeat(indent));
            return sb.append('}').toString();
        }
        return '"' + escape(value.toString()) + '"';
    }

    private String buildMarkdown(ConditionEvaluationReport report, Map<String, String> beanOrigins) {
        StringBuilder md = new StringBuilder();
        md.append("# Spring Boot Usage Report (Experimental)\n\n");
        md.append("Generated: ").append(Instant.now()).append("\n\n");
        md.append("## Applied Auto-Configurations\n\n");
        List<String> applied = new ArrayList<>(report.getConditionAndOutcomesBySource().keySet());
        applied.sort(String::compareTo);
        for (String name : applied) {
            md.append("- ").append(name).append('\n');
        }
        md.append("\n## Bean Origins (non-Spring framework)\n\n");
        beanOrigins.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> md.append("- `").append(e.getKey()).append("` -> ")
                        .append(shorten(e.getValue())).append('\n'));
        return md.toString();
    }

    private String escape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String limit(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    private String shorten(String loc) {
        int bang = loc.indexOf('!');
        if (bang > 0) {
            return loc.substring(0, bang);
        }
        return loc;
    }
}
