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
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
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
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

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
 * @author Mark Paluch
 * @author Ryon Day
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hazelcast-client-*.jar")
public class CacheAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class));

	@Test
	public void noEnableCaching() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class).run(
				(context) -> assertThat(context).doesNotHaveBean(CacheManager.class));
	}

	@Test
	public void cacheManagerBackOff() {
		this.contextRunner.withUserConfiguration(CustomCacheManagerConfiguration.class)
				.run((context) -> assertThat(
						getCacheManager(context, ConcurrentMapCacheManager.class)
								.getCacheNames()).containsOnly("custom1"));
	}

	@Test
	public void cacheManagerFromSupportBackOff() {
		this.contextRunner
				.withUserConfiguration(CustomCacheManagerFromSupportConfiguration.class)
				.run((context) -> assertThat(
						getCacheManager(context, ConcurrentMapCacheManager.class)
								.getCacheNames()).containsOnly("custom1"));
	}

	@Test
	public void cacheResolverFromSupportBackOff() throws Exception {
		this.contextRunner
				.withUserConfiguration(CustomCacheResolverFromSupportConfiguration.class)
				.run((context) -> assertThat(context)
						.doesNotHaveBean(CacheManager.class));
	}

	@Test
	public void customCacheResolverCanBeDefined() throws Exception {
		this.contextRunner.withUserConfiguration(SpecificCacheResolverConfiguration.class)
				.withPropertyValues("spring.cache.type=simple").run((context) -> {
					getCacheManager(context, ConcurrentMapCacheManager.class);
					assertThat(context).hasSingleBean(CacheResolver.class);
				});
	}

	@Test
	public void notSupportedCachingMode() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=foobar")
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class).hasMessageContaining(
								"Failed to bind properties under 'spring.cache.type'"));
	}

	@Test
	public void simpleCacheExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=simple")
				.run((context) -> assertThat(
						getCacheManager(context, ConcurrentMapCacheManager.class)
								.getCacheNames()).isEmpty());
	}

	@Test
	public void simpleCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "simple")
				.run(dunno("allCacheManagerCustomizer", "simpleCacheManagerCustomizer"));
	}

	@Test
	public void simpleCacheExplicitWithCacheNames() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=simple",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					ConcurrentMapCacheManager cacheManager = getCacheManager(context,
							ConcurrentMapCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void genericCacheWithCaches() {
		this.contextRunner.withUserConfiguration(GenericCacheConfiguration.class)
				.run((context) -> {
					SimpleCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=generic")
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("No cache manager could be auto-configured")
						.hasMessageContaining("GENERIC"));
	}

	@Test
	public void genericCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(GenericCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "generic")
				.run(dunno("allCacheManagerCustomizer", "genericCacheManagerCustomizer"));
	}

	@Test
	public void genericCacheExplicitWithCaches() {
		this.contextRunner.withUserConfiguration(GenericCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=generic").run((context) -> {
					SimpleCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner.withUserConfiguration(CouchbaseCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=couchbase").run((context) -> {
					CouchbaseCacheManager cacheManager = getCacheManager(context,
							CouchbaseCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
				});
	}

	@Test
	public void couchbaseCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(CouchbaseCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "couchbase").run(dunno(
						"allCacheManagerCustomizer", "couchbaseCacheManagerCustomizer"));
	}

	@Test
	public void couchbaseCacheExplicitWithCaches() {
		this.contextRunner.withUserConfiguration(CouchbaseCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=couchbase",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					CouchbaseCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner.withUserConfiguration(CouchbaseCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=couchbase",
						"spring.cache.cacheNames=foo,bar",
						"spring.cache.couchbase.expiration=2000")
				.run((context) -> {
					CouchbaseCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner.withUserConfiguration(RedisCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=redis",
						"spring.cache.redis.time-to-live=15000",
						"spring.cache.redis.cacheNullValues=false",
						"spring.cache.redis.keyPrefix=foo",
						"spring.cache.redis.useKeyPrefix=false")
				.run((context) -> {
					RedisCacheManager cacheManager = getCacheManager(context,
							RedisCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
					org.springframework.data.redis.cache.RedisCacheConfiguration redisCacheConfiguration = (org.springframework.data.redis.cache.RedisCacheConfiguration) new DirectFieldAccessor(
							cacheManager).getPropertyValue("defaultCacheConfig");
					assertThat(redisCacheConfiguration.getTtl())
							.isEqualTo(java.time.Duration.ofSeconds(15));
					assertThat(redisCacheConfiguration.getAllowCacheNullValues())
							.isFalse();
					assertThat(redisCacheConfiguration.getKeyPrefix()).contains("foo");
					assertThat(redisCacheConfiguration.usePrefix()).isFalse();
				});
	}

	@Test
	public void redisCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(RedisCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "redis")
				.run(dunno("allCacheManagerCustomizer", "redisCacheManagerCustomizer"));
	}

	@Test
	public void redisCacheExplicitWithCaches() {
		this.contextRunner.withUserConfiguration(RedisCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=redis",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					RedisCacheManager cacheManager = getCacheManager(context,
							RedisCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
					org.springframework.data.redis.cache.RedisCacheConfiguration redisCacheConfiguration = (org.springframework.data.redis.cache.RedisCacheConfiguration) new DirectFieldAccessor(
							cacheManager).getPropertyValue("defaultCacheConfig");
					assertThat(redisCacheConfiguration.getTtl())
							.isEqualTo(java.time.Duration.ofMinutes(0));
					assertThat(redisCacheConfiguration.getAllowCacheNullValues())
							.isTrue();
					assertThat(redisCacheConfiguration.getKeyPrefix()).isEmpty();
					assertThat(redisCacheConfiguration.usePrefix()).isTrue();
				});
	}

	@Test
	public void noOpCacheExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=none").run((context) -> {
					NoOpCacheManager cacheManager = getCacheManager(context,
							NoOpCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
				});
	}

	@Test
	public void jCacheCacheNoProviderExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache")
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("No cache manager could be auto-configured")
						.hasMessageContaining("JCACHE"));
	}

	@Test
	public void jCacheCacheWithProvider() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn)
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).isEmpty();
					assertThat(context.getBean(javax.cache.CacheManager.class))
							.isEqualTo(cacheManager.getCacheManager());
				});
	}

	@Test
	public void jCacheCacheWithCaches() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void jCacheCacheWithCachesAndCustomConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(JCacheCustomConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=one",
						"spring.cache.cacheNames[1]=two")
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner.withUserConfiguration(JCacheCustomCacheManager.class)
				.withPropertyValues("spring.cache.type=jcache").run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheManager())
							.isEqualTo(context.getBean("customJCacheCacheManager"));
				});
	}

	@Test
	public void jCacheCacheWithUnknownProvider() {
		String wrongCachingProviderClassName = "org.acme.FooBar";
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + wrongCachingProviderClassName)
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class)
						.hasMessageContaining(wrongCachingProviderClassName));
	}

	@Test
	public void jCacheCacheWithConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
		this.contextRunner.withUserConfiguration(JCacheCustomConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner.withUserConfiguration(JCacheCustomConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.run((context) -> assertThat(context).getFailure()
						.isInstanceOf(BeanCreationException.class)
						.hasMessageContaining("does not exist")
						.hasMessageContaining(configLocation));
	}

	@Test
	public void ehcacheCacheWithCaches() {
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
	public void ehcacheCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "ehcache")
				.run(dunno("allCacheManagerCustomizer", "ehcacheCacheManagerCustomizer"));
	}

	@Test
	public void ehcacheCacheWithConfig() {
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
	public void ehcacheCacheWithExistingCacheManager() {
		this.contextRunner.withUserConfiguration(EhCacheCustomCacheManager.class)
				.withPropertyValues("spring.cache.type=ehcache").run((context) -> {
					EhCacheCacheManager cacheManager = getCacheManager(context,
							EhCacheCacheManager.class);
					assertThat(cacheManager.getCacheManager())
							.isEqualTo(context.getBean("customEhCacheCacheManager"));
				});
	}

	@Test
	public void ehcache3AsJCacheWithCaches() {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
							JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void ehcache3AsJCacheWithConfig() throws IOException {
		String cachingProviderFqn = EhcacheCachingProvider.class.getName();
		String configLocation = "ehcache3.xml";
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
							JCacheCacheManager.class);

					Resource configResource = new ClassPathResource(configLocation);
					assertThat(cacheManager.getCacheManager().getURI())
							.isEqualTo(configResource.getURI());
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				});
	}

	@Test
	public void hazelcastCacheExplicit() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(HazelcastAutoConfiguration.class))
				.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=hazelcast").run((context) -> {
					HazelcastCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner
				.withUserConfiguration(HazelcastCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "hazelcast").run(dunno(
						"allCacheManagerCustomizer", "hazelcastCacheManagerCustomizer"));
	}

	@Test
	public void hazelcastCacheWithExistingHazelcastInstance() {
		this.contextRunner.withUserConfiguration(HazelcastCustomHazelcastInstance.class)
				.withPropertyValues("spring.cache.type=hazelcast").run((context) -> {
					HazelcastCacheManager cacheManager = getCacheManager(context,
							HazelcastCacheManager.class);
					assertThat(cacheManager.getHazelcastInstance())
							.isEqualTo(context.getBean("customHazelcastInstance"));
				});
	}

	@Test
	public void hazelcastCacheWithHazelcastAutoConfiguration() throws IOException {
		String hazelcastConfig = "org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml";
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(HazelcastAutoConfiguration.class))
				.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=hazelcast",
						"spring.hazelcast.config=" + hazelcastConfig)
				.run((context) -> {
					HazelcastCacheManager cacheManager = getCacheManager(context,
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
			this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
					.withPropertyValues("spring.cache.type=jcache",
							"spring.cache.jcache.provider=" + cachingProviderFqn,
							"spring.cache.cacheNames[0]=foo",
							"spring.cache.cacheNames[1]=bar")
					.run((context) -> {
						JCacheCacheManager cacheManager = getCacheManager(context,
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
			this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
					.withPropertyValues("spring.cache.type=jcache",
							"spring.cache.jcache.provider=" + cachingProviderFqn,
							"spring.cache.jcache.config=" + configLocation)
					.run((context) -> {
						JCacheCacheManager cacheManager = getCacheManager(context,
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
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(HazelcastAutoConfiguration.class))
				.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderFqn)
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context,
							JCacheCacheManager.class);
					javax.cache.CacheManager jCacheManager = cacheManager
							.getCacheManager();
					assertThat(jCacheManager).isInstanceOf(
							com.hazelcast.cache.HazelcastCacheManager.class);
					assertThat(context).hasSingleBean(HazelcastInstance.class);
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
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=infinispan",
						"spring.cache.infinispan.config=infinispan.xml")
				.run((context) -> {
					SpringEmbeddedCacheManager cacheManager = getCacheManager(context,
							SpringEmbeddedCacheManager.class);
					assertThat(cacheManager.getCacheNames()).contains("foo", "bar");
				});
	}

	@Test
	public void infinispanCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "infinispan").run(dunno(
						"allCacheManagerCustomizer", "infinispanCacheManagerCustomizer"));
	}

	@Test
	public void infinispanCacheWithCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=infinispan",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> assertThat(
						getCacheManager(context, SpringEmbeddedCacheManager.class)
								.getCacheNames()).containsOnly("foo", "bar"));
	}

	@Test
	public void infinispanCacheWithCachesAndCustomConfig() {
		this.contextRunner.withUserConfiguration(InfinispanCustomConfiguration.class)
				.withPropertyValues("spring.cache.type=infinispan",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					assertThat(getCacheManager(context, SpringEmbeddedCacheManager.class)
							.getCacheNames()).containsOnly("foo", "bar");
					verify(context.getBean(ConfigurationBuilder.class), times(2)).build();
				});
	}

	@Test
	public void infinispanAsJCacheWithCaches() {
		String cachingProviderClassName = JCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderClassName,
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run((context) -> assertThat(
						getCacheManager(context, JCacheCacheManager.class)
								.getCacheNames()).containsOnly("foo", "bar"));
	}

	@Test
	public void infinispanAsJCacheWithConfig() throws IOException {
		String cachingProviderClassName = JCachingProvider.class.getName();
		String configLocation = "infinispan.xml";
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache",
						"spring.cache.jcache.provider=" + cachingProviderClassName,
						"spring.cache.jcache.config=" + configLocation)
				.run((context) -> {
					Resource configResource = new ClassPathResource(configLocation);
					assertThat(getCacheManager(context, JCacheCacheManager.class)
							.getCacheManager().getURI())
									.isEqualTo(configResource.getURI());
				});
	}

	@Test
	public void jCacheCacheWithCachesAndCustomizer() {
		String cachingProviderClassName = HazelcastCachingProvider.class.getName();
		try {
			this.contextRunner
					.withUserConfiguration(JCacheWithCustomizerConfiguration.class)
					.withPropertyValues("spring.cache.type=jcache",
							"spring.cache.jcache.provider=" + cachingProviderClassName,
							"spring.cache.cacheNames[0]=foo",
							"spring.cache.cacheNames[1]=bar")
					.run((context) ->
					// see customizer
					assertThat(getCacheManager(context, JCacheCacheManager.class)
							.getCacheNames()).containsOnly("foo", "custom1"));
		}
		finally {
			Caching.getCachingProvider(cachingProviderClassName).close();
		}
	}

	@Test
	public void caffeineCacheWithExplicitCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=caffeine",
						"spring.cache.cacheNames=foo")
				.run((context) -> {
					CaffeineCacheManager manager = getCacheManager(context,
							CaffeineCacheManager.class);
					assertThat(manager.getCacheNames()).containsOnly("foo");
					Cache foo = manager.getCache("foo");
					foo.get("1");
					// See next tests: no spec given so stats should be disabled
					assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount())
							.isEqualTo(0L);
				});
	}

	@Test
	public void caffeineCacheWithCustomizers() {
		this.contextRunner
				.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
				.withPropertyValues("spring.cache.type=" + "caffeine").run(dunno(
						"allCacheManagerCustomizer", "caffeineCacheManagerCustomizer"));
	}

	@Test
	public void caffeineCacheWithExplicitCacheBuilder() {
		this.contextRunner.withUserConfiguration(CaffeineCacheBuilderConfiguration.class)
				.withPropertyValues("spring.cache.type=caffeine",
						"spring.cache.cacheNames=foo,bar")
				.run(this::validateCaffeineCacheWithStats);
	}

	@Test
	public void caffeineCacheExplicitWithSpec() {
		this.contextRunner.withUserConfiguration(CaffeineCacheSpecConfiguration.class)
				.withPropertyValues("spring.cache.type=caffeine",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run(this::validateCaffeineCacheWithStats);
	}

	@Test
	public void caffeineCacheExplicitWithSpecString() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=caffeine",
						"spring.cache.caffeine.spec=recordStats",
						"spring.cache.cacheNames[0]=foo",
						"spring.cache.cacheNames[1]=bar")
				.run(this::validateCaffeineCacheWithStats);
	}

	private void validateCaffeineCacheWithStats(AssertableApplicationContext context) {
		CaffeineCacheManager manager = getCacheManager(context,
				CaffeineCacheManager.class);
		assertThat(manager.getCacheNames()).containsOnly("foo", "bar");
		Cache foo = manager.getCache("foo");
		foo.get("1");
		assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount())
				.isEqualTo(1L);
	}

	@SuppressWarnings("rawtypes")
	private ContextConsumer<AssertableApplicationContext> dunno(
			String... expectedCustomizerNames) {
		return (context) -> {
			CacheManager cacheManager = getCacheManager(context, CacheManager.class);
			List<String> expected = new ArrayList<>(
					Arrays.asList(expectedCustomizerNames));
			Map<String, CacheManagerTestCustomizer> customizer = context
					.getBeansOfType(CacheManagerTestCustomizer.class);
			customizer.forEach((key, value) -> {
				if (expected.contains(key)) {
					expected.remove(key);
					assertThat(value.cacheManager).isSameAs(cacheManager);
				}
				else {
					assertThat(value.cacheManager).isNull();
				}
			});
			assertThat(expected).hasSize(0);
		};
	}

	private <T extends CacheManager> T getCacheManager(
			AssertableApplicationContext loaded, Class<T> type) {
		CacheManager cacheManager = loaded.getBean(CacheManager.class);
		assertThat(cacheManager).as("Wrong cache manager type").isInstanceOf(type);
		return type.cast(cacheManager);
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
		public RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
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
			given(cacheManager.getCacheNames()).willReturn(Collections.emptyList());
			return cacheManager;
		}

	}

	@Configuration
	@EnableCaching
	static class JCacheWithCustomizerConfiguration {

		@Bean
		JCacheManagerCustomizer myCustomizer() {
			return (cacheManager) -> {
				MutableConfiguration<?, ?> config = new MutableConfiguration<>();
				config.setExpiryPolicyFactory(
						CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
				config.setStatisticsEnabled(true);
				cacheManager.createCache("custom1", config);
				cacheManager.destroyCache("bar");
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
			return (context) -> Collections.singleton(mock(Cache.class));

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
