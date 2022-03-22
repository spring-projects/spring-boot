/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Collections;

import io.micrometer.core.instrument.binder.MeterBinder;
import org.cache2k.extra.micrometer.Cache2kCacheMetrics;
import org.cache2k.extra.spring.SpringCache2kCacheManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Cache2kCacheMeterBinderProvider}.
 *
 * @author Stephane Nicoll
 */
class Cache2kCacheMeterBinderProviderTests {

	@Test
	void cache2kCacheProvider() {
		SpringCache2kCacheManager cacheManager = new SpringCache2kCacheManager()
				.addCaches((builder) -> builder.name("test"));
		MeterBinder meterBinder = new Cache2kCacheMeterBinderProvider().getMeterBinder(cacheManager.getCache("test"),
				Collections.emptyList());
		assertThat(meterBinder).isInstanceOf(Cache2kCacheMetrics.class);
	}

}
