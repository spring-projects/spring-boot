/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.cache.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.jcache.embedded.JCachingProvider;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider;
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider.MockCacheManager;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.couchbase.CouchbaseClientFactory;
import org.springframework.data.couchbase.cache.CouchbaseCache;
import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration;
import org.springframework.data.couchbase.cache.CouchbaseCacheManager;
import org.springframework.data.redis.cache.FixedDurationTtlFunction;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link CacheAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Ryon Day
 */
class CacheAutoConfigurationTests extends AbstractCacheAutoConfigurationTests {

	@Test
	void noEnableCaching() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(CacheManager.class));
	}

	@Test
	void cacheManagerBackOff() {
		this.contextRunner.withUserConfiguration(CustomCacheManagerConfiguration.class)
			.run((context) -> assertThat(getCacheManager(context, ConcurrentMapCacheManager.class).getCacheNames())
				.containsOnly("custom1"));
	}

	@Test
	void cacheManagerFromSupportBackOff() {
		this.contextRunner.withUserConfiguration(CustomCacheManagerFromSupportConfiguration.class)
			.run((context) -> assertThat(getCacheManager(context, ConcurrentMapCacheManager.class).getCacheNames())
				.containsOnly("custom1"));
	}

	@Test
	void cacheResolverFromSupportBackOff() {
		this.contextRunner.withUserConfiguration(CustomCacheResolverFromSupportConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(CacheManager.class));
	}

	@Test
	void customCacheResolverCanBeDefined() {
		this.contextRunner.withUserConfiguration(SpecificCacheResolverConfiguration.class)
			.withPropertyValues("spring.cache.type=simple")
			.run((context) -> {
				getCacheManager(context, ConcurrentMapCacheManager.class);
				assertThat(context).hasSingleBean(CacheResolver.class);
			});
	}

	@Test
	void notSupportedCachingMode() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=foobar")
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.rootCause()
				.hasMessageContaining("No enum constant")
				.hasMessageContaining("foobar"));
	}

	@Test
	void simpleCacheExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=simple")
			.run((context) -> assertThat(getCacheManager(context, ConcurrentMapCacheManager.class).getCacheNames())
				.isEmpty());
	}

	@Test
	void simpleCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=simple")
			.run(verifyCustomizers("allCacheManagerCustomizer", "simpleCacheManagerCustomizer"));
	}

	@Test
	void simpleCacheExplicitWithCacheNames() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=simple", "spring.cache.cacheNames[0]=foo",
					"spring.cache.cacheNames[1]=bar")
			.run((context) -> {
				ConcurrentMapCacheManager cacheManager = getCacheManager(context, ConcurrentMapCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
			});
	}

	@Test
	void genericCacheWithCaches() {
		this.contextRunner.withUserConfiguration(GenericCacheConfiguration.class).run((context) -> {
			SimpleCacheManager cacheManager = getCacheManager(context, SimpleCacheManager.class);
			assertThat(cacheManager.getCache("first")).isEqualTo(context.getBean("firstCache"));
			assertThat(cacheManager.getCache("second")).isEqualTo(context.getBean("secondCache"));
			assertThat(cacheManager.getCacheNames()).hasSize(2);
		});
	}

	@Test
	void genericCacheExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=generic")
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("No cache manager could be auto-configured")
				.hasMessageContaining("GENERIC"));
	}

	@Test
	void genericCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(GenericCacheAndCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=generic")
			.run(verifyCustomizers("allCacheManagerCustomizer", "genericCacheManagerCustomizer"));
	}

	@Test
	void genericCacheExplicitWithCaches() {
		this.contextRunner.withUserConfiguration(GenericCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=generic")
			.run((context) -> {
				SimpleCacheManager cacheManager = getCacheManager(context, SimpleCacheManager.class);
				assertThat(cacheManager.getCache("first")).isEqualTo(context.getBean("firstCache"));
				assertThat(cacheManager.getCache("second")).isEqualTo(context.getBean("secondCache"));
				assertThat(cacheManager.getCacheNames()).hasSize(2);
			});
	}

	@Test
	void couchbaseCacheExplicit() {
		this.contextRunner.withUserConfiguration(CouchbaseConfiguration.class)
			.withPropertyValues("spring.cache.type=couchbase")
			.run((context) -> {
				CouchbaseCacheManager cacheManager = getCacheManager(context, CouchbaseCacheManager.class);
				assertThat(cacheManager.getCacheNames()).isEmpty();
			});
	}

	@Test
	void couchbaseCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(CouchbaseWithCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=couchbase")
			.run(verifyCustomizers("allCacheManagerCustomizer", "couchbaseCacheManagerCustomizer"));
	}

	@Test
	void couchbaseCacheExplicitWithCaches() {
		this.contextRunner.withUserConfiguration(CouchbaseConfiguration.class)
			.withPropertyValues("spring.cache.type=couchbase", "spring.cache.cacheNames[0]=foo",
					"spring.cache.cacheNames[1]=bar")
			.run((context) -> {
				CouchbaseCacheManager cacheManager = getCacheManager(context, CouchbaseCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				Cache cache = cacheManager.getCache("foo");
				assertThat(cache).isInstanceOf(CouchbaseCache.class);
				assertThat(((CouchbaseCache) cache).getCacheConfiguration().getExpiry()).hasSeconds(0);
			});
	}

	@Test
	void couchbaseCacheExplicitWithTtl() {
		this.contextRunner.withUserConfiguration(CouchbaseConfiguration.class)
			.withPropertyValues("spring.cache.type=couchbase", "spring.cache.cacheNames=foo,bar",
					"spring.cache.couchbase.expiration=2000")
			.run((context) -> {
				CouchbaseCacheManager cacheManager = getCacheManager(context, CouchbaseCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				Cache cache = cacheManager.getCache("foo");
				assertThat(cache).isInstanceOf(CouchbaseCache.class);
				assertThat(((CouchbaseCache) cache).getCacheConfiguration().getExpiry()).hasSeconds(2);
			});
	}

	@Test
	void couchbaseCacheWithCouchbaseCacheManagerBuilderCustomizer() {
		this.contextRunner.withUserConfiguration(CouchbaseConfiguration.class)
			.withPropertyValues("spring.cache.type=couchbase", "spring.cache.couchbase.expiration=15s")
			.withBean(CouchbaseCacheManagerBuilderCustomizer.class,
					() -> (builder) -> builder.cacheDefaults(CouchbaseCacheConfiguration.defaultCacheConfig()
						.entryExpiry(java.time.Duration.ofSeconds(10))))
			.run((context) -> {
				CouchbaseCacheManager cacheManager = getCacheManager(context, CouchbaseCacheManager.class);
				CouchbaseCacheConfiguration couchbaseCacheConfiguration = getDefaultCouchbaseCacheConfiguration(
						cacheManager);
				assertThat(couchbaseCacheConfiguration.getExpiry()).isEqualTo(java.time.Duration.ofSeconds(10));
			});
	}

	@Test
	void redisCacheExplicit() {
		this.contextRunner.withUserConfiguration(RedisConfiguration.class)
			.withPropertyValues("spring.cache.type=redis", "spring.cache.redis.time-to-live=15000",
					"spring.cache.redis.cacheNullValues=false", "spring.cache.redis.keyPrefix=prefix",
					"spring.cache.redis.useKeyPrefix=true")
			.run((context) -> {
				RedisCacheManager cacheManager = getCacheManager(context, RedisCacheManager.class);
				assertThat(cacheManager.getCacheNames()).isEmpty();
				RedisCacheConfiguration redisCacheConfiguration = getDefaultRedisCacheConfiguration(cacheManager);
				assertThat(redisCacheConfiguration).extracting(RedisCacheConfiguration::getTtlFunction)
					.isInstanceOf(FixedDurationTtlFunction.class)
					.extracting("duration")
					.isEqualTo(java.time.Duration.ofSeconds(15));
				assertThat(redisCacheConfiguration.getAllowCacheNullValues()).isFalse();
				assertThat(redisCacheConfiguration.getKeyPrefixFor("MyCache")).isEqualTo("prefixMyCache::");
				assertThat(redisCacheConfiguration.usePrefix()).isTrue();
			});
	}

	@Test
	void redisCacheWithRedisCacheConfiguration() {
		this.contextRunner.withUserConfiguration(RedisWithCacheConfigurationConfiguration.class)
			.withPropertyValues("spring.cache.type=redis", "spring.cache.redis.time-to-live=15000",
					"spring.cache.redis.keyPrefix=foo")
			.run((context) -> {
				RedisCacheManager cacheManager = getCacheManager(context, RedisCacheManager.class);
				assertThat(cacheManager.getCacheNames()).isEmpty();
				RedisCacheConfiguration redisCacheConfiguration = getDefaultRedisCacheConfiguration(cacheManager);
				assertThat(redisCacheConfiguration).extracting(RedisCacheConfiguration::getTtlFunction)
					.isInstanceOf(FixedDurationTtlFunction.class)
					.extracting("duration")
					.isEqualTo(java.time.Duration.ofSeconds(30));
				assertThat(redisCacheConfiguration.getKeyPrefixFor("")).isEqualTo("bar::");
			});
	}

	@Test
	void redisCacheWithRedisCacheManagerBuilderCustomizer() {
		this.contextRunner.withUserConfiguration(RedisWithRedisCacheManagerBuilderCustomizerConfiguration.class)
			.withPropertyValues("spring.cache.type=redis", "spring.cache.redis.time-to-live=15000")
			.run((context) -> {
				RedisCacheManager cacheManager = getCacheManager(context, RedisCacheManager.class);
				RedisCacheConfiguration redisCacheConfiguration = getDefaultRedisCacheConfiguration(cacheManager);
				assertThat(redisCacheConfiguration).extracting(RedisCacheConfiguration::getTtlFunction)
					.isInstanceOf(FixedDurationTtlFunction.class)
					.extracting("duration")
					.isEqualTo(java.time.Duration.ofSeconds(10));
			});
	}

	@Test
	void redisCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(RedisWithCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=redis")
			.run(verifyCustomizers("allCacheManagerCustomizer", "redisCacheManagerCustomizer"));
	}

	@Test
	void redisCacheExplicitWithCaches() {
		this.contextRunner.withUserConfiguration(RedisConfiguration.class)
			.withPropertyValues("spring.cache.type=redis", "spring.cache.cacheNames[0]=foo",
					"spring.cache.cacheNames[1]=bar")
			.run((context) -> {
				RedisCacheManager cacheManager = getCacheManager(context, RedisCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
				RedisCacheConfiguration redisCacheConfiguration = getDefaultRedisCacheConfiguration(cacheManager);
				assertThat(redisCacheConfiguration).extracting(RedisCacheConfiguration::getTtlFunction)
					.isInstanceOf(FixedDurationTtlFunction.class)
					.extracting("duration")
					.isEqualTo(java.time.Duration.ofSeconds(0));
				assertThat(redisCacheConfiguration.getAllowCacheNullValues()).isTrue();
				assertThat(redisCacheConfiguration.getKeyPrefixFor("test")).isEqualTo("test::");
				assertThat(redisCacheConfiguration.usePrefix()).isTrue();
			});
	}

	@Test
	void noOpCacheExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=none")
			.run((context) -> {
				NoOpCacheManager cacheManager = getCacheManager(context, NoOpCacheManager.class);
				assertThat(cacheManager.getCacheNames()).isEmpty();
			});
	}

	@Test
	void jCacheCacheNoProviderExplicit() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache")
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("No cache manager could be auto-configured")
				.hasMessageContaining("JCACHE"));
	}

	@Test
	void jCacheCacheWithProvider() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn)
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				assertThat(cacheManager.getCacheNames()).isEmpty();
				assertThat(context.getBean(javax.cache.CacheManager.class)).isEqualTo(cacheManager.getCacheManager());
			});
	}

	@Test
	void jCacheCacheWithCaches() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
			});
	}

	@Test
	void jCacheCacheWithCachesAndCustomConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(JCacheCustomConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.cacheNames[0]=one", "spring.cache.cacheNames[1]=two")
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				assertThat(cacheManager.getCacheNames()).containsOnly("one", "two");
				CompleteConfiguration<?, ?> defaultCacheConfiguration = context.getBean(CompleteConfiguration.class);
				MockCacheManager mockCacheManager = (MockCacheManager) cacheManager.getCacheManager();
				assertThat(mockCacheManager).isNotNull();
				assertThat(mockCacheManager.getConfigurations()).containsEntry("one", defaultCacheConfiguration)
					.containsEntry("two", defaultCacheConfiguration);
			});
	}

	@Test
	void jCacheCacheWithExistingJCacheManager() {
		this.contextRunner.withUserConfiguration(JCacheCustomCacheManager.class)
			.withPropertyValues("spring.cache.type=jcache")
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				assertThat(cacheManager.getCacheManager()).isEqualTo(context.getBean("customJCacheCacheManager"));
			});
	}

	@Test
	void jCacheCacheWithUnknownProvider() {
		String wrongCachingProviderClassName = "org.acme.FooBar";
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache",
					"spring.cache.jcache.provider=" + wrongCachingProviderClassName)
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining(wrongCachingProviderClassName));
	}

	@Test
	void jCacheCacheWithConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/cache/autoconfigure/hazelcast-specific.xml";
		this.contextRunner.withUserConfiguration(JCacheCustomConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.jcache.config=" + configLocation)
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				Resource configResource = new ClassPathResource(configLocation);
				javax.cache.CacheManager jCache = cacheManager.getCacheManager();
				assertThat(jCache).isNotNull();
				assertThat(jCache.getURI()).isEqualTo(configResource.getURI());
			});
	}

	@Test
	void jCacheCacheWithWrongConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		String configLocation = "org/springframework/boot/cache/autoconfigure/does-not-exist.xml";
		this.contextRunner.withUserConfiguration(JCacheCustomConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
					"spring.cache.jcache.config=" + configLocation)
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.hasMessageContaining("must exist")
				.hasMessageContaining(configLocation));
	}

	@Test
	void jCacheCacheUseBeanClassLoader() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn)
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				javax.cache.CacheManager jCache = cacheManager.getCacheManager();
				assertThat(jCache).isNotNull();
				assertThat(jCache.getClassLoader()).isEqualTo(context.getClassLoader());
			});
	}

	@Test
	void jCacheCacheWithPropertiesCustomizer() {
		JCachePropertiesCustomizer customizer = mock(JCachePropertiesCustomizer.class);
		willAnswer((invocation) -> {
			invocation.getArgument(0, Properties.class).setProperty("customized", "true");
			return null;
		}).given(customizer).customize(any(Properties.class));
		String cachingProviderFqn = MockCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn)
			.withBean(JCachePropertiesCustomizer.class, () -> customizer)
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				javax.cache.CacheManager jCache = cacheManager.getCacheManager();
				assertThat(jCache).isNotNull();
				assertThat(jCache.getProperties()).containsEntry("customized", "true");
			});
	}

	@Test
	@WithHazelcastXmlResource
	void hazelcastCacheExplicit() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class))
			.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=hazelcast")
			.run((context) -> {
				HazelcastCacheManager cacheManager = getCacheManager(context, HazelcastCacheManager.class);
				// NOTE: the hazelcast implementation knows about a cache in a lazy
				// manner.
				cacheManager.getCache("defaultCache");
				assertThat(cacheManager.getCacheNames()).containsOnly("defaultCache");
				assertThat(context.getBean(HazelcastInstance.class)).isEqualTo(cacheManager.getHazelcastInstance());
			});
	}

	@Test
	@WithHazelcastXmlResource
	void hazelcastCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(HazelcastCacheAndCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=hazelcast")
			.run(verifyCustomizers("allCacheManagerCustomizer", "hazelcastCacheManagerCustomizer"));
	}

	@Test
	void hazelcastCacheWithExistingHazelcastInstance() {
		this.contextRunner.withUserConfiguration(HazelcastCustomHazelcastInstance.class)
			.withPropertyValues("spring.cache.type=hazelcast")
			.run((context) -> {
				HazelcastCacheManager cacheManager = getCacheManager(context, HazelcastCacheManager.class);
				assertThat(cacheManager.getHazelcastInstance()).isEqualTo(context.getBean("customHazelcastInstance"));
			});
	}

	@Test
	void hazelcastCacheWithHazelcastAutoConfiguration() {
		String hazelcastConfig = "org/springframework/boot/cache/autoconfigure/hazelcast-specific.xml";
		this.contextRunner.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class))
			.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=hazelcast", "spring.hazelcast.config=" + hazelcastConfig)
			.run((context) -> {
				HazelcastCacheManager cacheManager = getCacheManager(context, HazelcastCacheManager.class);
				HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
				assertThat(cacheManager.getHazelcastInstance()).isSameAs(hazelcastInstance);
				assertThat(hazelcastInstance.getConfig().getConfigurationFile())
					.isEqualTo(new ClassPathResource(hazelcastConfig).getFile());
				assertThat(cacheManager.getCache("foobar")).isNotNull();
				assertThat(cacheManager.getCacheNames()).containsOnly("foobar");
			});
	}

	@Test
	void hazelcastAsJCacheWithCaches() {
		String cachingProviderFqn = HazelcastServerCachingProvider.class.getName();
		try {
			this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
					assertThat(cacheManager.getCacheNames()).containsOnly("foo", "bar");
					assertThat(Hazelcast.getAllHazelcastInstances()).hasSize(1);
				});
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	@WithResource(name = "hazelcast-specific.xml", content = """
			<hazelcast xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-5.0.xsd"
					   xmlns="http://www.hazelcast.com/schema/config"
					   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

				<queue name="foobar"/>

				<map name="foobar">
					<time-to-live-seconds>3600</time-to-live-seconds>
					<max-idle-seconds>600</max-idle-seconds>
				</map>

				<network>
					<join>
						<auto-detection enabled="false" />
						<multicast enabled="false"/>
					</join>
				</network>

			</hazelcast>
			""")
	void hazelcastAsJCacheWithConfig() {
		String cachingProviderFqn = HazelcastServerCachingProvider.class.getName();
		try {
			String configLocation = "hazelcast-specific.xml";
			this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.jcache.config=" + configLocation)
				.run((context) -> {
					JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
					Resource configResource = new ClassPathResource(configLocation);
					javax.cache.CacheManager jCache = cacheManager.getCacheManager();
					assertThat(jCache).isNotNull();
					assertThat(jCache.getURI()).isEqualTo(configResource.getURI());
					assertThat(Hazelcast.getAllHazelcastInstances()).hasSize(1);
				});
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	@WithHazelcastXmlResource
	void hazelcastAsJCacheWithExistingHazelcastInstance() {
		String cachingProviderFqn = HazelcastServerCachingProvider.class.getName();
		this.contextRunner.withConfiguration(AutoConfigurations.of(HazelcastAutoConfiguration.class))
			.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn)
			.run((context) -> {
				JCacheCacheManager cacheManager = getCacheManager(context, JCacheCacheManager.class);
				javax.cache.CacheManager jCacheManager = cacheManager.getCacheManager();
				assertThat(jCacheManager).isInstanceOf(com.hazelcast.cache.HazelcastCacheManager.class);
				assertThat(context).hasSingleBean(HazelcastInstance.class);
				HazelcastInstance hazelcastInstance = context.getBean(HazelcastInstance.class);
				assertThat(((com.hazelcast.cache.HazelcastCacheManager) jCacheManager).getHazelcastInstance())
					.isSameAs(hazelcastInstance);
				assertThat(hazelcastInstance.getName()).isEqualTo("default-instance");
				assertThat(Hazelcast.getAllHazelcastInstances()).hasSize(1);
			});
	}

	@Test
	@WithInfinispanXmlResource
	void infinispanCacheWithConfig() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=infinispan", "spring.cache.infinispan.config=infinispan.xml")
			.run((context) -> {
				SpringEmbeddedCacheManager cacheManager = getCacheManager(context, SpringEmbeddedCacheManager.class);
				assertThat(cacheManager.getCacheNames()).contains("foo", "bar");
			});
	}

	@Test
	void infinispanCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=infinispan")
			.run(verifyCustomizers("allCacheManagerCustomizer", "infinispanCacheManagerCustomizer"));
	}

	@Test
	void infinispanCacheWithCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=infinispan", "spring.cache.cacheNames[0]=foo",
					"spring.cache.cacheNames[1]=bar")
			.run((context) -> assertThat(getCacheManager(context, SpringEmbeddedCacheManager.class).getCacheNames())
				.containsOnly("foo", "bar"));
	}

	@Test
	void infinispanCacheWithCachesAndCustomConfig() {
		this.contextRunner.withUserConfiguration(InfinispanCustomConfiguration.class)
			.withPropertyValues("spring.cache.type=infinispan", "spring.cache.cacheNames[0]=foo",
					"spring.cache.cacheNames[1]=bar")
			.run((context) -> {
				assertThat(getCacheManager(context, SpringEmbeddedCacheManager.class).getCacheNames())
					.containsOnly("foo", "bar");
				then(context.getBean(ConfigurationBuilder.class)).should(times(2)).build();
			});
	}

	@Test
	void infinispanAsJCacheWithCaches() {
		String cachingProviderClassName = JCachingProvider.class.getName();
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderClassName,
					"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
			.run((context) -> assertThat(getCacheManager(context, JCacheCacheManager.class).getCacheNames())
				.containsOnly("foo", "bar"));
	}

	@Test
	@WithInfinispanXmlResource
	void infinispanAsJCacheWithConfig() {
		String cachingProviderClassName = JCachingProvider.class.getName();
		String configLocation = "infinispan.xml";
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderClassName,
					"spring.cache.jcache.config=" + configLocation)
			.run((context) -> {
				Resource configResource = new ClassPathResource(configLocation);
				javax.cache.CacheManager jCache = getCacheManager(context, JCacheCacheManager.class).getCacheManager();
				assertThat(jCache).isNotNull();
				assertThat(jCache.getURI()).isEqualTo(configResource.getURI());
			});
	}

	@Test
	void jCacheCacheWithCachesAndCustomizer() {
		String cachingProviderFqn = HazelcastServerCachingProvider.class.getName();
		try {
			this.contextRunner.withUserConfiguration(JCacheWithCustomizerConfiguration.class)
				.withPropertyValues("spring.cache.type=jcache", "spring.cache.jcache.provider=" + cachingProviderFqn,
						"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
				.run((context) ->
				// see customizer
				assertThat(getCacheManager(context, JCacheCacheManager.class).getCacheNames()).containsOnly("foo",
						"custom1"));
		}
		finally {
			Caching.getCachingProvider(cachingProviderFqn).close();
		}
	}

	@Test
	void cache2kCacheWithExplicitCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=cache2k", "spring.cache.cacheNames=foo,bar")
			.run((context) -> {
				SpringCache2kCacheManager manager = getCacheManager(context, SpringCache2kCacheManager.class);
				assertThat(manager.getCacheNames()).containsExactlyInAnyOrder("foo", "bar");
			});
	}

	@Test
	void cache2kCacheWithCustomizedDefaults() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=cache2k")
			.withBean(Cache2kBuilderCustomizer.class,
					() -> (builder) -> builder.valueType(String.class).loader((key) -> "default"))
			.run((context) -> {
				SpringCache2kCacheManager manager = getCacheManager(context, SpringCache2kCacheManager.class);
				assertThat(manager.getCacheNames()).isEmpty();
				Cache dynamic = manager.getCache("dynamic");
				assertThat(dynamic.get("1")).satisfies(hasEntry("default"));
				assertThat(dynamic.get("2")).satisfies(hasEntry("default"));
			});
	}

	@Test
	void cache2kCacheWithCustomizedDefaultsAndExplicitCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=cache2k", "spring.cache.cacheNames=foo,bar")
			.withBean(Cache2kBuilderCustomizer.class,
					() -> (builder) -> builder.valueType(String.class).loader((key) -> "default"))
			.run((context) -> {
				SpringCache2kCacheManager manager = getCacheManager(context, SpringCache2kCacheManager.class);
				assertThat(manager.getCacheNames()).containsExactlyInAnyOrder("foo", "bar");
				assertThat(manager.getCache("foo").get("1")).satisfies(hasEntry("default"));
				assertThat(manager.getCache("bar").get("1")).satisfies(hasEntry("default"));
			});
	}

	@Test
	void cache2kCacheWithCacheManagerCustomizer() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=cache2k")
			.withBean(CacheManagerCustomizer.class,
					() -> cache2kCacheManagerCustomizer((cacheManager) -> cacheManager.addCache("custom",
							(builder) -> builder.valueType(String.class).loader((key) -> "custom"))))
			.run((context) -> {
				SpringCache2kCacheManager manager = getCacheManager(context, SpringCache2kCacheManager.class);
				assertThat(manager.getCacheNames()).containsExactlyInAnyOrder("custom");
				assertThat(manager.getCache("custom").get("1")).satisfies(hasEntry("custom"));
			});
	}

	private CacheManagerCustomizer<SpringCache2kCacheManager> cache2kCacheManagerCustomizer(
			Consumer<SpringCache2kCacheManager> cacheManager) {
		return cacheManager::accept;
	}

	@Test
	void cache2kCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=cache2k")
			.run(verifyCustomizers("allCacheManagerCustomizer", "cache2kCacheManagerCustomizer"));
	}

	@Test
	void caffeineCacheWithExplicitCaches() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=caffeine", "spring.cache.cacheNames=foo")
			.run((context) -> {
				CaffeineCacheManager manager = getCacheManager(context, CaffeineCacheManager.class);
				assertThat(manager.getCacheNames()).containsOnly("foo");
				Cache foo = manager.getCache("foo");
				assertThat(foo).isNotNull();
				foo.get("1");
				// See next tests: no spec given so stats should be disabled
				assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount()).isZero();
			});
	}

	@Test
	void caffeineCacheWithCustomizers() {
		this.contextRunner.withUserConfiguration(DefaultCacheAndCustomizersConfiguration.class)
			.withPropertyValues("spring.cache.type=caffeine")
			.run(verifyCustomizers("allCacheManagerCustomizer", "caffeineCacheManagerCustomizer"));
	}

	@Test
	void caffeineCacheWithExplicitCacheBuilder() {
		this.contextRunner.withUserConfiguration(CaffeineCacheBuilderConfiguration.class)
			.withPropertyValues("spring.cache.type=caffeine", "spring.cache.cacheNames=foo,bar")
			.run(this::validateCaffeineCacheWithStats);
	}

	@Test
	void caffeineCacheExplicitWithSpec() {
		this.contextRunner.withUserConfiguration(CaffeineCacheSpecConfiguration.class)
			.withPropertyValues("spring.cache.type=caffeine", "spring.cache.cacheNames[0]=foo",
					"spring.cache.cacheNames[1]=bar")
			.run(this::validateCaffeineCacheWithStats);
	}

	@Test
	void caffeineCacheExplicitWithSpecString() {
		this.contextRunner.withUserConfiguration(DefaultCacheConfiguration.class)
			.withPropertyValues("spring.cache.type=caffeine", "spring.cache.caffeine.spec=recordStats",
					"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar")
			.run(this::validateCaffeineCacheWithStats);
	}

	@Test
	void autoConfiguredCacheManagerCanBeSwapped() {
		this.contextRunner.withUserConfiguration(CacheManagerPostProcessorConfiguration.class)
			.withPropertyValues("spring.cache.type=caffeine")
			.run((context) -> {
				getCacheManager(context, SimpleCacheManager.class);
				CacheManagerPostProcessor postProcessor = context.getBean(CacheManagerPostProcessor.class);
				assertThat(postProcessor.cacheManagers).hasSize(1);
				assertThat(postProcessor.cacheManagers.get(0)).isInstanceOf(CaffeineCacheManager.class);
			});
	}

	private Consumer<ValueWrapper> hasEntry(Object value) {
		return (valueWrapper) -> assertThat(valueWrapper.get()).isEqualTo(value);
	}

	private void validateCaffeineCacheWithStats(AssertableApplicationContext context) {
		CaffeineCacheManager manager = getCacheManager(context, CaffeineCacheManager.class);
		assertThat(manager.getCacheNames()).containsOnly("foo", "bar");
		Cache foo = manager.getCache("foo");
		assertThat(foo).isNotNull();
		foo.get("1");
		assertThat(((CaffeineCache) foo).getNativeCache().stats().missCount()).isOne();
	}

	private CouchbaseCacheConfiguration getDefaultCouchbaseCacheConfiguration(CouchbaseCacheManager cacheManager) {
		Object field = ReflectionTestUtils.getField(cacheManager, "defaultCacheConfig");
		assertThat(field).isNotNull();
		return (CouchbaseCacheConfiguration) field;
	}

	private RedisCacheConfiguration getDefaultRedisCacheConfiguration(RedisCacheManager cacheManager) {
		Object field = ReflectionTestUtils.getField(cacheManager, "defaultCacheConfiguration");
		assertThat(field).isNotNull();
		return (RedisCacheConfiguration) field;
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class DefaultCacheConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	@Import(CacheManagerCustomizersConfiguration.class)
	static class DefaultCacheAndCustomizersConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class GenericCacheConfiguration {

		@Bean
		Cache firstCache() {
			return new ConcurrentMapCache("first");
		}

		@Bean
		Cache secondCache() {
			return new ConcurrentMapCache("second");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ GenericCacheConfiguration.class, CacheManagerCustomizersConfiguration.class })
	static class GenericCacheAndCustomizersConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	@Import({ HazelcastAutoConfiguration.class, CacheManagerCustomizersConfiguration.class })
	static class HazelcastCacheAndCustomizersConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CouchbaseConfiguration {

		@Bean
		CouchbaseClientFactory couchbaseClientFactory() {
			return mock(CouchbaseClientFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ CouchbaseConfiguration.class, CacheManagerCustomizersConfiguration.class })
	static class CouchbaseWithCustomizersConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class RedisConfiguration {

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(RedisConfiguration.class)
	static class RedisWithCacheConfigurationConfiguration {

		@Bean
		org.springframework.data.redis.cache.RedisCacheConfiguration customRedisCacheConfiguration() {
			return org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(java.time.Duration.ofSeconds(30))
				.prefixCacheNameWith("bar");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(RedisConfiguration.class)
	static class RedisWithRedisCacheManagerBuilderCustomizerConfiguration {

		@Bean
		RedisCacheManagerBuilderCustomizer ttlRedisCacheManagerBuilderCustomizer() {
			return (builder) -> builder
				.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(java.time.Duration.ofSeconds(10)));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ RedisConfiguration.class, CacheManagerCustomizersConfiguration.class })
	static class RedisWithCustomizersConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class JCacheCustomConfiguration {

		@Bean
		CompleteConfiguration<?, ?> defaultCacheConfiguration() {
			return mock(CompleteConfiguration.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class JCacheCustomCacheManager {

		@Bean
		javax.cache.CacheManager customJCacheCacheManager() {
			javax.cache.CacheManager cacheManager = mock(javax.cache.CacheManager.class);
			given(cacheManager.getCacheNames()).willReturn(Collections.emptyList());
			return cacheManager;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class JCacheWithCustomizerConfiguration {

		@Bean
		JCacheManagerCustomizer myCustomizer() {
			return (cacheManager) -> {
				MutableConfiguration<?, ?> config = new MutableConfiguration<>();
				config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
				config.setStatisticsEnabled(true);
				cacheManager.createCache("custom1", config);
				cacheManager.destroyCache("bar");
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class HazelcastCustomHazelcastInstance {

		@Bean
		HazelcastInstance customHazelcastInstance() {
			return mock(HazelcastInstance.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class InfinispanCustomConfiguration {

		@Bean
		ConfigurationBuilder configurationBuilder() {
			ConfigurationBuilder builder = mock(ConfigurationBuilder.class);
			given(builder.build()).willReturn(new ConfigurationBuilder().build());
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CustomCacheManagerConfiguration {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("custom1");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CustomCacheManagerFromSupportConfiguration implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			// The @Bean annotation is important, see CachingConfigurerSupport Javadoc
			return new ConcurrentMapCacheManager("custom1");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CustomCacheResolverFromSupportConfiguration implements CachingConfigurer {

		@Override
		@Bean
		public CacheResolver cacheResolver() {
			// The @Bean annotation is important, see CachingConfigurerSupport Javadoc
			return (context) -> Collections.singleton(mock(Cache.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class SpecificCacheResolverConfiguration {

		@Bean
		CacheResolver myCacheResolver() {
			return mock(CacheResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CaffeineCacheBuilderConfiguration {

		@Bean
		Caffeine<Object, Object> cacheBuilder() {
			return Caffeine.newBuilder().recordStats();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CaffeineCacheSpecConfiguration {

		@Bean
		CaffeineSpec caffeineSpec() {
			return CaffeineSpec.parse("recordStats");
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CacheManagerPostProcessorConfiguration {

		@Bean
		static BeanPostProcessor cacheManagerBeanPostProcessor() {
			return new CacheManagerPostProcessor();
		}

	}

	static class CacheManagerPostProcessor implements BeanPostProcessor {

		private final List<CacheManager> cacheManagers = new ArrayList<>();

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof CacheManager cacheManager) {
				this.cacheManagers.add(cacheManager);
				return new SimpleCacheManager();
			}
			return bean;
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "infinispan.xml", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<infinispan>

				<!-- ************************************** -->
				<!-- Corresponds to @Cacheable("cache-name") -->
				<!-- ************************************** -->
				<cache-container default-cache="default">
					<local-cache name="default"/>
					<local-cache name="foo"/>
					<local-cache name="bar" />
				</cache-container>

			</infinispan>
			""")
	@interface WithInfinispanXmlResource {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "hazelcast.xml", content = """
			<hazelcast
				xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-config-5.0.xsd"
				xmlns="http://www.hazelcast.com/schema/config" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
				<instance-name>default-instance</instance-name>
				<map name="defaultCache" />
				<network>
					<join>
						<auto-detection enabled="false" />
						<multicast enabled="false" />
					</join>
				</network>
			</hazelcast>
			""")
	@interface WithHazelcastXmlResource {

	}

}
