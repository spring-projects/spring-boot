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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.spring.cache.CouchbaseCache;
import com.couchbase.client.spring.cache.CouchbaseCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.google.common.cache.CacheBuilder;
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import net.sf.ehcache.Status;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.jcache.embedded.JCachingProvider;
import org.infinispan.spring.provider.SpringEmbeddedCacheManager;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CacheAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class CacheAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noEnableCaching() {
		load(EmptyConfiguration.class);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(CacheManager.class);
	}

	@Test
	public void cacheManagerBackOff() {
		load(CustomCacheManagerConfiguration.class);
		ConcurrentMapCacheManager cacheManager = validateCacheManager(
				ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("custom1");
	}

	@Test
	public void cacheManagerFromSupportBackOff() {
		load(CustomCacheManagerFromSupportConfiguration.class);
		ConcurrentMapCacheManager cacheManager = validateCacheManager(
				ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("custom1");
	}

	@Test
	public void cacheResolverFromSupportBackOff() throws Exception {
		load(CustomCacheResolverFromSupportConfiguration.class);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(CacheManager.class);
	}

	@Test
	public void customCacheResolverCanBeDefined() throws Exception {
		load(SpecificCacheResolverConfiguration.class, "spring.cache.type=simple");
		validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(this.context.getBeansOfType(CacheResolver.class)).hasSize(1);
	}

	@Test
	public void notSupportedCachingMode() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("cache");
		this.thrown.expectMessage("foobar");
		load(DefaultCacheConfiguration.class, "spring.cache.type=foobar");
	}

	@Test
	public void simpleCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=simple");
		ConcurrentMapCacheManager cacheManager = validateCacheManager(
				ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames()).isEmpty();
	}

	@Test
	public void simpleCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "simple",
				"allCacheManagerCustomizer", "simpleCacheManagerCustomizer");
	}

	@Test
	public void simpleCacheExplicitWithCacheNames() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=simple",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		ConcurrentMapCacheManager cacheManager = validateCacheManager(
				ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void genericCacheWithCaches() {
		load(GenericCacheConfiguration.class);
		SimpleCacheManager cacheManager = validateCacheManager(SimpleCacheManager.class);
		assertThat(cacheManager.getCache("first"))
				.isEqualTo(this.context.getBean("firstCache"));
		assertThat(cacheManager.getCache("second"))
				.isEqualTo(this.context.getBean("secondCache"));
		assertThat(cacheManager.getCacheNames()).hasSize(2);
	}

	@Test
	public void genericCacheExplicit() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("No cache manager could be auto-configured");
		this.thrown.expectMessage("GENERIC");
		load(DefaultCacheConfiguration.class, "spring.cache.type=generic");
	}

	@Test
	public void genericCacheWithCustomizers() {
		testCustomizers(GenericCacheAndCustomizersConfiguration.class, "generic",
				"allCacheManagerCustomizer", "genericCacheManagerCustomizer");
	}

	@Test
	public void genericCacheExplicitWithCaches() {
		load(GenericCacheConfiguration.class, "spring.cache.type=generic");
		SimpleCacheManager cacheManager = validateCacheManager(SimpleCacheManager.class);
		assertThat(cacheManager.getCache("first"))
				.isEqualTo(this.context.getBean("firstCache"));
		assertThat(cacheManager.getCache("second"))
				.isEqualTo(this.context.getBean("secondCache"));
		assertThat(cacheManager.getCacheNames()).hasSize(2);
	}

	@Test
	public void couchbaseCacheExplicit() {
		load(CouchbaseCacheConfiguration.class, "spring.cache.type=couchbase");
		CouchbaseCacheManager cacheManager = validateCacheManager(
				CouchbaseCacheManager.class);
		assertThat(cacheManager.getCacheNames()).isEmpty();
	}

	@Test
	public void couchbaseCacheWithCustomizers() {
		testCustomizers(CouchbaseCacheAndCustomizersConfiguration.class, "couchbase",
				"allCacheManagerCustomizer", "couchbaseCacheManagerCustomizer");
	}

	@Test
	public void couchbaseCacheExplicitWithCaches() {
		load(CouchbaseCacheConfiguration.class, "spring.cache.type=couchbase",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		CouchbaseCacheManager cacheManager = validateCacheManager(
				CouchbaseCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		Cache cache = cacheManager.getCache("foo");
		assertThat(cache).isInstanceOf(CouchbaseCache.class);
		assertThat(((CouchbaseCache) cache).getTtl()).isEqualTo(0);
		assertThat(((CouchbaseCache) cache).getNativeCache())
				.isEqualTo(this.context.getBean("bucket"));
	}

	@Test
	public void couchbaseCacheExplicitWithTtl() {
		load(CouchbaseCacheConfiguration.class, "spring.cache.type=couchbase",
				"spring.cache.cacheNames=foo,bar",
				"spring.cache.couchbase.expiration=2000");
		CouchbaseCacheManager cacheManager = validateCacheManager(
				CouchbaseCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		Cache cache = cacheManager.getCache("foo");
		assertThat(cache).isInstanceOf(CouchbaseCache.class);
		assertThat(((CouchbaseCache) cache).getTtl()).isEqualTo(2000);
		assertThat(((CouchbaseCache) cache).getNativeCache())
				.isEqualTo(this.context.getBean("bucket"));
	}

	@Test
	public void redisCacheExplicit() {
		load(RedisCacheConfiguration.class, "spring.cache.type=redis");
		RedisCacheManager cacheManager = validateCacheManager(RedisCacheManager.class);
		assertThat(cacheManager.getCacheNames()).isEmpty();
		assertThat((Boolean) new DirectFieldAccessor(cacheManager)
				.getPropertyValue("usePrefix")).isTrue();
	}

	@Test
	public void redisCacheWithCustomizers() {
		testCustomizers(RedisCacheAndCustomizersConfiguration.class, "redis",
				"allCacheManagerCustomizer", "redisCacheManagerCustomizer");
	}

	@Test
	public void redisCacheExplicitWithCaches() {
		load(RedisCacheConfiguration.class, "spring.cache.type=redis",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		RedisCacheManager cacheManager = validateCacheManager(RedisCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void noOpCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=none");
		NoOpCacheManager cacheManager = validateCacheManager(NoOpCacheManager.class);
		assertThat(cacheManager.getCacheNames()).isEmpty();
	}

	@Test
	public void jCacheCacheNoProviderExplicit() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("No cache manager could be auto-configured");
		this.thrown.expectMessage("JCACHE");
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache");
	}

	@Test
	public void jCacheCacheWithProvider() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn);
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).isEmpty();
		assertThat(this.context.getBean(javax.cache.CacheManager.class))
				.isEqualTo(cacheManager.getCacheManager());
	}

	@Test
	public void jCacheCacheWithCaches() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void jCacheCacheWithCachesAndCustomConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(JCacheCustomConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=one", "spring.cache.cacheNames[1]=two");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("one", "two");
		CompleteConfiguration<?, ?> defaultCacheConfiguration = this.context
				.getBean(CompleteConfiguration.class);
		verify(cacheManager.getCacheManager()).createCache("one",
				defaultCacheConfiguration);
		verify(cacheManager.getCacheManager()).createCache("two",
				defaultCacheConfiguration);
	}

	@Test
	public void jCacheCacheWithExistingJCacheManager() {
		load(JCacheCustomCacheManager.class, "spring.cache.type=jcache");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheManager())
				.isEqualTo(this.context.getBean("customJCacheCacheManager"));
	}

	@Test
	public void jCacheCacheWithUnknownProvider() {
		String wrongCachingProviderFqn = "org.acme.FooBar";
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage(wrongCachingProviderFqn);
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + wrongCachingProviderFqn);
	}

	@Test
	public void jCacheCacheWithConfig() throws IOException {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/autoconfigure/cache/hazelcast-specific.xml";
		load(JCacheCustomConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.jcache.config=" + configLocation);
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		Resource configResource = new ClassPathResource(configLocation);
		assertThat(cacheManager.getCacheManager().getURI())
				.isEqualTo(configResource.getURI());
	}

	@Test
	public void jCacheCacheWithWrongConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/autoconfigure/cache/does-not-exist.xml";
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("does not exist");
		this.thrown.expectMessage(configLocation);
		load(JCacheCustomConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.jcache.config=" + configLocation);
	}

	@Test
	public void ehcacheCacheWithCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=ehcache");
		EhCacheCacheManager cacheManager = validateCacheManager(
				EhCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("cacheTest1", "cacheTest2");
		assertThat(this.context.getBean(net.sf.ehcache.CacheManager.class))
				.isEqualTo(cacheManager.getCacheManager());
	}

	@Test
	public void ehcacheCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "ehcache",
				"allCacheManagerCustomizer", "ehcacheCacheManagerCustomizer");
	}

	@Test
	public void ehcacheCacheWithConfig() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=ehcache",
				"spring.cache.ehcache.config=cache/ehcache-override.xml");
		EhCacheCacheManager cacheManager = validateCacheManager(
				EhCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("cacheOverrideTest1",
				"cacheOverrideTest2");
	}

	@Test
	public void ehcacheCacheWithExistingCacheManager() {
		load(EhCacheCustomCacheManager.class, "spring.cache.type=ehcache");
		EhCacheCacheManager cacheManager = validateCacheManager(
				EhCacheCacheManager.class);
		assertThat(cacheManager.getCacheManager())
				.isEqualTo(this.context.getBean("customEhCacheCacheManager"));
	}

	@Test
	public void ehcache3AsJCacheWithCaches() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void ehcache3AsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		String configLocation = "ehcache3.xml";
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.jcache.config=" + configLocation);
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);

		Resource configResource = new ClassPathResource(configLocation);
		assertThat(cacheManager.getCacheManager().getURI())
				.isEqualTo(configResource.getURI());
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void hazelcastCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=hazelcast");
		HazelcastCacheManager cacheManager = validateCacheManager(
				HazelcastCacheManager.class);
		// NOTE: the hazelcast implementation knows about a cache in a lazy manner.
		cacheManager.getCache("defaultCache");
		assertThat(cacheManager.getCacheNames()).containsOnly("defaultCache");
		assertThat(this.context.getBean(HazelcastInstance.class))
				.isEqualTo(getHazelcastInstance(cacheManager));
	}

	@Test
	public void hazelcastCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "hazelcast",
				"allCacheManagerCustomizer", "hazelcastCacheManagerCustomizer");
	}

	@Test
	public void hazelcastCacheWithConfig() throws IOException {
		load(DefaultCacheConfiguration.class, "spring.cache.type=hazelcast",
				"spring.cache.hazelcast.config=org/springframework/boot/autoconfigure/cache/hazelcast-specific.xml");
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		HazelcastCacheManager cacheManager = validateCacheManager(
				HazelcastCacheManager.class);
		HazelcastInstance actual = getHazelcastInstance(cacheManager);
		assertThat(actual).isSameAs(hazelcastInstance);
		assertThat(actual.getConfig().getConfigurationUrl())
				.isEqualTo(new ClassPathResource(
						"org/springframework/boot/autoconfigure/cache/hazelcast-specific.xml")
								.getURL());
		cacheManager.getCache("foobar");
		assertThat(cacheManager.getCacheNames()).containsOnly("foobar");
	}

	@Test
	public void hazelcastWithWrongConfig() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("foo/bar/unknown.xml");
		load(DefaultCacheConfiguration.class, "spring.cache.type=hazelcast",
				"spring.cache.hazelcast.config=foo/bar/unknown.xml");
	}

	@Test
	public void hazelcastCacheWithExistingHazelcastInstance() {
		load(HazelcastCustomHazelcastInstance.class, "spring.cache.type=hazelcast");
		HazelcastCacheManager cacheManager = validateCacheManager(
				HazelcastCacheManager.class);
		assertThat(getHazelcastInstance(cacheManager))
				.isEqualTo(this.context.getBean("customHazelcastInstance"));
	}

	@Test
	public void hazelcastCacheWithMainHazelcastAutoConfiguration() throws IOException {
		String mainConfig = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext,
				"spring.cache.type=hazelcast", "spring.hazelcast.config=" + mainConfig);
		applicationContext.register(DefaultCacheConfiguration.class);
		applicationContext.register(HazelcastAndCacheConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
		HazelcastCacheManager cacheManager = validateCacheManager(
				HazelcastCacheManager.class);
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		assertThat(getHazelcastInstance(cacheManager)).isEqualTo(hazelcastInstance);
		assertThat(hazelcastInstance.getConfig().getConfigurationFile())
				.isEqualTo(new ClassPathResource(mainConfig).getFile());
	}

	@Test
	public void hazelcastCacheWithMainHazelcastAutoConfigurationAndSeparateCacheConfig()
			throws IOException {
		String mainConfig = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
		String cacheConfig = "org/springframework/boot/autoconfigure/cache/hazelcast-specific.xml";
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext,
				"spring.cache.type=hazelcast",
				"spring.cache.hazelcast.config=" + cacheConfig,
				"spring.hazelcast.config=" + mainConfig);
		applicationContext.register(DefaultCacheConfiguration.class);
		applicationContext.register(HazelcastAndCacheConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
		HazelcastInstance hazelcastInstance = this.context
				.getBean(HazelcastInstance.class);
		HazelcastCacheManager cacheManager = validateCacheManager(
				HazelcastCacheManager.class);
		HazelcastInstance cacheHazelcastInstance = (HazelcastInstance) new DirectFieldAccessor(
				cacheManager).getPropertyValue("hazelcastInstance");
		assertThat(cacheHazelcastInstance).isNotEqualTo(hazelcastInstance); // Our custom
		assertThat(hazelcastInstance.getConfig().getConfigurationFile())
				.isEqualTo(new ClassPathResource(mainConfig).getFile());
		assertThat(cacheHazelcastInstance.getConfig().getConfigurationFile())
				.isEqualTo(new ClassPathResource(cacheConfig).getFile());
	}

	@Test
	public void hazelcastAsJCacheWithCaches() {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		try {
			load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
					"spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
			JCacheCacheManager cacheManager = validateCacheManager(
					JCacheCacheManager.class);
			assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	public void hazelcastAsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/autoconfigure/cache/hazelcast-specific.xml";
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.jcache.config=" + configLocation);
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);

		Resource configResource = new ClassPathResource(configLocation);
		assertThat(cacheManager.getCacheManager().getURI())
				.isEqualTo(configResource.getURI());
	}

	@Test
	public void infinispanCacheWithConfig() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=infinispan",
				"spring.cache.infinispan.config=infinispan.xml");
		SpringEmbeddedCacheManager cacheManager = validateCacheManager(
				SpringEmbeddedCacheManager.class);
		assertThat(cacheManager.getCacheNames()).contains("foo", "bar");
	}

	@Test
	public void infinispanCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "infinispan",
				"allCacheManagerCustomizer", "infinispanCacheManagerCustomizer");
	}

	@Test
	public void infinispanCacheWithCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=infinispan",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		SpringEmbeddedCacheManager cacheManager = validateCacheManager(
				SpringEmbeddedCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void infinispanCacheWithCachesAndCustomConfig() {
		load(InfinispanCustomConfiguration.class, "spring.cache.type=infinispan",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		SpringEmbeddedCacheManager cacheManager = validateCacheManager(
				SpringEmbeddedCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		ConfigurationBuilder defaultConfigurationBuilder = this.context
				.getBean(ConfigurationBuilder.class);
		verify(defaultConfigurationBuilder, times(2)).build();
	}

	@Test
	public void infinispanAsJCacheWithCaches() {
		String cachingProviderFqn = JCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
	}

	@Test
	public void infinispanAsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = JCachingProvider.class.getName();
		String configLocation = "infinispan.xml";
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.jcache.config=" + configLocation);
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);

		Resource configResource = new ClassPathResource(configLocation);
		assertThat(cacheManager.getCacheManager().getURI())
				.isEqualTo(configResource.getURI());
	}

	@Test
	public void jCacheCacheWithCachesAndCustomizer() {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		load(JCacheWithCustomizerConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		// see customizer
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "custom1");
	}

	@Test
	public void guavaCacheExplicitWithCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=guava",
				"spring.cache.cacheNames=foo");
		GuavaCacheManager cacheManager = validateCacheManager(GuavaCacheManager.class);
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		// See next tests: no spec given so stats should be disabled
		assertThat(((GuavaCache) foo).getNativeCache().stats().missCount()).isEqualTo(0L);
	}

	@Test
	public void guavaCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "guava",
				"allCacheManagerCustomizer", "guavaCacheManagerCustomizer");
	}

	@Test
	public void guavaCacheExplicitWithSpec() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=guava",
				"spring.cache.guava.spec=recordStats", "spring.cache.cacheNames[0]=foo",
				"spring.cache.cacheNames[1]=bar");
		validateGuavaCacheWithStats();
	}

	@Test
	public void guavaCacheExplicitWithCacheBuilder() {
		load(GuavaCacheBuilderConfiguration.class, "spring.cache.type=guava",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		validateGuavaCacheWithStats();
	}

	private void validateGuavaCacheWithStats() {
		GuavaCacheManager cacheManager = validateCacheManager(GuavaCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		assertThat(((GuavaCache) foo).getNativeCache().stats().missCount()).isEqualTo(1L);
	}

	@Test
	public void caffeineCacheWithExplicitCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=caffeine",
				"spring.cache.cacheNames=foo");
		CaffeineCacheManager cacheManager = validateCacheManager(
				CaffeineCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo");
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		// See next tests: no spec given so stats should be disabled
		assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount())
				.isEqualTo(0L);
	}

	@Test
	public void caffeineCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "caffeine",
				"allCacheManagerCustomizer", "caffeineCacheManagerCustomizer");
	}

	@Test
	public void caffeineCacheWithExplicitCacheBuilder() {
		load(CaffeineCacheBuilderConfiguration.class, "spring.cache.type=caffeine",
				"spring.cache.cacheNames=foo,bar");
		validateCaffeineCacheWithStats();
	}

	@Test
	public void caffeineCacheExplicitWithSpec() {
		load(CaffeineCacheSpecConfiguration.class, "spring.cache.type=caffeine",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		validateCaffeineCacheWithStats();
	}

	@Test
	public void caffeineCacheExplicitWithSpecString() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=caffeine",
				"spring.cache.caffeine.spec=recordStats",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		validateCaffeineCacheWithStats();
	}

	private void validateCaffeineCacheWithStats() {
		CaffeineCacheManager cacheManager = validateCacheManager(
				CaffeineCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount())
				.isEqualTo(1L);
	}

	private <T extends CacheManager> T validateCacheManager(Class<T> type) {
		CacheManager cacheManager = this.context.getBean(CacheManager.class);
		assertThat(cacheManager).as("Wrong cache manager type").isInstanceOf(type);
		return type.cast(cacheManager);
	}

	@SuppressWarnings("rawtypes")
	private void testCustomizers(Class<?> config, String cacheType,
			String... expectedCustomizerNames) {
		load(config, "spring.cache.type=" + cacheType);
		CacheManager cacheManager = validateCacheManager(CacheManager.class);
		List<String> expected = new ArrayList<String>();
		expected.addAll(Arrays.asList(expectedCustomizerNames));
		Map<String, CacheManagerTestCustomizer> map = this.context
				.getBeansOfType(CacheManagerTestCustomizer.class);
		for (Map.Entry<String, CacheManagerTestCustomizer> entry : map.entrySet()) {
			if (expected.contains(entry.getKey())) {
				expected.remove(entry.getKey());
				assertThat(entry.getValue().cacheManager).isSameAs(cacheManager);
			}
			else {
				assertThat(entry.getValue().cacheManager).isNull();
			}
		}
		assertThat(expected).hasSize(0);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(config);
		applicationContext.register(CacheAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	private static HazelcastInstance getHazelcastInstance(
			HazelcastCacheManager cacheManager) {
		return (HazelcastInstance) new DirectFieldAccessor(cacheManager)
				.getPropertyValue("hazelcastInstance");
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	@EnableCaching
	static class DefaultCacheConfiguration {

	}

	@Configuration
	@EnableCaching
	@Import(CacheManagerCustomizersConfiguration.class)
	static class DefaultCacheAndCustomizersConfiguration {

	}

	@Configuration
	@EnableCaching
	static class GenericCacheConfiguration {

		@Bean
		public Cache firstCache() {
			return new ConcurrentMapCache("first");
		}

		@Bean
		public Cache secondCache() {
			return new ConcurrentMapCache("second");
		}

	}

	@Configuration
	@Import({ GenericCacheConfiguration.class,
			CacheManagerCustomizersConfiguration.class })
	static class GenericCacheAndCustomizersConfiguration {
	}

	@Configuration
	@EnableCaching
	static class CouchbaseCacheConfiguration {

		@Bean
		public Bucket bucket() {
			BucketManager bucketManager = mock(BucketManager.class);
			Bucket bucket = mock(Bucket.class);
			given(bucket.bucketManager()).willReturn(bucketManager);
			return bucket;
		}

	}

	@Configuration
	@Import({ CouchbaseCacheConfiguration.class,
			CacheManagerCustomizersConfiguration.class })
	static class CouchbaseCacheAndCustomizersConfiguration {

	}

	@Configuration
	@EnableCaching
	static class RedisCacheConfiguration {

		@Bean
		public RedisTemplate<?, ?> redisTemplate() {
			return mock(RedisTemplate.class);
		}

	}

	@Configuration
	@Import({ RedisCacheConfiguration.class, CacheManagerCustomizersConfiguration.class })
	static class RedisCacheAndCustomizersConfiguration {

	}

	@Configuration
	@EnableCaching
	static class JCacheCustomConfiguration {

		@Bean
		public CompleteConfiguration<?, ?> defaultCacheConfiguration() {
			return mock(CompleteConfiguration.class);
		}

	}

	@Configuration
	@EnableCaching
	static class JCacheCustomCacheManager {

		@Bean
		public javax.cache.CacheManager customJCacheCacheManager() {
			javax.cache.CacheManager cacheManager = mock(javax.cache.CacheManager.class);
			given(cacheManager.getCacheNames())
					.willReturn(Collections.<String>emptyList());
			return cacheManager;
		}

	}

	@Configuration
	@EnableCaching
	static class JCacheWithCustomizerConfiguration {

		@Bean
		JCacheManagerCustomizer myCustomizer() {
			return new JCacheManagerCustomizer() {
				@Override
				public void customize(javax.cache.CacheManager cacheManager) {
					MutableConfiguration<?, ?> config = new MutableConfiguration<Object, Object>();
					config.setExpiryPolicyFactory(
							CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
					config.setStatisticsEnabled(true);
					cacheManager.createCache("custom1", config);
					cacheManager.destroyCache("bar");
				}
			};
		}

	}

	@Configuration
	@EnableCaching
	static class EhCacheCustomCacheManager {

		@Bean
		public net.sf.ehcache.CacheManager customEhCacheCacheManager() {
			net.sf.ehcache.CacheManager cacheManager = mock(
					net.sf.ehcache.CacheManager.class);
			given(cacheManager.getStatus()).willReturn(Status.STATUS_ALIVE);
			given(cacheManager.getCacheNames()).willReturn(new String[0]);
			return cacheManager;
		}

	}

	@Configuration
	@EnableCaching
	static class HazelcastCustomHazelcastInstance {

		@Bean
		public HazelcastInstance customHazelcastInstance() {
			return mock(HazelcastInstance.class);
		}

	}

	@Configuration
	@ImportAutoConfiguration({ CacheAutoConfiguration.class,
			HazelcastAutoConfiguration.class })
	static class HazelcastAndCacheConfiguration {

	}

	@Configuration
	@EnableCaching
	static class InfinispanCustomConfiguration {

		@Bean
		public ConfigurationBuilder configurationBuilder() {
			ConfigurationBuilder builder = mock(ConfigurationBuilder.class);
			given(builder.build()).willReturn(new ConfigurationBuilder().build());
			return builder;
		}

	}

	@Configuration
	@EnableCaching
	static class CustomCacheManagerConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("custom1");
		}

	}

	@Configuration
	@EnableCaching
	static class CustomCacheManagerFromSupportConfiguration
			extends CachingConfigurerSupport {

		@Override
		@Bean
		// The @Bean annotation is important, see CachingConfigurerSupport Javadoc
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("custom1");
		}

	}

	@Configuration
	@EnableCaching
	static class CustomCacheResolverFromSupportConfiguration
			extends CachingConfigurerSupport {

		@Override
		@Bean
		// The @Bean annotation is important, see CachingConfigurerSupport Javadoc
		public CacheResolver cacheResolver() {
			return new CacheResolver() {

				@Override
				public Collection<? extends Cache> resolveCaches(
						CacheOperationInvocationContext<?> context) {
					return Collections.singleton(mock(Cache.class));
				}

			};

		}

	}

	@Configuration
	@EnableCaching
	static class SpecificCacheResolverConfiguration {

		@Bean
		public CacheResolver myCacheResolver() {
			return mock(CacheResolver.class);
		}

	}

	@Configuration
	@EnableCaching
	static class GuavaCacheBuilderConfiguration {

		@Bean
		CacheBuilder<Object, Object> cacheBuilder() {
			return CacheBuilder.newBuilder().recordStats();
		}

	}

	@Configuration
	@EnableCaching
	static class CaffeineCacheBuilderConfiguration {

		@Bean
		Caffeine<Object, Object> cacheBuilder() {
			return Caffeine.newBuilder().recordStats();
		}

	}

	@Configuration
	@EnableCaching
	static class CaffeineCacheSpecConfiguration {

		@Bean
		CaffeineSpec caffeineSpec() {
			return CaffeineSpec.parse("recordStats");
		}

	}

	@Configuration
	static class CacheManagerCustomizersConfiguration {

		@Bean
		public CacheManagerCustomizer<CacheManager> allCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<CacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<ConcurrentMapCacheManager> simpleCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<ConcurrentMapCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<SimpleCacheManager> genericCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<SimpleCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<CouchbaseCacheManager> couchbaseCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<CouchbaseCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<RedisCacheManager> redisCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<RedisCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<EhCacheCacheManager> ehcacheCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<EhCacheCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<HazelcastCacheManager> hazelcastCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<HazelcastCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<SpringEmbeddedCacheManager> infinispanCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<SpringEmbeddedCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<GuavaCacheManager> guavaCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<GuavaCacheManager>() {
			};
		}

		@Bean
		public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
			return new CacheManagerTestCustomizer<CaffeineCacheManager>() {
			};
		}

	}

	static abstract class CacheManagerTestCustomizer<T extends CacheManager>
			implements CacheManagerCustomizer<T> {

		private T cacheManager;

		@Override
		public void customize(T cacheManager) {
			if (this.cacheManager != null) {
				throw new IllegalStateException("Customized invoked twice");
			}
			this.cacheManager = cacheManager;
		}

	}

}
