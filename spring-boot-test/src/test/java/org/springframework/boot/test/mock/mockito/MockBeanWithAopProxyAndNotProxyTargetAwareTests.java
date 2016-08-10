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

package org.springframework.boot.test.mock.mockito;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.exceptions.misusing.UnfinishedVerificationException;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test {@link MockBean} when mixed with Spring AOP.
 *
 * @author Phillip Webb
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5837">5837</a>
 */
@RunWith(SpringRunner.class)
public class MockBeanWithAopProxyAndNotProxyTargetAwareTests {

	@MockBean(proxyTargetAware = false)
	private DateService dateService;

	@Test(expected = UnfinishedVerificationException.class)
	public void verifyShouldUseProxyTarget() throws Exception {
		this.dateService.getDate();
		verify(this.dateService, times(1)).getDate();
		reset(this.dateService);
	}

	@Configuration
	@EnableCaching(proxyTargetClass = true)
	@Import(DateService.class)
	static class Config {

		@Bean
		public CacheResolver cacheResolver(CacheManager cacheManager) {
			SimpleCacheResolver resolver = new SimpleCacheResolver();
			resolver.setCacheManager(cacheManager);
			return resolver;
		}

		@Bean
		public ConcurrentMapCacheManager cacheManager() {
			ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
			cacheManager.setCacheNames(Arrays.asList("test"));
			return cacheManager;
		}

	}

	@Service
	static class DateService {

		@Cacheable(cacheNames = "test")
		public Long getDate() {
			return System.nanoTime();
		}

	}

}
