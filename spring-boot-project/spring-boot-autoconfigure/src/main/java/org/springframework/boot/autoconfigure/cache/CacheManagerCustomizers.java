/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.cache.CacheManager;

/**
 * Invokes the available {@link CacheManagerCustomizer} instances in the context for a
 * given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
public class CacheManagerCustomizers {

	private final List<CacheManagerCustomizer<?>> customizers;

	public CacheManagerCustomizers(
			List<? extends CacheManagerCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers)
				: Collections.emptyList();
	}

	/**
	 * Customize the specified {@link CacheManager}. Locates all
	 * {@link CacheManagerCustomizer} beans able to handle the specified instance and
	 * invoke {@link CacheManagerCustomizer#customize(CacheManager)} on them.
	 * @param <T> the type of cache manager
	 * @param cacheManager the cache manager to customize
	 * @return the cache manager
	 */
	@SuppressWarnings("unchecked")
	public <T extends CacheManager> T customize(T cacheManager) {
		LambdaSafe.callbacks(CacheManagerCustomizer.class, this.customizers, cacheManager)
				.withLogger(CacheManagerCustomizers.class)
				.invoke((customizer) -> customizer.customize(cacheManager));
		return cacheManager;
	}

}
