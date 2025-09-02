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

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for the experimental usage (dependency & auto-config) report.
 * <p>Enable with {@code spring.boot.usage.report.enabled=true} to generate a JSON &
 * plain-text summary of applied and skipped auto-configurations plus bean -> artifact
 * origin mapping.
 *
 * @author Generated
 * @since 3.x
 */
@ConfigurationProperties("spring.boot.usage.report")
public class UsageReportProperties {

    /**
     * Enable generation of a runtime usage report (experimental).
     */
    private boolean enabled = false;

    /**
     * Output directory for report artifacts. Relative paths are resolved against the
     * working directory.
     */
    private Path outputDir = Path.of("build", "boot-usage");

    /**
     * Write a Markdown summary alongside the JSON report.
     */
    private boolean markdownSummary = true;

    /**
     * Include skipped auto-configuration details (conditions & reasons) in JSON.
     */
    private boolean includeSkippedDetails = true;

    /**
     * Mark application as failed if unused starters (future phase) are detected.
     */
    private boolean failOnUnused = false;

    /**
     * Fail application startup if report writing fails.
     */
    private boolean failOnError = false;

    /**
     * Cache TTL for generated report served via endpoint (0 disables caching).
     */
    private Duration cacheTtl = Duration.ofMinutes(5);

    /**
     * Include bean origin details in the report (may reveal artifact coordinates); disable
     * to reduce potentially sensitive information.
     */
    private boolean includeOrigins = true;

    /**
     * Include a basic confidence score map for suggestions.
     */
    private boolean includeConfidence = true;

    /**
     * Attempt a lightweight heuristic to list potentially unused jars on the classpath.
     * (Experimental, may produce false positives.)
     */
    private boolean detectUnusedJars = false;

    /**
     * Policy evaluation configuration.
     */
    private final Policies policies = new Policies();

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getOutputDir() {
        return this.outputDir;
    }

    public void setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isMarkdownSummary() {
        return this.markdownSummary;
    }

    public void setMarkdownSummary(boolean markdownSummary) {
        this.markdownSummary = markdownSummary;
    }

    public boolean isFailOnError() {
        return this.failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean isIncludeSkippedDetails() {
        return this.includeSkippedDetails;
    }

    public void setIncludeSkippedDetails(boolean includeSkippedDetails) {
        this.includeSkippedDetails = includeSkippedDetails;
    }

    public boolean isFailOnUnused() {
        return this.failOnUnused;
    }

    public void setFailOnUnused(boolean failOnUnused) {
        this.failOnUnused = failOnUnused;
    }

    public Duration getCacheTtl() {
        return this.cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public boolean isIncludeOrigins() {
        return this.includeOrigins;
    }

    public void setIncludeOrigins(boolean includeOrigins) {
        this.includeOrigins = includeOrigins;
    }

    public boolean isIncludeConfidence() {
        return this.includeConfidence;
    }

    public void setIncludeConfidence(boolean includeConfidence) {
        this.includeConfidence = includeConfidence;
    }

    public boolean isDetectUnusedJars() {
        return this.detectUnusedJars;
    }

    public void setDetectUnusedJars(boolean detectUnusedJars) {
        this.detectUnusedJars = detectUnusedJars;
    }

    public Policies getPolicies() {
        return this.policies;
    }

    /**
     * Nested properties related to policy evaluation.
     */
    public static class Policies {
        /**
         * Fail application startup if any policy violation is reported.
         */
        private boolean failOnViolation = false;

        public boolean isFailOnViolation() {
            return this.failOnViolation;
        }

        public void setFailOnViolation(boolean failOnViolation) {
            this.failOnViolation = failOnViolation;
        }
    }
}
