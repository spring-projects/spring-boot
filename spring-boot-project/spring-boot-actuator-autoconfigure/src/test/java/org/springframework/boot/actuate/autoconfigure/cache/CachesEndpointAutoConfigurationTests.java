/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cache;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.cache.CachesEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CachesEndpointAutoConfiguration}.
 *
 * @author Johannes Edmeier
 * @author Stephane Nicoll
 */
class CachesEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CachesEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withBean(CacheManager.class, () -> mock(CacheManager.class))
				.withPropertyValues("management.endpoints.web.exposure.include=caches")
				.run((context) -> assertThat(context).hasSingleBean(CachesEndpoint.class));
	}

	@Test
	void runWithoutCacheManagerShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=caches")
				.run((context) -> assertThat(context).hasSingleBean(CachesEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.withBean(CacheManager.class, () -> mock(CacheManager.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CachesEndpoint.class));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.caches.enabled:false")
				.withPropertyValues("management.endpoints.web.exposure.include=*")
				.withBean(CacheManager.class, () -> mock(CacheManager.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CachesEndpoint.class));
	}

}
