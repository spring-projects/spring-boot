/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import com.netflix.spectator.atlas.AtlasConfig;
import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for applying {@link MeterRegistryCustomizer} beans.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 */
class MeterRegistryCustomizerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(AtlasMeterRegistry.class, () -> new AtlasMeterRegistry(new AtlasConfig() {

			@Override
			public @Nullable String get(String k) {
				return null;
			}

		}))
		.withBean(PrometheusMeterRegistry.class, () -> new PrometheusMeterRegistry(new PrometheusConfig() {

			@Override
			public @Nullable String get(String key) {
				return null;
			}

		}))
		.withPropertyValues("management.metrics.use-global-registry=false")
		.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
				CompositeMeterRegistryAutoConfiguration.class, JvmMetricsAutoConfiguration.class));

	@Test
	void commonTagsAreAppliedToAutoConfiguredBinders() {
		this.contextRunner.withUserConfiguration(MeterRegistryCustomizerConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("jvm.memory.used").tags("region", "us-east-1").gauge();
		});
	}

	@Test
	void commonTagsAreAppliedBeforeRegistryIsInjectableElsewhere() {
		this.contextRunner.withUserConfiguration(MeterRegistryCustomizerConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("my.thing").tags("region", "us-east-1").gauge();
		});
	}

	@Test
	void customizersCanBeAppliedToSpecificRegistryTypes() {
		this.contextRunner.withUserConfiguration(MeterRegistryCustomizerConfiguration.class).run((context) -> {
			MeterRegistry prometheus = context.getBean(PrometheusMeterRegistry.class);
			prometheus.get("jvm.memory.used").tags("job", "myjob").gauge();
			MeterRegistry atlas = context.getBean(AtlasMeterRegistry.class);
			assertThat(atlas.find("jvm.memory.used").tags("job", "myjob").gauge()).isNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MeterRegistryCustomizerConfiguration {

		@Bean
		MeterRegistryCustomizer<MeterRegistry> commonTags() {
			return (registry) -> registry.config().commonTags("region", "us-east-1");
		}

		@Bean
		MeterRegistryCustomizer<PrometheusMeterRegistry> prometheusOnlyCommonTags() {
			return (registry) -> registry.config().commonTags("job", "myjob");
		}

		@Bean
		MyThing myThing(MeterRegistry registry) {
			registry.gauge("my.thing", 0);
			return new MyThing();
		}

		class MyThing {

		}

	}

}
