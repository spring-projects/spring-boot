/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.cache;

import java.util.Collections;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.junit.Test;

import org.springframework.cache.caffeine.CaffeineCache;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CaffeineCacheMeterBinderProvider}.
 *
 * @author Stephane Nicoll
 */
public class CaffeineCacheMeterBinderProviderTests {

	@Test
	public void caffeineCacheProvider() {
		CaffeineCache cache = new CaffeineCache("test", Caffeine.newBuilder().build());
		MeterBinder meterBinder = new CaffeineCacheMeterBinderProvider()
				.getMeterBinder(cache, Collections.emptyList());
		assertThat(meterBinder).isInstanceOf(CaffeineCacheMetrics.class);
	}

}
