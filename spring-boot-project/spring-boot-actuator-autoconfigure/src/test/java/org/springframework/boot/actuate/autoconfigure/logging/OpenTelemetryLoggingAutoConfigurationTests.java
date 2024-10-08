/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.logging;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryLoggingAutoConfiguration}.
 *
 * @author Toshiaki Maki
 */
class OpenTelemetryLoggingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner;

	OpenTelemetryLoggingAutoConfigurationTests() {
		this.contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(OpenTelemetryAutoConfiguration.class, OpenTelemetryLoggingAutoConfiguration.class));
	}

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context).hasSingleBean(SdkLoggerProvider.class);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "io.opentelemetry.sdk.logs", "io.opentelemetry.api" })
	void shouldNotSupplyBeansIfDependencyIsMissing(String packageName) {
		this.contextRunner.withClassLoader(new FilteredClassLoader(packageName)).run((context) -> {
			assertThat(context).doesNotHaveBean(BatchLogRecordProcessor.class);
			assertThat(context).doesNotHaveBean(SdkLoggerProvider.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfig.class).run((context) -> {
			assertThat(context).hasBean("customBatchLogRecordProcessor").hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context.getBeansOfType(LogRecordProcessor.class)).hasSize(1);
			assertThat(context).hasBean("customSdkLoggerProvider").hasSingleBean(SdkLoggerProvider.class);
		});
	}

	@Test
	void shouldAllowMultipleLogRecordExporters() {
		this.contextRunner.withUserConfiguration(MultipleLogRecordExportersConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context.getBeansOfType(LogRecordExporter.class)).hasSize(2);
			assertThat(context).hasBean("customLogRecordExporter1");
			assertThat(context).hasBean("customLogRecordExporter2");
		});
	}

	@Test
	void shouldAllowMultipleLogRecordProcessorsInAdditionToBatchLogRecordProcessor() {
		this.contextRunner.withUserConfiguration(MultipleLogRecordProcessorsConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context).hasSingleBean(SdkLoggerProvider.class);
			assertThat(context.getBeansOfType(LogRecordProcessor.class)).hasSize(3);
			assertThat(context).hasBean("batchLogRecordProcessor");
			assertThat(context).hasBean("customLogRecordProcessor1");
			assertThat(context).hasBean("customLogRecordProcessor2");
		});
	}

	@Test
	void shouldAllowMultipleSdkLoggerProviderBuilderCustomizers() {
		this.contextRunner.withUserConfiguration(MultipleSdkLoggerProviderBuilderCustomizersConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(SdkLoggerProvider.class);
				assertThat(context.getBeansOfType(SdkLoggerProviderBuilderCustomizer.class)).hasSize(2);
				assertThat(context).hasBean("customSdkLoggerProviderBuilderCustomizer1");
				assertThat(context).hasBean("customSdkLoggerProviderBuilderCustomizer2");
				assertThat(context
					.getBean("customSdkLoggerProviderBuilderCustomizer1", NoopSdkLoggerProviderBuilderCustomizer.class)
					.called()).isEqualTo(1);
				assertThat(context
					.getBean("customSdkLoggerProviderBuilderCustomizer2", NoopSdkLoggerProviderBuilderCustomizer.class)
					.called()).isEqualTo(1);
			});
	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomConfig {

		@Bean
		public BatchLogRecordProcessor customBatchLogRecordProcessor() {
			return BatchLogRecordProcessor.builder(new NoopLogRecordExporter()).build();
		}

		@Bean
		public SdkLoggerProvider customSdkLoggerProvider() {
			return SdkLoggerProvider.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class MultipleLogRecordExportersConfig {

		@Bean
		public LogRecordExporter customLogRecordExporter1() {
			return new NoopLogRecordExporter();
		}

		@Bean
		public LogRecordExporter customLogRecordExporter2() {
			return new NoopLogRecordExporter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class MultipleLogRecordProcessorsConfig {

		@Bean
		public LogRecordProcessor customLogRecordProcessor1() {
			return new NoopLogRecordProcessor();
		}

		@Bean
		public LogRecordProcessor customLogRecordProcessor2() {
			return new NoopLogRecordProcessor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class MultipleSdkLoggerProviderBuilderCustomizersConfig {

		@Bean
		public SdkLoggerProviderBuilderCustomizer customSdkLoggerProviderBuilderCustomizer1() {
			return new NoopSdkLoggerProviderBuilderCustomizer();
		}

		@Bean
		public SdkLoggerProviderBuilderCustomizer customSdkLoggerProviderBuilderCustomizer2() {
			return new NoopSdkLoggerProviderBuilderCustomizer();
		}

	}

	static class NoopLogRecordExporter implements LogRecordExporter {

		@Override
		public CompletableResultCode export(Collection<LogRecordData> logs) {
			return CompletableResultCode.ofSuccess();
		}

		@Override
		public CompletableResultCode flush() {
			return CompletableResultCode.ofSuccess();
		}

		@Override
		public CompletableResultCode shutdown() {
			return CompletableResultCode.ofSuccess();
		}

	}

	static class NoopLogRecordProcessor implements LogRecordProcessor {

		@Override
		public void onEmit(Context context, ReadWriteLogRecord logRecord) {

		}

	}

	static class NoopSdkLoggerProviderBuilderCustomizer implements SdkLoggerProviderBuilderCustomizer {

		final AtomicInteger called = new AtomicInteger(0);

		@Override
		public void customize(SdkLoggerProviderBuilder builder) {
			this.called.incrementAndGet();
		}

		int called() {
			return this.called.get();
		}

	}

}
