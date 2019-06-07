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

package org.springframework.boot.autoconfigure.orm.jpa;

import org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Hibernate 2nd level cache with ehcache2.
 *
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("ehcache-3*.jar")
public class Hibernate2ndLevelCacheEhCacheIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class, DataSourceAutoConfiguration.class,
					HibernateJpaAutoConfiguration.class))
			.withPropertyValues("spring.datasource.initialization-mode=never")
			.withUserConfiguration(TestConfiguration.class);

	@Test
	public void hibernate2ndLevelCacheWithEhCache2() {
		this.contextRunner
				.withPropertyValues("spring.cache.type=ehcache",
						"spring.jpa.properties.hibernate.cache.region.factory_class="
								+ SingletonEhCacheRegionFactory.class.getName())
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Configuration
	@EnableCaching
	static class TestConfiguration {

	}

}
