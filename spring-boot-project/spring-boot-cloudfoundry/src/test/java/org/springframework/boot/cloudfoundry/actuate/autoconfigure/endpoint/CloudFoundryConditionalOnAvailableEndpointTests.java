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

package org.springframework.boot.cloudfoundry.actuate.autoconfigure.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnAvailableEndpoint @ConditionalOnAvailableEndpoint} when
 * running on Cloud Foundry.
 *
 * @author Brian Clozel
 */
class CloudFoundryConditionalOnAvailableEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(AllEndpointsConfiguration.class)
		.withInitializer(
				(context) -> context.getEnvironment().setConversionService(new ApplicationConversionService()));

	@Test
	void outcomeOnCloudFoundryShouldMatchAll() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---")
			.run((context) -> assertThat(context).hasBean("info").hasBean("health").hasBean("spring").hasBean("test"));
	}

	@Endpoint(id = "health")
	static class HealthEndpoint {

	}

	@Endpoint(id = "info")
	static class InfoEndpoint {

	}

	@Endpoint(id = "spring")
	static class SpringEndpoint {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

	}

	@Endpoint(id = "shutdown", defaultAccess = Access.NONE)
	static class ShutdownEndpoint {

	}

	@Configuration(proxyBeanMethods = false)
	static class AllEndpointsConfiguration {

		@Bean
		@ConditionalOnAvailableEndpoint
		HealthEndpoint health() {
			return new HealthEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		InfoEndpoint info() {
			return new InfoEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		SpringEndpoint spring() {
			return new SpringEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		TestEndpoint test() {
			return new TestEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		ShutdownEndpoint shutdown() {
			return new ShutdownEndpoint();
		}

	}

}
