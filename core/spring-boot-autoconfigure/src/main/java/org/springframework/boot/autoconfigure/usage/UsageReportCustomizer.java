package org.springframework.boot.autoconfigure.usage;

import java.util.Map;

/**
 * Callback interface that can customize the structured usage report before it is
 * cached or returned by the actuator endpoint. Implementations can add or mutate
 * entries in the provided {@code structured} map.
 * <p>Experimental API – may evolve.</p>
 */
@FunctionalInterface
public interface UsageReportCustomizer {

    void customize(Map<String, Object> structured, UsageReportProperties properties);
}
