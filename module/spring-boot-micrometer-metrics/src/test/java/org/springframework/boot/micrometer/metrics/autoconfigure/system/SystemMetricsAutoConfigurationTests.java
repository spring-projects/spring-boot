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

package org.springframework.boot.micrometer.metrics.autoconfigure.system;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuCountMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuLoadMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuTimeMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.system.DiskSpaceMetricsBinder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link SystemMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Chris Bono
 */
class SystemMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
		.withConfiguration(AutoConfigurations.of(SystemMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresUptimeMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(UptimeMetrics.class));
	}

	@Test
	void allowsCustomUptimeMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomUptimeMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(UptimeMetrics.class).hasBean("customUptimeMetrics"));
	}

	@Test
	void autoConfiguresProcessorMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class));
	}

	@Test
	void allowsCustomProcessorMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomProcessorMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class)
				.hasBean("customProcessorMetrics"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void allowsCustomJvmCpuMeterConventionsToBeUsed() {
		JvmCpuMeterConventions conventions = spy(new MicrometerJvmCpuMeterConventions(Tags.empty()));
		this.contextRunner.withBean(JvmCpuMeterConventions.class, () -> conventions).run((context) -> {
			assertThat(context).hasSingleBean(ProcessorMetrics.class);
			then(conventions).should(atLeast(1)).cpuTimeConvention();
			then(conventions).should(atLeast(1)).cpuCountConvention();
			then(conventions).should(atLeast(1)).processCpuLoadConvention();
		});
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmCpuMeterConventionsAndJvmCpuTimeMeterConventionAreSet() {
		this.contextRunner.withBean(JvmCpuMeterConventions.class, () -> mock(JvmCpuMeterConventions.class))
			.withBean(JvmCpuTimeMeterConvention.class, () -> mock(JvmCpuTimeMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmCpuMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmCpuMeterConventionsAndJvmCpuCountMeterConventionAreSet() {
		this.contextRunner.withBean(JvmCpuMeterConventions.class, () -> mock(JvmCpuMeterConventions.class))
			.withBean(JvmCpuCountMeterConvention.class, () -> mock(JvmCpuCountMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmCpuMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmCpuMeterConventionsAndJvmCpuLoadMeterConventionAreSet() {
		this.contextRunner.withBean(JvmCpuMeterConventions.class, () -> mock(JvmCpuMeterConventions.class))
			.withBean(JvmCpuLoadMeterConvention.class, () -> mock(JvmCpuLoadMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmCpuMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	void allowsCustomJvmCpuTimeMeterConventionToBeUsed() {
		JvmCpuTimeMeterConvention cpuTimeConvention = mock(JvmCpuTimeMeterConvention.class);
		this.contextRunner.withBean(JvmCpuTimeMeterConvention.class, () -> cpuTimeConvention)
			.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class)
				.getBean(ProcessorMetrics.class)
				.hasFieldOrPropertyWithValue("cpuTimeConvention", cpuTimeConvention));
	}

	@Test
	void allowsCustomJvmCpuCountMeterConventionToBeUsed() {
		JvmCpuCountMeterConvention cpuCountConvention = mock(JvmCpuCountMeterConvention.class);
		this.contextRunner.withBean(JvmCpuCountMeterConvention.class, () -> cpuCountConvention)
			.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class)
				.getBean(ProcessorMetrics.class)
				.hasFieldOrPropertyWithValue("cpuCountConvention", cpuCountConvention));
	}

	@Test
	void allowsCustomJvmCpuLoadMeterConventionToBeUsed() {
		JvmCpuLoadMeterConvention cpuLoadConvention = mock(JvmCpuLoadMeterConvention.class);
		this.contextRunner.withBean(JvmCpuLoadMeterConvention.class, () -> cpuLoadConvention)
			.run((context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class)
				.getBean(ProcessorMetrics.class)
				.hasFieldOrPropertyWithValue("cpuLoadConvention", cpuLoadConvention));
	}

	@Test
	void autoConfiguresFileDescriptorMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(FileDescriptorMetrics.class));
	}

	@Test
	void allowsCustomFileDescriptorMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomFileDescriptorMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(FileDescriptorMetrics.class)
				.hasBean("customFileDescriptorMetrics"));
	}

	@Test
	void autoConfiguresDiskSpaceMetrics() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DiskSpaceMetricsBinder.class));
	}

	@Test
	void allowsCustomDiskSpaceMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomDiskSpaceMetricsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(DiskSpaceMetricsBinder.class)
				.hasBean("customDiskSpaceMetrics"));
	}

	@Test
	void diskSpaceMetricsUsesDefaultPath() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("diskSpaceMetrics")
			.getBean(DiskSpaceMetricsBinder.class)
			.hasFieldOrPropertyWithValue("paths", Collections.singletonList(new File("."))));
	}

	@Test
	void allowsDiskSpaceMetricsPathToBeConfiguredWithSinglePath() {
		this.contextRunner.withPropertyValues("management.metrics.system.diskspace.paths:..")
			.run((context) -> assertThat(context).hasBean("diskSpaceMetrics")
				.getBean(DiskSpaceMetricsBinder.class)
				.hasFieldOrPropertyWithValue("paths", Collections.singletonList(new File(".."))));
	}

	@Test
	void allowsDiskSpaceMetricsPathToBeConfiguredWithMultiplePaths() {
		this.contextRunner.withPropertyValues("management.metrics.system.diskspace.paths:.,..")
			.run((context) -> assertThat(context).hasBean("diskSpaceMetrics")
				.getBean(DiskSpaceMetricsBinder.class)
				.hasFieldOrPropertyWithValue("paths", Arrays.asList(new File("."), new File(".."))));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomUptimeMetricsConfiguration {

		@Bean
		UptimeMetrics customUptimeMetrics() {
			return new UptimeMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomProcessorMetricsConfiguration {

		@Bean
		ProcessorMetrics customProcessorMetrics() {
			return new ProcessorMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFileDescriptorMetricsConfiguration {

		@Bean
		FileDescriptorMetrics customFileDescriptorMetrics() {
			return new FileDescriptorMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDiskSpaceMetricsConfiguration {

		@Bean
		DiskSpaceMetricsBinder customDiskSpaceMetrics() {
			return new DiskSpaceMetricsBinder(Collections.singletonList(new File(System.getProperty("user.dir"))),
					Tags.empty());
		}

	}

}
