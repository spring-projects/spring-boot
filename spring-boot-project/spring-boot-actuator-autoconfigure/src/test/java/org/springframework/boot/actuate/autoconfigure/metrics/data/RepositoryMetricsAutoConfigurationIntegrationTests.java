/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.data;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.data.city.CityRepository;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RepositoryMetricsAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class RepositoryMetricsAutoConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(
					AutoConfigurations.of(HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
							PropertyPlaceholderAutoConfiguration.class, RepositoryMetricsAutoConfiguration.class))
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, TestConfig.class);

	@Test
	void repositoryMethodCallRecordsMetrics() {
		this.contextRunner.run((context) -> {
			context.getBean(CityRepository.class).count();
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.get("spring.data.repository.invocations").tag("repository", "CityRepository").timer()
					.count()).isEqualTo(1);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigurationPackage
	static class TestConfig {

	}

}
