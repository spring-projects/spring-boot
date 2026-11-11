/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.cache;

import java.time.Duration;

import com.redis.testcontainers.RedisContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SampleCacheApplicationRedisTests {

	@Container
	@ServiceConnection
	private static final RedisContainer redis = TestImage.forContainer(RedisContainer.class);

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
		Awaitility.waitAtMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			ValueWrapper belgium = countries.get("BE");
			assertThat(belgium).isNotNull();
			assertThat((Country) belgium.get()).isEqualTo(be);
		});
	}

}
