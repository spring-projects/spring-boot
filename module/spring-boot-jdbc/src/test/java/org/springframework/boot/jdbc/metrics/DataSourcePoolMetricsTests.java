/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metrics;

import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link DataSourcePoolMetrics}.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 */
class DataSourcePoolMetricsTests {

	@Test
	void dataSourceIsInstrumented() {
		new ApplicationContextRunner().withUserConfiguration(DataSourceConfig.class, MetricsApp.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true", "metrics.use-global-registry=false")
			.run((context) -> {
				context.getBean(DataSource.class).getConnection().getMetaData();
				context.getBean(MeterRegistry.class).get("jdbc.connections.max").meter();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class MetricsApp {

		@Bean
		MeterRegistry registry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceConfig {

		DataSourceConfig(DataSource dataSource, Collection<DataSourcePoolMetadataProvider> metadataProviders,
				MeterRegistry registry) {
			new DataSourcePoolMetrics(dataSource, metadataProviders, "data.source", Collections.emptyList())
				.bindTo(registry);
		}

	}

}
