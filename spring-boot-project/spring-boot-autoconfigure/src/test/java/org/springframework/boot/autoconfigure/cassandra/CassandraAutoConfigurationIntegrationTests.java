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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.CassandraContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link CassandraAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class CassandraAutoConfigurationIntegrationTests {

	@Container
	static final CassandraContainer cassandra = new CassandraContainer();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class)).withPropertyValues(
					"spring.data.cassandra.contact-points:" + cassandra.getHost() + ":"
							+ cassandra.getFirstMappedPort(),
					"spring.data.cassandra.local-datacenter=datacenter1", "spring.data.cassandra.request.timeout=20s",
					"spring.data.cassandra.connection.init-query-timeout=10s");

	@Test
	void whenTheContextIsClosedThenTheDriverConfigLoaderIsClosed() {
		this.contextRunner.withUserConfiguration(DriverConfigLoaderSpyConfiguration.class).run((context) -> {
			assertThat(((BeanDefinitionRegistry) context.getSourceApplicationContext())
					.getBeanDefinition("cassandraDriverConfigLoader").getDestroyMethodName()).isEmpty();
			// Initialize lazy bean
			context.getBean(CqlSession.class);
			DriverConfigLoader driverConfigLoader = context.getBean(DriverConfigLoader.class);
			context.close();
			verify(driverConfigLoader).close();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class DriverConfigLoaderSpyConfiguration {

		@Bean
		static BeanPostProcessor driverConfigLoaderSpy() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof DriverConfigLoader) {
						return spy(bean);
					}
					return bean;
				}

			};
		}

	}

}
