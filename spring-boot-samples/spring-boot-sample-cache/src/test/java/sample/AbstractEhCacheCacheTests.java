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

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SampleEhCacheCacheApplication.class})
public abstract class AbstractEhCacheCacheTests {

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private EhCacheCacheManager ehCacheManager;

	@After
	public void tearDown() {
		if (this.ehCacheManager != null) {
			this.ehCacheManager.getCacheManager().shutdown();
		}
	}

	@Test
	public void testCacheManagerInstance() {
		assertThat(this.cacheManager, instanceOf(EhCacheCacheManager.class));
	}

	@Test
	public void testCacheManager() {
		assertThat(this.cacheManager.getCacheNames(), contains("countries"));
	}

}
