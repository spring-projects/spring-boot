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

package org.springframework.boot.actuate.autoconfigure.ssl;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.ssl.SslHealthContributorAutoConfigurationTests.CustomSslInfoConfiguration.CustomSslHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.ssl.SslHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.CertificateChainInfo;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslHealthContributorAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class SslHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(SslHealthContributorAutoConfiguration.class, SslAutoConfiguration.class))
		.withPropertyValues("server.ssl.bundle=ssltest",
				"spring.ssl.bundle.jks.ssltest.keystore.location=classpath:test.jks");

	@Test
	void beansShouldNotBeConfigured() {
		this.contextRunner.withPropertyValues("management.health.ssl.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class)
				.doesNotHaveBean(SslInfo.class));
	}

	@Test
	void beansShouldBeConfigured() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(SslHealthIndicator.class);
			assertThat(context).hasSingleBean(SslInfo.class);
			Health health = context.getBean(SslHealthIndicator.class).health();
			assertThat(health.getStatus()).isSameAs(Status.OUT_OF_SERVICE);
			assertDetailsKeys(health);
			List<CertificateChainInfo> invalidChains = getInvalidChains(health);
			assertThat(invalidChains).hasSize(1);
			assertThat(invalidChains).first().isInstanceOf(CertificateChainInfo.class);

		});
	}

	@Test
	void beansShouldBeConfiguredWithWarningThreshold() {
		this.contextRunner.withPropertyValues("management.health.ssl.certificate-validity-warning-threshold=1d")
			.run((context) -> {
				assertThat(context).hasSingleBean(SslHealthIndicator.class);
				assertThat(context).hasSingleBean(SslInfo.class);
				assertThat(context).hasSingleBean(SslHealthIndicatorProperties.class);
				assertThat(context.getBean(SslHealthIndicatorProperties.class).getCertificateValidityWarningThreshold())
					.isEqualTo(Duration.ofDays(1));
				Health health = context.getBean(SslHealthIndicator.class).health();
				assertThat(health.getStatus()).isSameAs(Status.OUT_OF_SERVICE);
				assertDetailsKeys(health);
				List<CertificateChainInfo> invalidChains = getInvalidChains(health);
				assertThat(invalidChains).hasSize(1);
				assertThat(invalidChains).first().isInstanceOf(CertificateChainInfo.class);
			});
	}

	@Test
	void customBeansShouldBeConfigured() {
		this.contextRunner.withUserConfiguration(CustomSslInfoConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SslHealthIndicator.class);
			assertThat(context.getBean(SslHealthIndicator.class))
				.isSameAs(context.getBean(CustomSslHealthIndicator.class));
			assertThat(context).hasSingleBean(SslInfo.class);
			assertThat(context.getBean(SslInfo.class)).isSameAs(context.getBean("customSslInfo"));
			Health health = context.getBean(SslHealthIndicator.class).health();
			assertThat(health.getStatus()).isSameAs(Status.OUT_OF_SERVICE);
			assertDetailsKeys(health);
			List<CertificateChainInfo> invalidChains = getInvalidChains(health);
			assertThat(invalidChains).hasSize(1);
			assertThat(invalidChains).first().isInstanceOf(CertificateChainInfo.class);
		});
	}

	private static void assertDetailsKeys(Health health) {
		assertThat(health.getDetails()).containsOnlyKeys("validChains", "invalidChains");
	}

	@SuppressWarnings("unchecked")
	private static List<CertificateChainInfo> getInvalidChains(Health health) {
		return (List<CertificateChainInfo>) health.getDetails().get("invalidChains");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSslInfoConfiguration {

		@Bean
		SslHealthIndicator sslHealthIndicator(SslInfo sslInfo) {
			return new CustomSslHealthIndicator(sslInfo);
		}

		@Bean
		SslInfo customSslInfo(SslBundles sslBundles) {
			return new SslInfo(sslBundles, Duration.ofDays(7));
		}

		static class CustomSslHealthIndicator extends SslHealthIndicator {

			CustomSslHealthIndicator(SslInfo sslInfo) {
				super(sslInfo);
			}

		}

	}

}
