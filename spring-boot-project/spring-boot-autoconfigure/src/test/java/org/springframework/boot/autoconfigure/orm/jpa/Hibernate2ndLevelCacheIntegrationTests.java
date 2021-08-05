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

package org.springframework.boot.autoconfigure.orm.jpa;

import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Hibernate 2nd level cache with jcache.
 *
 * @author Stephane Nicoll
 */
class Hibernate2ndLevelCacheIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class, DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class);

	@Test
	void hibernate2ndLevelCacheWithJCacheAndEhCache3() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		String configLocation = "ehcache3.xml";
		this.contextRunner
				.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation,
						"spring.jpa.properties.hibernate.cache.region.factory_class=jcache",
						"spring.jpa.properties.hibernate.cache.provider=" + cachingProviderFqn,
						"spring.jpa.properties.hibernate.javax.cache.uri=" + configLocation)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class TestConfiguration {

	}

}
