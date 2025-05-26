/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.cache;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SampleCacheApplicationRedisTests {

	@Container
	@ServiceConnection
	private static final RedisContainer redis = TestImage.container(RedisContainer.class);

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CountryRepository countryRepository;

	@Test
	void validateCache() {
		Cache countries = this.cacheManager.getCache("countries");
		assertThat(countries).isNotNull();
		countries.clear(); // Simple test assuming the cache is empty
		assertThat(countries.get("BE")).isNull();
		Country be = this.countryRepository.findByCode("BE");
		assertThat((Country) countries.get("BE").get()).isEqualTo(be);
	}

}
