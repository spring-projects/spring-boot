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

package sample;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SampleRedisCacheApplication.class})
public class SampleRedisCacheApplicationTests {

	@Autowired
	private CacheManager cacheManager;

	@Test
	public void testCacheManagerInstance() {
		assertThat(this.cacheManager, instanceOf(RedisCacheManager.class));
	}

	@Test
	public void testCacheManager() {
		assertThat(this.cacheManager.getCacheNames(), contains("countries"));
	}

	@Configuration
	static class RedisCacheConfiguration {

		@Bean
		public RedisTemplate<?, ?> redisTemplate() {
			return mock(RedisTemplate.class);
		}

	}

}
