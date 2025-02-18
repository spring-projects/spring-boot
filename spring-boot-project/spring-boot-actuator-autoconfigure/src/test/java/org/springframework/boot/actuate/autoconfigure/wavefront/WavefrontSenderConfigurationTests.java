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

package org.springframework.boot.actuate.autoconfigure.wavefront;

import java.util.concurrent.LinkedBlockingQueue;

import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.clients.service.token.CSPTokenService;
import com.wavefront.sdk.common.clients.service.token.NoopProxyTokenService;
import com.wavefront.sdk.common.clients.service.token.WavefrontTokenService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WavefrontSenderConfiguration}.
 *
 * @author Moritz Halbritter
 */
class WavefrontSenderConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WavefrontSenderConfiguration.class));

	private final ApplicationContextRunner metricsDisabledContextRunner = this.contextRunner.withPropertyValues(
			"management.defaults.metrics.export.enabled=false", "management.simple.metrics.export.enabled=true");

	@Test
	void shouldNotFailIfWavefrontIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("com.wavefront"))
			.run(((context) -> assertThat(context).doesNotHaveBean(WavefrontSender.class)));
	}

	@Test
	void failsWithoutAnApiTokenWhenPublishingDirectly() {
		this.contextRunner.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void defaultWavefrontSenderSettingsAreConsistent() {
		this.contextRunner.withPropertyValues("management.wavefront.api-token=abcde").run((context) -> {
			WavefrontProperties properties = new WavefrontProperties();
			WavefrontSender sender = context.getBean(WavefrontSender.class);
			assertThat(sender)
				.extracting("metricsBuffer", as(InstanceOfAssertFactories.type(LinkedBlockingQueue.class)))
				.satisfies((queue) -> assertThat(queue.remainingCapacity() + queue.size())
					.isEqualTo(properties.getSender().getMaxQueueSize()));
			assertThat(sender).hasFieldOrPropertyWithValue("batchSize", properties.getSender().getBatchSize());
			assertThat(sender).hasFieldOrPropertyWithValue("messageSizeBytes",
					(int) properties.getSender().getMessageSize().toBytes());
		});
	}

	@Test
	void configureWavefrontSender() {
		this.contextRunner
			.withPropertyValues("management.wavefront.api-token=abcde", "management.wavefront.sender.batch-size=50",
					"management.wavefront.sender.max-queue-size=100", "management.wavefront.sender.message-size=1KB")
			.run((context) -> {
				WavefrontSender sender = context.getBean(WavefrontSender.class);
				assertThat(sender).hasFieldOrPropertyWithValue("batchSize", 50);
				assertThat(sender)
					.extracting("metricsBuffer", as(InstanceOfAssertFactories.type(LinkedBlockingQueue.class)))
					.satisfies((queue) -> assertThat(queue.remainingCapacity() + queue.size()).isEqualTo(100));
				assertThat(sender).hasFieldOrPropertyWithValue("messageSizeBytes", 1024);
			});
	}

	@Test
	void shouldNotSupplyWavefrontSenderIfMetricsAndGlobalTracingIsDisabled() {
		this.metricsDisabledContextRunner
			.withPropertyValues("management.tracing.enabled=false", "management.wavefront.api-token=abcde")
			.run((context) -> assertThat(context).doesNotHaveBean(WavefrontSender.class));
	}

	@Test
	void shouldNotSupplyWavefrontSenderIfMetricsAndWavefrontTracingIsDisabled() {
		this.metricsDisabledContextRunner
			.withPropertyValues("management.wavefront.tracing.export.enabled=false",
					"management.wavefront.api-token=abcde")
			.run((context) -> assertThat(context).doesNotHaveBean(WavefrontSender.class));
	}

	@Test
	void shouldSupplyWavefrontSenderIfOnlyGlobalTracingIsDisabled() {
		this.contextRunner
			.withPropertyValues("management.tracing.enabled=false", "management.wavefront.api-token=abcde")
			.run((context) -> assertThat(context).hasSingleBean(WavefrontSender.class));
	}

	@Test
	void shouldSupplyWavefrontSenderIfOnlyWavefrontTracingIsDisabled() {
		this.contextRunner
			.withPropertyValues("management.wavefront.tracing.export.enabled=false",
					"management.wavefront.api-token=abcde")
			.run((context) -> assertThat(context).hasSingleBean(WavefrontSender.class));
	}

	@Test
	void shouldSupplyWavefrontSenderIfOnlyMetricsAreDisabled() {
		this.metricsDisabledContextRunner.withPropertyValues("management.wavefront.api-token=abcde")
			.run((context) -> assertThat(context).hasSingleBean(WavefrontSender.class));
	}

	@Test
	void allowsWavefrontSenderToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomSenderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(WavefrontSender.class).hasBean("customSender"));
	}

	@Test
	void shouldApplyTokenTypeWavefrontApiToken() {
		this.contextRunner
			.withPropertyValues("management.wavefront.api-token-type=WAVEFRONT_API_TOKEN",
					"management.wavefront.api-token=abcde")
			.run((context) -> {
				WavefrontSender sender = context.getBean(WavefrontSender.class);
				assertThat(sender).extracting("tokenService").isInstanceOf(WavefrontTokenService.class);
			});
	}

	@Test
	void shouldApplyTokenTypeCspApiToken() {
		this.contextRunner
			.withPropertyValues("management.wavefront.api-token-type=CSP_API_TOKEN",
					"management.wavefront.api-token=abcde")
			.run((context) -> {
				WavefrontSender sender = context.getBean(WavefrontSender.class);
				assertThat(sender).extracting("tokenService").isInstanceOf(CSPTokenService.class);
			});
	}

	@Test
	void shouldApplyTokenTypeCspClientCredentials() {
		this.contextRunner
			.withPropertyValues("management.wavefront.api-token-type=CSP_CLIENT_CREDENTIALS",
					"management.wavefront.api-token=clientid=cid,clientsecret=csec")
			.run((context) -> {
				WavefrontSender sender = context.getBean(WavefrontSender.class);
				assertThat(sender).extracting("tokenService").isInstanceOf(CSPTokenService.class);
			});
	}

	@Test
	void shouldApplyTokenTypeNoToken() {
		this.contextRunner.withPropertyValues("management.wavefront.api-token-type=NO_TOKEN").run((context) -> {
			WavefrontSender sender = context.getBean(WavefrontSender.class);
			assertThat(sender).extracting("tokenService").isInstanceOf(NoopProxyTokenService.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSenderConfiguration {

		@Bean
		WavefrontSender customSender() {
			return mock(WavefrontSender.class);
		}

	}

}
