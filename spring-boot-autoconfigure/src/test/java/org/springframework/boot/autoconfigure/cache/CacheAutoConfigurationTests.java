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

package org.springframework.boot.autoconfigure.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import com.google.common.cache.CacheBuilder;
import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import net.sf.ehcache.Status;
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
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), contains("custom1"));
		assertThat(cacheManager.getCacheNames(), hasSize(1));
	}

	@Test
	public void cacheManagerFromSupportBackOff() {
		load(CustomCacheManagerFromSupportConfiguration.class);
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), contains("custom1"));
		assertThat(cacheManager.getCacheNames(), hasSize(1));
	}

	@Test
	public void cacheResolverBackOff() throws Exception {
		load(CustomCacheResolverConfiguration.class);
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(CacheManager.class);
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
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test
	public void simpleCacheExplicitWithCacheNames() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=simple",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void genericCacheWithCaches() {
		load(GenericCacheConfiguration.class);
		SimpleCacheManager cacheManager = validateCacheManager(SimpleCacheManager.class);
		assertThat(cacheManager.getCache("first"),
				equalTo(this.context.getBean("firstCache")));
		assertThat(cacheManager.getCache("second"),
				equalTo(this.context.getBean("secondCache")));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void genericCacheExplicit() {
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("No cache manager could be auto-configured");
		this.thrown.expectMessage("GENERIC");
		load(DefaultCacheConfiguration.class, "spring.cache.type=generic");
	}

	@Test
	public void genericCacheExplicitWithCaches() {
		load(GenericCacheConfiguration.class, "spring.cache.type=generic");
		SimpleCacheManager cacheManager = validateCacheManager(SimpleCacheManager.class);
		assertThat(cacheManager.getCache("first"),
				equalTo(this.context.getBean("firstCache")));
		assertThat(cacheManager.getCache("second"),
				equalTo(this.context.getBean("secondCache")));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void redisCacheExplicit() {
		load(RedisCacheConfiguration.class, "spring.cache.type=redis");
		RedisCacheManager cacheManager = validateCacheManager(RedisCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test
	public void redisCacheExplicitWithCaches() {
		load(RedisCacheConfiguration.class, "spring.cache.type=redis",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		RedisCacheManager cacheManager = validateCacheManager(RedisCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void noOpCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=none");
		NoOpCacheManager cacheManager = validateCacheManager(NoOpCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
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
		assertThat(cacheManager.getCacheNames(), is(empty()));
		assertThat(this.context.getBean(javax.cache.CacheManager.class),
				is(cacheManager.getCacheManager()));
	}

	@Test
	public void jCacheCacheWithCaches() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void jCacheCacheWithCachesAndCustomConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(JCacheCustomConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=one", "spring.cache.cacheNames[1]=two");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("one", "two"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));

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
		assertThat(cacheManager.getCacheManager(), is(this.context.getBean("customJCacheCacheManager")));
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
		assertThat(cacheManager.getCacheManager().getURI(), is(configResource.getURI()));
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
	public void ehCacheCacheWithCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=ehcache");
		EhCacheCacheManager cacheManager = validateCacheManager(EhCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(),
				containsInAnyOrder("cacheTest1", "cacheTest2"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
		assertThat(this.context.getBean(net.sf.ehcache.CacheManager.class),
				is(cacheManager.getCacheManager()));
	}

	@Test
	public void ehCacheCacheWithConfig() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=ehcache",
				"spring.cache.ehcache.config=cache/ehcache-override.xml");
		EhCacheCacheManager cacheManager = validateCacheManager(EhCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(),
				containsInAnyOrder("cacheOverrideTest1", "cacheOverrideTest2"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void ehCacheCacheWithExistingCacheManager() {
		load(EhCacheCustomCacheManager.class, "spring.cache.type=ehcache");
		EhCacheCacheManager cacheManager = validateCacheManager(EhCacheCacheManager.class);
		assertThat(cacheManager.getCacheManager(), is(this.context.getBean("customEhCacheCacheManager")));
	}

	@Test
	public void hazelcastCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=hazelcast");
		HazelcastCacheManager cacheManager = validateCacheManager(HazelcastCacheManager.class);
		// NOTE: the hazelcast implementation knows about a cache in a lazy manner.
		cacheManager.getCache("defaultCache");
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("defaultCache"));
		assertThat(cacheManager.getCacheNames(), hasSize(1));
		assertThat(this.context.getBean(HazelcastInstance.class),
				is(new DirectFieldAccessor(cacheManager).getPropertyValue("hazelcastInstance")));
	}

	@Test
	public void hazelcastCacheWithConfig() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=hazelcast",
				"spring.cache.hazelcast.config=org/springframework/boot/autoconfigure/cache/hazelcast-specific.xml");
		HazelcastCacheManager cacheManager = validateCacheManager(HazelcastCacheManager.class);
		cacheManager.getCache("foobar");
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foobar"));
		assertThat(cacheManager.getCacheNames(), hasSize(1));
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
		HazelcastCacheManager cacheManager = validateCacheManager(HazelcastCacheManager.class);
		assertThat(new DirectFieldAccessor(cacheManager).getPropertyValue("hazelcastInstance"),
				is(this.context.getBean("customHazelcastInstance")));
	}

	@Test
	public void hazelcastAsJCacheWithCaches() {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
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
		assertThat(cacheManager.getCacheManager().getURI(),
				is(configResource.getURI()));
	}

	@Test
	public void infinispanCacheWithConfig() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=infinispan",
				"spring.cache.infinispan.config=infinispan.xml");
		SpringEmbeddedCacheManager cacheManager = validateCacheManager(SpringEmbeddedCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
	}

	@Test
	public void infinispanCacheWithCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=infinispan",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		SpringEmbeddedCacheManager cacheManager = validateCacheManager(SpringEmbeddedCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void infinispanCacheWithCachesAndCustomConfig() {
		load(InfinispanCustomConfiguration.class, "spring.cache.type=infinispan",
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		SpringEmbeddedCacheManager cacheManager = validateCacheManager(SpringEmbeddedCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));

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
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
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
		assertThat(cacheManager.getCacheManager().getURI(),
				is(configResource.getURI()));
	}

	@Test
	public void jCacheCacheWithCachesAndCustomizer() {
		String cachingProviderFqn = HazelcastCachingProvider.class.getName();
		load(JCacheWithCustomizerConfiguration.class, "spring.cache.type=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo", "spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "custom1")); // see
		// customizer
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void guavaCacheExplicitWithCaches() {
		load(DefaultCacheConfiguration.class, "spring.cache.type=guava",
				"spring.cache.cacheNames=foo");
		GuavaCacheManager cacheManager = validateCacheManager(GuavaCacheManager.class);
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		// See next tests: no spec given so stats should be disabled
		assertThat(((GuavaCache) foo).getNativeCache().stats().missCount(), equalTo(0L));
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
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
		Cache foo = cacheManager.getCache("foo");
		foo.get("1");
		assertThat(((GuavaCache) foo).getNativeCache().stats().missCount(), equalTo(1L));
	}

	private <T extends CacheManager> T validateCacheManager(Class<T> type) {
		CacheManager cacheManager = this.context.getBean(CacheManager.class);
		assertThat("Wrong cache manager type", cacheManager, is(instanceOf(type)));
		return type.cast(cacheManager);
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?> config,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(config);
		applicationContext.register(CacheAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
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
	@EnableCaching
	static class RedisCacheConfiguration {

		@Bean
		public RedisTemplate<?, ?> redisTemplate() {
			return mock(RedisTemplate.class);
		}

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
			when(cacheManager.getCacheNames()).thenReturn(Collections.<String>emptyList());
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
					config.setExpiryPolicyFactory(CreatedExpiryPolicy
							.factoryOf(Duration.TEN_MINUTES));
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
			net.sf.ehcache.CacheManager cacheManager = mock(net.sf.ehcache.CacheManager.class);
			when(cacheManager.getStatus()).thenReturn(Status.STATUS_ALIVE);
			when(cacheManager.getCacheNames()).thenReturn(new String[0]);
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
			when(builder.build()).thenReturn(new ConfigurationBuilder().build());
			return builder;
		}

	}

	@Configuration
	@Import({ GenericCacheConfiguration.class, RedisCacheConfiguration.class })
	static class CustomCacheManagerConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("custom1");
		}

	}

	@Configuration
	@Import({ GenericCacheConfiguration.class, RedisCacheConfiguration.class })
	static class CustomCacheManagerFromSupportConfiguration extends
			CachingConfigurerSupport {

		@Override
		@Bean
		// The @Bean annotation is important, see CachingConfigurerSupport Javadoc
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("custom1");
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
	@Import({ GenericCacheConfiguration.class, RedisCacheConfiguration.class })
	static class CustomCacheResolverConfiguration extends CachingConfigurerSupport {

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

}
