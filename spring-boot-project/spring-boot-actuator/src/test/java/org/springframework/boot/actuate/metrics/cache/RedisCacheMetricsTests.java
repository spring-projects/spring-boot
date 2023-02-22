/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.metrics.cache;

import java.util.UUID;
import java.util.function.BiConsumer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisCacheMetrics}.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisCacheMetricsTests {

	@Container
	static final RedisContainer redis = new RedisContainer();

	private static final Tags TAGS = Tags.of("app", "test").and("cache", "test");

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class, CacheAutoConfiguration.class))
		.withUserConfiguration(CachingConfiguration.class)
		.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
				"spring.data.redis.port=" + redis.getFirstMappedPort(), "spring.cache.type=redis",
				"spring.cache.redis.enable-statistics=true");

	@Test
	void cacheStatisticsAreExposed() {
		this.contextRunner.run(withCacheMetrics((cache, meterRegistry) -> {
			assertThat(meterRegistry.find("cache.size").tags(TAGS).functionCounter()).isNull();
			assertThat(meterRegistry.find("cache.gets").tags(TAGS.and("result", "hit")).functionCounter()).isNotNull();
			assertThat(meterRegistry.find("cache.gets").tags(TAGS.and("result", "miss")).functionCounter()).isNotNull();
			assertThat(meterRegistry.find("cache.gets").tags(TAGS.and("result", "pending")).functionCounter())
				.isNotNull();
			assertThat(meterRegistry.find("cache.evictions").tags(TAGS).functionCounter()).isNull();
			assertThat(meterRegistry.find("cache.puts").tags(TAGS).functionCounter()).isNotNull();
			assertThat(meterRegistry.find("cache.removals").tags(TAGS).functionCounter()).isNotNull();
			assertThat(meterRegistry.find("cache.lock.duration").tags(TAGS).timeGauge()).isNotNull();
		}));
	}

	@Test
	void cacheHitsAreExposed() {
		this.contextRunner.run(withCacheMetrics((cache, meterRegistry) -> {
			String key = UUID.randomUUID().toString();
			cache.put(key, "test");

			cache.get(key);
			cache.get(key);
			assertThat(meterRegistry.get("cache.gets").tags(TAGS.and("result", "hit")).functionCounter().count())
				.isEqualTo(2.0d);
		}));
	}

	@Test
	void cacheMissesAreExposed() {
		this.contextRunner.run(withCacheMetrics((cache, meterRegistry) -> {
			String key = UUID.randomUUID().toString();
			cache.get(key);
			cache.get(key);
			cache.get(key);
			assertThat(meterRegistry.get("cache.gets").tags(TAGS.and("result", "miss")).functionCounter().count())
				.isEqualTo(3.0d);
		}));
	}

	@Test
	void cacheMetricsMatchCacheStatistics() {
		this.contextRunner.run((context) -> {
			RedisCache cache = getTestCache(context);
			RedisCacheMetrics cacheMetrics = new RedisCacheMetrics(cache, TAGS);
			assertThat(cacheMetrics.hitCount()).isEqualTo(cache.getStatistics().getHits());
			assertThat(cacheMetrics.missCount()).isEqualTo(cache.getStatistics().getMisses());
			assertThat(cacheMetrics.putCount()).isEqualTo(cache.getStatistics().getPuts());
			assertThat(cacheMetrics.size()).isNull();
			assertThat(cacheMetrics.evictionCount()).isNull();
		});
	}

	private ContextConsumer<AssertableApplicationContext> withCacheMetrics(
			BiConsumer<RedisCache, MeterRegistry> stats) {
		return (context) -> {
			RedisCache cache = getTestCache(context);
			SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
			new RedisCacheMetrics(cache, Tags.of("app", "test")).bindTo(meterRegistry);
			stats.accept(cache, meterRegistry);
		};
	}

	private RedisCache getTestCache(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(RedisCacheManager.class);
		RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);
		RedisCache cache = (RedisCache) cacheManager.getCache("test");
		assertThat(cache).isNotNull();
		return cache;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	static class CachingConfiguration {

	}

}
