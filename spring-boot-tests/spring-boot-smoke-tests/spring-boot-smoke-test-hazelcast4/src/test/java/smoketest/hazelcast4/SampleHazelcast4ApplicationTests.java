/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.hazelcast4;

import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SampleHazelcast4ApplicationTests {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CountryRepository countryRepository;

	@Test
	void cacheManagerIsUsingHazelcast() {
		assertThat(this.cacheManager).isInstanceOf(HazelcastCacheManager.class);
	}

	@Test
	void healthEndpointHasHazelcastContributor() {
		this.webClient.get().uri("/actuator/health/hazelcast").exchange().expectStatus().isOk().expectBody()
				.jsonPath("status").isEqualTo("UP").jsonPath("details.name").isNotEmpty().jsonPath("details.uuid")
				.isNotEmpty();
	}

	@Test
	void metricsEndpointHasCacheMetrics() {
		this.webClient.get().uri("/actuator/metrics/cache.entries").exchange().expectStatus().isOk();
	}

}
