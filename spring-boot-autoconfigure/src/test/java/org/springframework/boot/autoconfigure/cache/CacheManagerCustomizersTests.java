/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 *
 * @author Stephane Nicoll
 */
public class CacheManagerCustomizersTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void customizeSimpleCacheManager() {
		load(SimpleConfiguration.class, "spring.cache.type=simple");
		ConcurrentMapCacheManager cacheManager = this.context
				.getBean(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("one", "two");
	}

	@Test
	public void customizeNoConfigurableApplicationContext() {
		CacheManagerCustomizers invoker = new CacheManagerCustomizers();
		ApplicationContext context = mock(ApplicationContext.class);
		invoker.setApplicationContext(context);
		invoker.customize(mock(CacheManager.class));
		verifyZeroInteractions(context);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(config);
		applicationContext.register(CacheAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	@EnableCaching
	static class SimpleConfiguration {

		@Bean
		public CacheManagerCustomizer<ConcurrentMapCacheManager> cacheManagerCustomizer() {
			return new CacheManagerCustomizer<ConcurrentMapCacheManager>() {

				@Override
				public void customize(ConcurrentMapCacheManager cacheManager) {
					cacheManager.setCacheNames(Arrays.asList("one", "two"));
				}

			};
		}

	}
}
