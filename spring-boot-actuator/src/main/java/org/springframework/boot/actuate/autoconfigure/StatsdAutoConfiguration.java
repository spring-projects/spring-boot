package org.springframework.boot.actuate.autoconfigure;

import com.timgroup.statsd.NonBlockingStatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.statsd.StatsdMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * Statsd integration. Exports a {@link StatsdMetricWriter} which
 * pushes metrics to the configured statsd server.
 *
 * @author Simon Buettner
 */
@Configuration
@ConditionalOnClass(NonBlockingStatsDClient.class)
@AutoConfigureBefore(MetricExportAutoConfiguration.class)
@EnableConfigurationProperties(StatsdProperties.class)
public class StatsdAutoConfiguration {

    @Autowired
    StatsdProperties statsdProperties;

    @Bean
    @ConditionalOnProperty(prefix = "statsd", name = {"host", "port"})
    @ExportMetricWriter
    public MetricWriter statsdMetricWriter() {
        return new StatsdMetricWriter(
            statsdProperties.getPrefix(),
            statsdProperties.getHost(),
            statsdProperties.getPort()
        );
    }

}
