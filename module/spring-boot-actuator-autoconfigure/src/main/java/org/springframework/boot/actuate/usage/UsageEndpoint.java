package org.springframework.boot.actuate.usage;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.usage.BeanOriginTrackingPostProcessor;
import org.springframework.boot.autoconfigure.usage.UsageReportProperties;
import org.springframework.boot.autoconfigure.usage.UsageReportService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Actuator endpoint that exposes the structured Spring Boot usage report (experimental).
 */
/**
 * Experimental actuator endpoint exposing the structured runtime usage report.
 */
@Endpoint(id = "bootusage")
public class UsageEndpoint {

    private final ConfigurableApplicationContext context;
    private final UsageReportProperties properties;
    private final UsageReportService service;
    
    public UsageEndpoint(ConfigurableApplicationContext context, UsageReportProperties properties, UsageReportService service) {
        Assert.notNull(context, "context must not be null");
        this.context = context;
        this.properties = properties;
        this.service = service;
    }

    @ReadOperation
    public Map<String, Object> usage(Boolean force) {
        boolean forceRefresh = (force != null && force);
        if (!this.properties.isEnabled()) {
            return OperationResponseBody.of(Map.of("enabled", Boolean.FALSE));
        }
        ConditionEvaluationReport report = ConditionEvaluationReport.get(this.context.getBeanFactory());
        Map<String, String> origins = this.context.getBeansOfType(BeanOriginTrackingPostProcessor.class)
                .values().stream().findFirst().map(BeanOriginTrackingPostProcessor::getBeanOrigins)
                .orElseGet(Map::of);
        Map<String, Object> structured = this.service.currentStructuredReport(report, origins, forceRefresh);
        return OperationResponseBody.of(structured);
    }
}
