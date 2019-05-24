/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CachesEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Stephane Nicoll
 */
class CachesEndpointWebIntegrationTests {

	@WebEndpointTest
	void allCaches(WebTestClient client) {
		client.get().uri("/actuator/caches").exchange().expectStatus().isOk().expectBody()
				.jsonPath("cacheManagers.one.caches.a.target").isEqualTo(ConcurrentHashMap.class.getName())
				.jsonPath("cacheManagers.one.caches.b.target").isEqualTo(ConcurrentHashMap.class.getName())
				.jsonPath("cacheManagers.two.caches.a.target").isEqualTo(ConcurrentHashMap.class.getName())
				.jsonPath("cacheManagers.two.caches.c.target").isEqualTo(ConcurrentHashMap.class.getName());
	}

	@WebEndpointTest
	void namedCache(WebTestClient client) {
		client.get().uri("/actuator/caches/b").exchange().expectStatus().isOk().expectBody().jsonPath("name")
				.isEqualTo("b").jsonPath("cacheManager").isEqualTo("one").jsonPath("target")
				.isEqualTo(ConcurrentHashMap.class.getName());
	}

	@WebEndpointTest
	void namedCacheWithUnknownName(WebTestClient client) {
		client.get().uri("/actuator/caches/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void namedCacheWithNonUniqueName(WebTestClient client) {
		client.get().uri("/actuator/caches/a").exchange().expectStatus().isBadRequest();
	}

	@WebEndpointTest
	void clearNamedCache(WebTestClient client, ApplicationContext context) {
		Cache b = context.getBean("one", CacheManager.class).getCache("b");
		b.put("test", "value");
		client.delete().uri("/actuator/caches/b").exchange().expectStatus().isNoContent();
		assertThat(b.get("test")).isNull();
	}

	@WebEndpointTest
	void cleanNamedCacheWithUnknownName(WebTestClient client) {
		client.delete().uri("/actuator/caches/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void clearNamedCacheWithNonUniqueName(WebTestClient client) {
		client.get().uri("/actuator/caches/a").exchange().expectStatus().isBadRequest();
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		public CacheManager one() {
			return new ConcurrentMapCacheManager("a", "b");
		}

		@Bean
		public CacheManager two() {
			return new ConcurrentMapCacheManager("a", "c");
		}

		@Bean
		public CachesEndpoint endpoint(Map<String, CacheManager> cacheManagers) {
			return new CachesEndpoint(cacheManagers);
		}

		@Bean
		public CachesEndpointWebExtension cachesEndpointWebExtension(CachesEndpoint endpoint) {
			return new CachesEndpointWebExtension(endpoint);
		}

	}

}
