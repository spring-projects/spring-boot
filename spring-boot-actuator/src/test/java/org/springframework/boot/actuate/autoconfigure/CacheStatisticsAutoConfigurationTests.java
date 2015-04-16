/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;
import java.util.Arrays;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.CacheStatisticsAutoConfiguration;
import org.springframework.boot.actuate.cache.CacheStatistics;
import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link CacheStatisticsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CacheStatisticsAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	private CacheManager cacheManager;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void basicJCacheCacheStatistics() {
		load(JCacheCacheConfig.class);
		CacheStatisticsProvider provider = this.context.getBean(
				"jCacheStatisticsProvider", CacheStatisticsProvider.class);
		doTestCoreStatistics(provider, false);
	}

	@Test
	public void basicEhCacheCacheStatistics() {
		load(EhCacheConfig.class);
		CacheStatisticsProvider provider = this.context.getBean(
				"ehCacheCacheStatisticsProvider", CacheStatisticsProvider.class);
		doTestCoreStatistics(provider, true);
	}

	@Test
	public void basicHazelcastCacheStatistics() {
		load(HazelcastConfig.class);
		CacheStatisticsProvider provider = this.context.getBean(
				"hazelcastCacheStatisticsProvider", CacheStatisticsProvider.class);
		doTestCoreStatistics(provider, true);
	}

	@Test
	public void basicGuavaCacheStatistics() {
		load(GuavaConfig.class);
		CacheStatisticsProvider provider = this.context.getBean(
				"guavaCacheStatisticsProvider", CacheStatisticsProvider.class);
		doTestCoreStatistics(provider, true);
	}

	@Test
	public void concurrentMapCacheStatistics() {
		load(ConcurrentMapConfig.class);
		CacheStatisticsProvider provider = this.context.getBean(
				"concurrentMapCacheStatisticsProvider", CacheStatisticsProvider.class);
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(this.cacheManager,
				books);
		assertCoreStatistics(cacheStatistics, 0L, null, null);
		getOrCreate(books, "a", "b", "b", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(
				this.cacheManager, books);
		assertCoreStatistics(updatedCacheStatistics, 2L, null, null);
	}

	@Test
	public void noOpCacheStatistics() {
		load(NoOpCacheConfig.class);
		CacheStatisticsProvider provider = this.context.getBean(
				"noOpCacheStatisticsProvider", CacheStatisticsProvider.class);
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(this.cacheManager,
				books);
		assertCoreStatistics(cacheStatistics, null, null, null);
		getOrCreate(books, "a", "b", "b", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(
				this.cacheManager, books);
		assertCoreStatistics(updatedCacheStatistics, null, null, null);
	}

	private void doTestCoreStatistics(CacheStatisticsProvider provider,
			boolean supportSize) {
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(this.cacheManager,
				books);
		assertCoreStatistics(cacheStatistics, (supportSize ? 0L : null), null, null);
		getOrCreate(books, "a", "b", "b", "a", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(
				this.cacheManager, books);
		assertCoreStatistics(updatedCacheStatistics, (supportSize ? 2L : null), 0.66D,
				0.33D);
	}

	private void assertCoreStatistics(CacheStatistics metrics, Long size,
			Double hitRatio, Double missRatio) {
		assertNotNull("Cache metrics must not be null", metrics);
		assertEquals("Wrong size for metrics " + metrics, size, metrics.getSize());
		checkRatio("Wrong hit ratio for metrics " + metrics, hitRatio,
				metrics.getHitRatio());
		checkRatio("Wrong miss ratio for metrics " + metrics, missRatio,
				metrics.getMissRatio());
	}

	private void checkRatio(String message, Double expected, Double actual) {
		if (expected == null || actual == null) {
			assertEquals(message, expected, actual);
		}
		else {
			assertEquals(message, expected, actual, 0.01D);
		}
	}

	private void getOrCreate(Cache cache, String... ids) {
		for (String id : ids) {
			Cache.ValueWrapper wrapper = cache.get(id);
			if (wrapper == null) {
				cache.put(id, id);
			}
		}
	}

	private Cache getCache(String cacheName) {
		Cache cache = this.cacheManager.getCache(cacheName);
		Assert.notNull("No cache with name '" + cacheName + "' found.");
		return cache;
	}

	private void load(Class<?>... config) {
		this.context = new AnnotationConfigApplicationContext();
		if (config.length > 0) {
			this.context.register(config);
		}
		this.context.register(CacheStatisticsAutoConfiguration.class);
		this.context.refresh();
		this.cacheManager = this.context.getBean(CacheManager.class);
	}

	@Configuration
	static class JCacheCacheConfig {

		@Bean
		public JCacheCacheManager cacheManager() {
			javax.cache.CacheManager cacheManager = jCacheCacheManager();
			return new JCacheCacheManager(cacheManager);
		}

		@Bean
		public javax.cache.CacheManager jCacheCacheManager() {
			javax.cache.CacheManager cacheManager = Caching.getCachingProvider(
					HazelcastCachingProvider.class.getName()).getCacheManager();
			MutableConfiguration<Object, Object> config = new MutableConfiguration<Object, Object>();
			config.setStatisticsEnabled(true);
			cacheManager.createCache("books", config);
			cacheManager.createCache("speakers", config);
			return cacheManager;
		}

	}

	@Configuration
	static class EhCacheConfig {

		@Bean
		public EhCacheCacheManager cacheManager() {
			return new EhCacheCacheManager(ehCacheCacheManager());
		}

		@Bean
		public net.sf.ehcache.CacheManager ehCacheCacheManager() {
			return EhCacheManagerUtils.buildCacheManager(new ClassPathResource(
					"cache/test-ehcache.xml"));
		}

	}

	@Configuration
	static class HazelcastConfig {

		@Bean
		public HazelcastCacheManager cacheManager() throws IOException {
			return new HazelcastCacheManager(hazelcastInstance());
		}

		@Bean
		public HazelcastInstance hazelcastInstance() throws IOException {
			Resource resource = new ClassPathResource("cache/test-hazelcast.xml");
			Config cfg = new XmlConfigBuilder(resource.getURL()).build();
			return Hazelcast.newHazelcastInstance(cfg);
		}

	}

	@Configuration
	static class GuavaConfig {

		@Bean
		public GuavaCacheManager cacheManager() throws IOException {
			GuavaCacheManager cacheManager = new GuavaCacheManager();
			cacheManager.setCacheBuilder(CacheBuilder.newBuilder().recordStats());
			cacheManager.setCacheNames(Arrays.asList("books", "speakers"));
			return cacheManager;
		}

	}

	@Configuration
	static class ConcurrentMapConfig {

		@Bean
		public ConcurrentMapCacheManager cacheManager() {
			return new ConcurrentMapCacheManager("books", "speakers");
		}

	}

	@Configuration
	static class NoOpCacheConfig {

		@Bean
		public NoOpCacheManager cacheManager() {
			return new NoOpCacheManager();
		}

	}

}
