/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureCache @AutoConfigureCache} with an existing
 * {@link CacheManager}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
@AutoConfigureCache
class AutoConfigureCacheWithExistingCacheManagerIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldNotReplaceExistingCacheManager() {
		CacheManager bean = this.applicationContext.getBean(CacheManager.class);
		assertThat(bean).isInstanceOf(ConcurrentMapCacheManager.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class Config {

		@Bean
		ConcurrentMapCacheManager existingCacheManager() {
			return new ConcurrentMapCacheManager();
		}

	}

}
