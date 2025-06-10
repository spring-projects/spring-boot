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

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OpenTelemetrySdkAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Toshiaki Maki
 * @author Phillip Webb
 */
class OpenTelemetrySdkAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OpenTelemetrySdkAutoConfiguration.class));

	@Test
	void registeredInAutoConfigurationImports() {
		assertThat(ImportCandidates.load(AutoConfiguration.class, null).getCandidates())
			.contains(OpenTelemetrySdkAutoConfiguration.class.getName());
	}

	@Test
	void providesBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetrySdk.class);
			assertThat(context).hasSingleBean(Resource.class);
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context).hasSingleBean(SdkLoggerProvider.class);
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "io.opentelemetry", "io.opentelemetry.api" })
	void whenOpenTelemetryIsNotOnClasspathDoesNotProvideBeans(String packageName) {
		this.contextRunner.withClassLoader(new FilteredClassLoader(packageName)).run((context) -> {
			assertThat(context).doesNotHaveBean(OpenTelemetrySdk.class);
			assertThat(context).doesNotHaveBean(Resource.class);
			assertThat(context).doesNotHaveBean(BatchLogRecordProcessor.class);
			assertThat(context).doesNotHaveBean(SdkLoggerProvider.class);
		});
	}

	@Test
	void whenOpenTelemetryLogsIsNotOnClasspathDoesNotProvideBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.sdk.logs")).run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetrySdk.class);
			assertThat(context).hasSingleBean(Resource.class);
			assertThat(context).doesNotHaveBean(BatchLogRecordProcessor.class);
			assertThat(context).doesNotHaveBean(SdkLoggerProvider.class);
		});
	}

	@Test
	void whenHasUserSuppliedBeansDoesNotProvideBeans() {
		this.contextRunner.withUserConfiguration(UserConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasBean("customOpenTelemetry");
			assertThat(context).hasSingleBean(Resource.class);
			assertThat(context).hasBean("customResource");
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context).hasBean("customBatchLogRecordProcessor").hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context).hasSingleBean(LogRecordProcessor.class);
			assertThat(context).hasBean("customSdkLoggerProvider").hasSingleBean(SdkLoggerProvider.class);
		});
	}

	@Test
	void whenHasApplicationNamePropertyProvidesServiceNameResourceAttribute() {
		this.contextRunner.withPropertyValues("spring.application.name=my-application").run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap())
				.contains(entry(AttributeKey.stringKey("service.name"), "my-application"));
		});
	}

	@Test
	void whenHasApplicationGroupPropertyProvidesServiceNamespaceResourceAttribute() {
		this.contextRunner.withPropertyValues("spring.application.group=my-group").run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap()).containsEntry(AttributeKey.stringKey("service.namespace"),
					"my-group");
		});
	}

	@Test
	void whenHasNoApplicationGroupPropertyProvidesNoServiceGroupResourceAttribute() {
		this.contextRunner.run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap()).doesNotContainKey(AttributeKey.stringKey("service.group"));
		});
	}

	@Test
	void whenHasNoApplicationGroupPropertyProvidesNoServiceNamespaceResourceAttribute() {
		this.contextRunner.run(((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap()).doesNotContainKey(AttributeKey.stringKey("service.namespace"));
		}));
	}

	@Test
	void whenHasNoApplicationNamePropertyProvidesDefaultApplicationName() {
		this.contextRunner.run((context) -> {
			Resource resource = context.getBean(Resource.class);
			assertThat(resource.getAttributes().asMap())
				.contains(entry(AttributeKey.stringKey("service.name"), "unknown_service"));
		});
	}

	@Test
	void whenHasResourceAttributesPropertyProvidesResourceAttributes() {
		this.contextRunner.withPropertyValues("management.opentelemetry.resource-attributes.region=us-west")
			.run((context) -> {
				Resource resource = context.getBean(Resource.class);
				assertThat(resource.getAttributes().asMap())
					.contains(entry(AttributeKey.stringKey("region"), "us-west"));
			});
	}

	@Test
	void whenHasSdkTracerProviderBeanProvidesTracerProvider() {
		this.contextRunner.withBean(SdkTracerProvider.class, () -> SdkTracerProvider.builder().build())
			.run((context) -> {
				OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
				assertThat(openTelemetry.getTracerProvider()).isNotNull();
			});
	}

	@Test
	void whenHasContextPropagatorsBeanProvidesPropagators() {
		this.contextRunner.withBean(ContextPropagators.class, ContextPropagators::noop).run((context) -> {
			OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
			assertThat(openTelemetry.getPropagators()).isNotNull();
		});
	}

	@Test
	void whenHasSdkLoggerProviderBeanProvidesLogsBridge() {
		this.contextRunner.withBean(SdkLoggerProvider.class, () -> SdkLoggerProvider.builder().build())
			.run((context) -> {
				OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
				assertThat(openTelemetry.getLogsBridge()).isNotNull();
			});
	}

	@Test
	void whenHasSdkMeterProviderProvidesMeterProvider() {
		this.contextRunner.withBean(SdkMeterProvider.class, () -> SdkMeterProvider.builder().build()).run((context) -> {
			OpenTelemetry openTelemetry = context.getBean(OpenTelemetry.class);
			assertThat(openTelemetry.getMeterProvider()).isNotNull();
		});
	}

	@Test
	void whenHasMultipleLogRecordExportersProvidesBatchLogRecordProcessor() {
		this.contextRunner.withUserConfiguration(MultipleLogRecordExportersConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context.getBeansOfType(LogRecordExporter.class)).hasSize(2);
			assertThat(context).hasBean("customLogRecordExporter1");
			assertThat(context).hasBean("customLogRecordExporter2");
		});
	}

	@Test
	void whenHasMultipleLogRecordProcessorsStillProvidesBatchLogRecordProcessor() {
		this.contextRunner.withUserConfiguration(MultipleLogRecordProcessorsConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(BatchLogRecordProcessor.class);
			assertThat(context).hasSingleBean(SdkLoggerProvider.class);
			assertThat(context.getBeansOfType(LogRecordProcessor.class)).hasSize(3);
			assertThat(context).hasBean("openTelemetryBatchLogRecordProcessor");
			assertThat(context).hasBean("customLogRecordProcessor1");
			assertThat(context).hasBean("customLogRecordProcessor2");
		});
	}

	@Test
	void whenHasMultipleSdkLoggerProviderBuilderCustomizersCallsCustomizeMethod() {
		this.contextRunner.withUserConfiguration(MultipleSdkLoggerProviderBuilderCustomizersConfiguration.class)
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
	static class UserConfiguration {

		@Bean
		OpenTelemetry customOpenTelemetry() {
			return mock(OpenTelemetry.class);
		}

		@Bean
		Resource customResource() {
			return Resource.getDefault();
		}

		@Bean
		BatchLogRecordProcessor customBatchLogRecordProcessor() {
			return BatchLogRecordProcessor.builder(new NoopLogRecordExporter()).build();
		}

		@Bean
		SdkLoggerProvider customSdkLoggerProvider() {
			return SdkLoggerProvider.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleLogRecordExportersConfiguration {

		@Bean
		LogRecordExporter customLogRecordExporter1() {
			return new NoopLogRecordExporter();
		}

		@Bean
		LogRecordExporter customLogRecordExporter2() {
			return new NoopLogRecordExporter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleLogRecordProcessorsConfiguration {

		@Bean
		LogRecordProcessor customLogRecordProcessor1() {
			return new NoopLogRecordProcessor();
		}

		@Bean
		LogRecordProcessor customLogRecordProcessor2() {
			return new NoopLogRecordProcessor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleSdkLoggerProviderBuilderCustomizersConfiguration {

		@Bean
		SdkLoggerProviderBuilderCustomizer customSdkLoggerProviderBuilderCustomizer1() {
			return new NoopSdkLoggerProviderBuilderCustomizer();
		}

		@Bean
		SdkLoggerProviderBuilderCustomizer customSdkLoggerProviderBuilderCustomizer2() {
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
