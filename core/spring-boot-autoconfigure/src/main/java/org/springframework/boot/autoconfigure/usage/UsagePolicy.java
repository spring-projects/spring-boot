package org.springframework.boot.autoconfigure.usage;

import java.util.List;

/**
 * Experimental SPI allowing custom policy evaluation against the structured usage report.
 * Implementations should be cheap and side-effect free.
 */
public interface UsagePolicy {

    /**
     * Evaluate the report and return a result. Never return null.
     */
    PolicyResult evaluate(java.util.Map<String,Object> structured, UsageReportProperties properties);

    /**
     * Simple record containing policy warnings and violations.
     */
    record PolicyResult(List<String> warnings, List<String> violations) {
        public static PolicyResult empty() { return new PolicyResult(List.of(), List.of()); }
    }
}
