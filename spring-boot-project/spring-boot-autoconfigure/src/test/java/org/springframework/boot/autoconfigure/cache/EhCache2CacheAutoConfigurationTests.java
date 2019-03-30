/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfigurationTests.DefaultCacheAndCustomizersConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfigurationTests.DefaultCacheConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfigurationTests.EhCacheCustomCacheManager;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.cache.ehcache.EhCacheCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CacheAutoConfiguration} with EhCache 2.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("ehcache-3*.jar")
public class EhCache2CacheAutoConfigurationTests
		extends AbstractCacheAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));

	@Test
	public void ehCacheWithCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=ehcache").run((context) -> {
					EhCacheCacheManager cacheManager = getCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("cacheTest1",
							"cacheTest2");
					assertThat(context.getBean(net.sf.ehcache.CacheManager.class))
							.isEqualTo(cacheManager.getCacheManager());
				});
	}

	@Test
	public void ehCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "ehcache")
				.run(verifyCustomizers("allCacheManagerCustomizer",
						"ehcacheCacheManagerCustomizer"));
	}

	@Test
	public void ehCacheWithConfig() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=ehcache",
						"spring.cache.ehcache.config=cache/ehcache-override.xml")
				.run((context) -> {
					EhCacheCacheManager cacheManager = getCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames())
							.containsOnly("cacheOverrideTest1", "cacheOverrideTest2");
				});
	}

	@Test
	public void ehCacheWithExistingCacheManager() {
		this.contextRunner.withUserConfiguration(EhCacheCustomCacheManager.class)
				.withPropertyValues("spring.cache.type=ehcache").run((context) -> {
					EhCacheCacheManager cacheManager = getCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheManager())
							.isEqualTo(context.getBean("customEhCacheCacheManager"));
				});
	}

}
