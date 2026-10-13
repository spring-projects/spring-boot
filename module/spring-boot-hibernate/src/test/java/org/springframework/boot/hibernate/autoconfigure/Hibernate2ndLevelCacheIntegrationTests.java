/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.ehcache.jsr107.EhcacheCachingProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Hibernate 2nd level cache with jcache.
 *
 * @author Stephane Nicoll
 */
class Hibernate2ndLevelCacheIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
		.withUserConfiguration(TestConfiguration.class);

	@Test
	void hibernate2ndLevelCacheWithJCacheAndEhCache() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		this.contextRunner
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.jpa.properties.hibernate.cache.region.factory_class=jcache",
					"spring.jpa.properties.hibernate.cache.provider=" + cachingProviderFqn)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class TestConfiguration {

		@Bean
		CacheManager cacheManager() {
			return Caching.getCachingProvider(EhcacheCachingProvider.class.getName()).getCacheManager();
		}

		@Bean
		JCacheCacheManager jcacheCacheManager(CacheManager cacheManager) {
			return new JCacheCacheManager(cacheManager);
		}

	}

}
