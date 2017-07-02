/*
 * Copyright 2012-2017 the original author or authors.
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
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import net.sf.ehcache.Status;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.jcache.embedded.JCachingProvider;
import org.infinispan.spring.provider.SpringEmbeddedCacheManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.test.context.ContextLoader;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
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
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hazelcast-client-*.jar")
public class CacheAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ContextLoader<AnnotationConfigApplicationContext> contextLoader = ContextLoader
			.standard().autoConfig(CacheAutoConfiguration.class);

	@Test
	public void noEnableCaching() {
		this.contextLoader.config(EmptyConfiguration.class).load(context -> {
			this.thrown.expect(NoSuchBeanDefinitionException.class);
			context.getBean(CacheManager.class);
		});
	}

	@Test
	public void cacheManagerBackOff() {
		this.contextLoader.config(CustomCacheManagerConfiguration.class).load(context -> {
			ConcurrentMapCacheManager cacheManager = validateCacheManager(context,
					ConcurrentMapCacheManager.class);
			assertThat(cacheManager.getCacheNames()).containsOnly("custom1");
		});
	}

	@Test
	public void cacheManagerFromSupportBackOff() {
		this.contextLoader.config(CustomCacheManagerFromSupportConfiguration.class)
				.load(context -> {
					ConcurrentMapCacheManager cacheManager = validateCacheManager(context,
							ConcurrentMapCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("custom1");
				});
	}

	@Test
	public void cacheResolverFromSupportBackOff() throws Exception {
		this.contextLoader.config(CustomCacheResolverFromSupportConfiguration.class)
				.load(context -> {
					this.thrown.expect(NoSuchBeanDefinitionException.class);
					context.getBean(CacheManager.class);
				});
	}

	@Test
	public void customCacheResolverCanBeDefined() throws Exception {
		this.contextLoader.config(SpecificCacheResolverConfiguration.class)
				.env("spring.cache.type=simple").load(context -> {
					validateCacheManager(context, ConcurrentMapCacheManager.class);
					assertThat(context.getBeansOfType(CacheResolver.class)).hasSize(1);
				});
	}

	@Test
	public void notSupportedCachingMode() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=foobar").loadAndFail(BeanCreationException.class,
						ex -> assertThat(ex.getMessage()).contains(
								"Failed to bind properties under 'spring.cache.type'"));
	}

	@Test
	public void simpleCacheExplicit() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=simple").load(context -> {
					ConcurrentMapCacheManager cacheManager = validateCacheManager(context,
							ConcurrentMapCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
				});
	}

	@Test
	public void simpleCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "simple",
				"allCacheManagerCustomizer", "simpleCacheManagerCustomizer");
	}

	@Test
	public void simpleCacheExplicitWithCacheNames() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=simple", "spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					ConcurrentMapCacheManager cacheManager = validateCacheManager(context,
							ConcurrentMapCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void genericCacheWithCaches() {
		this.contextLoader.config(GenericCacheConfiguration.class).load(context -> {
			SimpleCacheManager cacheManager = validateCacheManager(context,
					SimpleCacheManager.class);
			assertThat(cacheManager.getCache("first"))
					.isEqualTo(context.getBean("firstCache"));
			assertThat(cacheManager.getCache("second"))
					.isEqualTo(context.getBean("secondCache"));
			assertThat(cacheManager.getCacheNames()).hasSize(2);
		});
	}

	@Test
	public void genericCacheExplicit() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=generic").loadAndFail(BeanCreationException.class,
						ex -> assertThat(ex.getMessage()).contains(
								"No cache manager could be auto-configured", "GENERIC"));
	}

	@Test
	public void genericCacheWithCustomizers() {
		testCustomizers(GenericCacheAndCustomizersConfiguration.class, "generic",
				"allCacheManagerCustomizer", "genericCacheManagerCustomizer");
	}

	@Test
	public void genericCacheExplicitWithCaches() {
		this.contextLoader.config(GenericCacheConfiguration.class)
				.env("spring.cache.type=generic").load(context -> {
					SimpleCacheManager cacheManager = validateCacheManager(context,
							SimpleCacheManager.class);
					assertThat(cacheManager.getCache("first"))
							.isEqualTo(context.getBean("firstCache"));
					assertThat(cacheManager.getCache("second"))
							.isEqualTo(context.getBean("secondCache"));
					assertThat(cacheManager.getCacheNames()).hasSize(2);
				});
	}

	@Test
	public void couchbaseCacheExplicit() {
		this.contextLoader.config(CouchbaseCacheConfiguration.class)
				.env("spring.cache.type=couchbase").load(context -> {
					CouchbaseCacheManager cacheManager = validateCacheManager(context,
							CouchbaseCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
				});
	}

	@Test
	public void couchbaseCacheWithCustomizers() {
		testCustomizers(CouchbaseCacheAndCustomizersConfiguration.class, "couchbase",
				"allCacheManagerCustomizer", "couchbaseCacheManagerCustomizer");
	}

	@Test
	public void couchbaseCacheExplicitWithCaches() {
		this.contextLoader.config(CouchbaseCacheConfiguration.class)
				.env("spring.cache.type=couchbase", "spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					CouchbaseCacheManager cacheManager = validateCacheManager(context,
							CouchbaseCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
					Cache cache = cacheManager.getCache("foo");
					assertThat(cache).isInstanceOf(CouchbaseCache.class);
					assertThat(((CouchbaseCache) cache).getTtl()).isEqualTo(0);
					assertThat(((CouchbaseCache) cache).getNativeCache())
							.isEqualTo(context.getBean("bucket"));
				});
	}

	@Test
	public void couchbaseCacheExplicitWithTtl() {
		this.contextLoader.config(CouchbaseCacheConfiguration.class)
				.env("spring.cache.type=couchbase", "spring.cache.cacheNames=foo,bar",
						"spring.cache.couchbase.expiration=2000")
				.load(context -> {
					CouchbaseCacheManager cacheManager = validateCacheManager(context,
							CouchbaseCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
					Cache cache = cacheManager.getCache("foo");
					assertThat(cache).isInstanceOf(CouchbaseCache.class);
					assertThat(((CouchbaseCache) cache).getTtl()).isEqualTo(2);
					assertThat(((CouchbaseCache) cache).getNativeCache())
							.isEqualTo(context.getBean("bucket"));
				});
	}

	@Test
	public void redisCacheExplicit() {
		this.contextLoader.config(RedisCacheConfiguration.class)
				.env("spring.cache.type=redis").load(context -> {
					RedisCacheManager cacheManager = validateCacheManager(context,
							RedisCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
					assertThat((Boolean) new DirectFieldAccessor(cacheManager)
							.getPropertyValue("usePrefix")).isTrue();
				});
	}

	@Test
	public void redisCacheWithCustomizers() {
		testCustomizers(RedisCacheAndCustomizersConfiguration.class, "redis",
				"allCacheManagerCustomizer", "redisCacheManagerCustomizer");
	}

	@Test
	public void redisCacheExplicitWithCaches() {
		this.contextLoader.config(RedisCacheConfiguration.class)
				.env("spring.cache.type=redis", "spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					RedisCacheManager cacheManager = validateCacheManager(context,
							RedisCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void noOpCacheExplicit() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=none").load(context -> {
					NoOpCacheManager cacheManager = validateCacheManager(context,
							NoOpCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
				});
	}

	@Test
	public void jCacheCacheNoProviderExplicit() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache").loadAndFail(ex -> {
					assertThat(ex).isInstanceOf(BeanCreationException.class);
					assertThat(ex.getMessage()).contains(
							"No cache manager could be auto-configured", "JCACHE");
				});
	}

	@Test
	public void jCacheCacheWithProvider() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn)
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
					assertThat(context.getBean(javax.cache.CacheManager.class))
							.isEqualTo(cacheManager.getCacheManager());
				});
	}

	@Test
	public void jCacheCacheWithCaches() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void jCacheCacheWithCachesAndCustomConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextLoader.config(JCacheCustomConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=one",
						"spring.cache.cacheNames[1]=two")
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("one", "two");
					CompleteConfiguration<?, ?> defaultCacheConfiguration = context
							.getBean(CompleteConfiguration.class);
					verify(cacheManager.getCacheManager()).createCache("one",
							defaultCacheConfiguration);
					verify(cacheManager.getCacheManager()).createCache("two",
							defaultCacheConfiguration);
				});
	}

	@Test
	public void jCacheCacheWithExistingJCacheManager() {
		this.contextLoader.config(JCacheCustomCacheManager.class)
				.env("spring.cache.type=jcache").load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheManager())
							.isEqualTo(context.getBean("customJCacheCacheManager"));
				});
	}

	@Test
	public void jCacheCacheWithUnknownProvider() {
		String wrongCachingProviderFqn = "org.acme.FooBar";
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + wrongCachingProviderFqn)
				.loadAndFail(BeanCreationException.class, ex -> assertThat(
						ex.getMessage().contains(wrongCachingProviderFqn)));
	}

	@Test
	public void jCacheCacheWithConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
		this.contextLoader.config(JCacheCustomConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					Resource configResource = new ClassPathResource(configLocation);
					assertThat(cacheManager.getCacheManager().getURI())
							.isEqualTo(configResource.getURI());
				});
	}

	@Test
	public void jCacheCacheWithWrongConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/autoconfigure/cache/does-not-exist.xml";
		this.contextLoader.config(JCacheCustomConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.loadAndFail(BeanCreationException.class,
						ex -> assertThat(ex.getMessage()).contains("does not exist",
								configLocation));
	}

	@Test
	public void ehcacheCacheWithCaches() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=ehcache").load(context -> {
					EhCacheCacheManager cacheManager = validateCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("cacheTest1",
							"cacheTest2");
					assertThat(context.getBean(net.sf.ehcache.CacheManager.class))
							.isEqualTo(cacheManager.getCacheManager());
				});
	}

	@Test
	public void ehcacheCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "ehcache",
				"allCacheManagerCustomizer", "ehcacheCacheManagerCustomizer");
	}

	@Test
	public void ehcacheCacheWithConfig() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=ehcache",
						"spring.cache.ehcache.config=cache/ehcache-override.xml")
				.load(context -> {
					EhCacheCacheManager cacheManager = validateCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames())
							.containsOnly("cacheOverrideTest1", "cacheOverrideTest2");
				});
	}

	@Test
	public void ehcacheCacheWithExistingCacheManager() {
		this.contextLoader.config(EhCacheCustomCacheManager.class)
				.env("spring.cache.type=ehcache").load(context -> {
					EhCacheCacheManager cacheManager = validateCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheManager())
							.isEqualTo(context.getBean("customEhCacheCacheManager"));
				});
	}

	@Test
	public void ehcache3AsJCacheWithCaches() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void ehcache3AsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		String configLocation = "ehcache3.xml";
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);

					Resource configResource = new ClassPathResource(configLocation);
					assertThat(cacheManager.getCacheManager().getURI())
							.isEqualTo(configResource.getURI());
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void hazelcastCacheExplicit() {
		this.contextLoader.autoConfigFirst(HazelcastAutoConfiguration.class)
				.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=hazelcast").load(context -> {
					HazelcastCacheManager cacheManager = validateCacheManager(context,
							HazelcastCacheManager.class);
					// NOTE: the hazelcast implementation knows about a cache in a lazy
					// manner.
					cacheManager.getCache("defaultCache");
					assertThat(cacheManager.getCacheNames()).containsOnly("defaultCache");
					assertThat(context.getBean(HazelcastInstance.class))
							.isEqualTo(cacheManager.getHazelcastInstance());
				});
	}

	@Test
	public void hazelcastCacheWithCustomizers() {
		testCustomizers(HazelcastCacheAndCustomizersConfiguration.class, "hazelcast",
				"allCacheManagerCustomizer", "hazelcastCacheManagerCustomizer");
	}

	@Test
	public void hazelcastCacheWithExistingHazelcastInstance() {
		this.contextLoader.config(HazelcastCustomHazelcastInstance.class)
				.env("spring.cache.type=hazelcast").load(context -> {
					HazelcastCacheManager cacheManager = validateCacheManager(context,
							HazelcastCacheManager.class);
					assertThat(cacheManager.getHazelcastInstance())
							.isEqualTo(context.getBean("customHazelcastInstance"));
				});
	}

	@Test
	public void hazelcastCacheWithHazelcastAutoConfiguration() throws IOException {
		String hazelcastConfig = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
		this.contextLoader.autoConfigFirst(HazelcastAutoConfiguration.class)
				.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=hazelcast",
						"spring.hazelcast.config=" + hazelcastConfig)
				.load(context -> {
					HazelcastCacheManager cacheManager = validateCacheManager(context,
							HazelcastCacheManager.class);
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(cacheManager.getHazelcastInstance())
							.isSameAs(hazelcastInstance);
					assertThat(hazelcastInstance.getConfig().getConfigurationFile())
							.isEqualTo(new ClassPathResource(hazelcastConfig).getFile());
					assertThat(cacheManager.getCache("foobar")).isNotNull();
					assertThat(cacheManager.getCacheNames()).containsOnly("foobar");
				});
	}

	@Test
	public void hazelcastAsJCacheWithCaches() {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		try {
			this.contextLoader.config(DefaultCacheConfiguration.class)
					.env("spring.cache.type=jcache",
							"spring.cache.jcache.provider=" + cachingProviderFqn,
							"spring.cache.cacheNames[0]=foo",
							"spring.cache.cacheNames[1]=bar")
					.load(context -> {
						JCacheCacheManager cacheManager = validateCacheManager(context,
								JCacheCacheManager.class);
						assertThat(cacheManager.getCacheNames()).containsOnly("foo",
								"bar");
						assertThat(Hazelcast.getAllHazelcastInstances()).hasSize(1);
					});
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	public void hazelcastAsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		try {
			String configLocation = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
			this.contextLoader.config(DefaultCacheConfiguration.class)
					.env("spring.cache.type=jcache",
							"spring.cache.jcache.provider=" + cachingProviderFqn,
							"spring.cache.jcache.config=" + configLocation)
					.load(context -> {
						JCacheCacheManager cacheManager = validateCacheManager(context,
								JCacheCacheManager.class);

						Resource configResource = new ClassPathResource(configLocation);
						assertThat(cacheManager.getCacheManager().getURI())
								.isEqualTo(configResource.getURI());
						assertThat(Hazelcast.getAllHazelcastInstances()).hasSize(1);
					});
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	public void hazelcastAsJCacheWithExistingHazelcastInstance() throws IOException {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		this.contextLoader.autoConfig(HazelcastAutoConfiguration.class)
				.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn)
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					javax.cache.CacheManager jCacheManager = cacheManager
							.getCacheManager();
					assertThat(jCacheManager).isInstanceOf(
							com.hazelcast.cache.HazelcastCacheManager.class);
					assertThat(context.getBeansOfType(HazelcastInstance.class))
							.hasSize(1);
					HazelcastInstance hazelcastInstance = context
							.getBean(HazelcastInstance.class);
					assertThat(((com.hazelcast.cache.HazelcastCacheManager) jCacheManager)
							.getHazelcastInstance()).isSameAs(hazelcastInstance);
					assertThat(hazelcastInstance.getName()).isEqualTo("default-instance");
					assertThat(Hazelcast.getAllHazelcastInstances()).hasSize(1);
				});
	}

	@Test
	public void infinispanCacheWithConfig() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=infinispan",
						"spring.cache.infinispan.config=infinispan.xml")
				.load(context -> {
					SpringEmbeddedCacheManager cacheManager = validateCacheManager(
							context, SpringEmbeddedCacheManager.class);
					assertThat(cacheManager.getCacheNames()).contains("foo", "bar");
				});
	}

	@Test
	public void infinispanCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "infinispan",
				"allCacheManagerCustomizer", "infinispanCacheManagerCustomizer");
	}

	@Test
	public void infinispanCacheWithCaches() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=infinispan", "spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					SpringEmbeddedCacheManager cacheManager = validateCacheManager(
							context, SpringEmbeddedCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void infinispanCacheWithCachesAndCustomConfig() {
		this.contextLoader.config(InfinispanCustomConfiguration.class)
				.env("spring.cache.type=infinispan", "spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					SpringEmbeddedCacheManager cacheManager = validateCacheManager(
							context, SpringEmbeddedCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
					ConfigurationBuilder defaultConfigurationBuilder = context
							.getBean(ConfigurationBuilder.class);
					verify(defaultConfigurationBuilder, times(2)).build();
				});
	}

	@Test
	public void infinispanAsJCacheWithCaches() {
		String cachingProviderFqn = JCachingProvider.class.getName();
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void infinispanAsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = JCachingProvider.class.getName();
		String configLocation = "infinispan.xml";
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.load(context -> {
					JCacheCacheManager cacheManager = validateCacheManager(context,
							JCacheCacheManager.class);

					Resource configResource = new ClassPathResource(configLocation);
					assertThat(cacheManager.getCacheManager().getURI())
							.isEqualTo(configResource.getURI());
				});
	}

	@Test
	public void jCacheCacheWithCachesAndCustomizer() {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		try {
			this.contextLoader.config(JCacheWithCustomizerConfiguration.class)
					.env("spring.cache.type=jcache",
							"spring.cache.jcache.provider=" + cachingProviderFqn,
							"spring.cache.cacheNames[0]=foo",
							"spring.cache.cacheNames[1]=bar")
					.load(context -> {
						JCacheCacheManager cacheManager = validateCacheManager(context,
								JCacheCacheManager.class);
						// see customizer
						assertThat(cacheManager.getCacheNames()).containsOnly("foo",
								"custom1");
					});
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	public void caffeineCacheWithExplicitCaches() {
		this.contextLoader.config(DefaultCacheConfiguration.class)
				.env("spring.cache.type=caffeine", "spring.cache.cacheNames=foo")
				.load(context -> {
					CaffeineCacheManager cacheManager = validateCacheManager(context,
							CaffeineCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo");
					Cache foo = cacheManager.getCache("foo");
					foo.get("1");
					// See next tests: no spec given so stats should be disabled
					assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount())
							.isEqualTo(0L);
				});
	}

	@Test
	public void caffeineCacheWithCustomizers() {
		testCustomizers(DefaultCacheAndCustomizersConfiguration.class, "caffeine",
				"allCacheManagerCustomizer", "caffeineCacheManagerCustomizer");
	}

	@Test
	public void caffeineCacheWithExplicitCacheBuilder() {
		this.contextLoader.config(CaffeineCacheBuilderConfiguration.class)
				.env("spring.cache.type=caffeine", "spring.cache.cacheNames=foo,bar")
				.load(this::validateCaffeineCacheWithStats);
	}

	@Test
	public void caffeineCacheExplicitWithSpec() {
		this.contextLoader.config(CaffeineCacheSpecConfiguration.class)
				.env("spring.cache.type=caffeine", "spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.load(this::validateCaffeineCacheWithStats);
	}

	@Test
	public void caffeineCacheExplicitWithSpecString() {
		this.contextLoader.config(DefaultCacheConfiguration.class).env(
				"spring.cache.type=caffeine", "spring.cache.caffeine.spec=recordStats",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
				.load(this::validateCaffeineCacheWithStats);
	}

	private void validateCaffeineCacheWithStats(ConfigurableApplicationContext context) {
		CaffeineCacheManager cacheManager = validateCacheManager(context,
				CaffeineCacheManager.class);
		assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount())
				.isEqualTo(1L);
	}

	private <T extends CacheManager> T validateCacheManager(
			ConfigurableApplicationContext context, Class<T> type) {
		CacheManager cacheManager = context.getBean(CacheManager.class);
		assertThat(cacheManager).as("Wrong cache manager type").isInstanceOf(type);
		return type.cast(cacheManager);
	}

	@SuppressWarnings("rawtypes")
	private void testCustomizers(Class<?> config, String cacheType,
			String... expectedCustomizerNames) {
		this.contextLoader.config(config).env("spring.cache.type=" + cacheType)
				.load(context -> {
					CacheManager cacheManager = validateCacheManager(context,
							CacheManager.class);
					List<String> expected = new ArrayList<>();
					expected.addAll(Arrays.asList(expectedCustomizerNames));
					Map<String, CacheManagerTestCustomizer> map = context
							.getBeansOfType(CacheManagerTestCustomizer.class);
					for (Map.Entry<String, CacheManagerTestCustomizer> entry : map
							.entrySet()) {
						if (expected.contains(entry.getKey())) {
							expected.remove(entry.getKey());
							assertThat(entry.getValue().cacheManager)
									.isSameAs(cacheManager);
						}
						else {
							assertThat(entry.getValue().cacheManager).isNull();
						}
					}
					assertThat(expected).hasSize(0);
				});
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
	@Import({ HazelcastAutoConfiguration.class,
			CacheManagerCustomizersConfiguration.class })
	static class HazelcastCacheAndCustomizersConfiguration {

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
					MutableConfiguration<?, ?> config = new MutableConfiguration<>();
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
