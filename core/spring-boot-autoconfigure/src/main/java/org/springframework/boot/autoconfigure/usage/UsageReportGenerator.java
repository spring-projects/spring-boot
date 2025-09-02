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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;

/**
 * Builds a structured usage report map (schemaVersion 1).
 */
/**
 * Internal support class building the structured usage report map (schemaVersion=1).
 * Kept package-private; output format is experimental.
 */
class UsageReportGenerator {

    static final String SCHEMA_VERSION = "1";

    UsageReportGenerator() {
    }

    UsageReport build(ConditionEvaluationReport report, Map<String, String> beanOrigins, UsageReportProperties props) {
        List<String> applied = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        report.getConditionAndOutcomesBySource().forEach((name, outcomes) -> {
            if (outcomes.isFullMatch()) {
                applied.add(name);
            }
            else {
                skipped.add(name);
            }
        });
        applied.sort(String::compareTo);
        skipped.sort(String::compareTo);
        StarterUsageAnalyzer analyzer = new StarterUsageAnalyzer();
        StarterUsageAnalyzer.StarterUsageResult usage = analyzer.classify(applied);
        return new UsageReport(applied, skipped, usage, beanOrigins);
    }

    record UsageReport(List<String> applied, List<String> skipped, StarterUsageAnalyzer.StarterUsageResult starterUsage,
            Map<String, String> beanOrigins) {
    }

    Map<String, Object> toStructuredMap(UsageReport r, boolean includeSkippedDetails, boolean failOnUnused) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("timestamp", Instant.now().toString());
        root.put("appliedAutoConfigurations", r.applied());
        if (includeSkippedDetails) {
            // For now just provide the names list; detailed reasons already in file builder
            root.put("skippedAutoConfigurations", r.skipped());
        }
        root.put("declaredStarters", r.starterUsage().declaredStarters);
        root.put("usedStarters", r.starterUsage().usedStarters);
        root.put("unusedStarters", r.starterUsage().unusedStarters);
        if (failOnUnused && !r.starterUsage().unusedStarters.isEmpty()) {
            root.put("unusedStarterFailure", Boolean.TRUE);
        }
        Map<String, Object> suggestions = new LinkedHashMap<>();
        suggestions.put("removeStarters", r.starterUsage().unusedStarters);
        root.put("suggestions", suggestions);
        Map<String, Object> origins = new TreeMap<>();
        r.beanOrigins().forEach((bean, loc) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("location", loc);
            origins.put(bean, entry);
        });
        root.put("beanOrigins", origins);
        root.put("appliedCount", r.applied().size());
        root.put("skippedCount", r.skipped().size());
        root.put("beanCount", r.beanOrigins().size());
        return root;
    }
}
