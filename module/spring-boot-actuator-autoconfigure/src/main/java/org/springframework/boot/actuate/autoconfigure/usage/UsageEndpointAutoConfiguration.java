package org.springframework.boot.actuate.autoconfigure.usage;

import org.springframework.boot.actuate.usage.UsageEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.usage.UsageReportProperties;
import org.springframework.boot.autoconfigure.usage.UsageReportService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the bootusage actuator endpoint.
 */
import org.springframework.boot.autoconfigure.usage.UsageAnalysisAutoConfiguration;

@AutoConfiguration(after = UsageAnalysisAutoConfiguration.class)
@ConditionalOnClass(UsageEndpoint.class)
@ConditionalOnProperty(prefix = "spring.boot.usage.report", name = "enabled", havingValue = "true")
@ConditionalOnAvailableEndpoint(UsageEndpoint.class)
public final class UsageEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    UsageEndpoint usageEndpoint(ConfigurableApplicationContext context, UsageReportProperties props, UsageReportService service) {
        return new UsageEndpoint(context, props, service);
    }
}
