/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.CacheManager;
import org.springframework.core.ResolvableType;

/**
 * Invokes the available {@link CacheManagerCustomizer} instances in the context for a
 * given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
public class CacheManagerCustomizers {

	private static final Log logger = LogFactory.getLog(CacheManagerCustomizers.class);

	private final List<CacheManagerCustomizer<?>> customizers;

	public CacheManagerCustomizers(
			List<? extends CacheManagerCustomizer<?>> customizers) {
		this.customizers = (customizers != null ? new ArrayList<>(customizers)
				: Collections.emptyList());
	}

	/**
	 * Customize the specified {@link CacheManager}. Locates all
	 * {@link CacheManagerCustomizer} beans able to handle the specified instance and
	 * invoke {@link CacheManagerCustomizer#customize(CacheManager)} on them.
	 * @param <T> the type of cache manager
	 * @param cacheManager the cache manager to customize
	 * @return the cache manager
	 */
	public <T extends CacheManager> T customize(T cacheManager) {
		for (CacheManagerCustomizer<?> customizer : this.customizers) {
			Class<?> generic = ResolvableType
					.forClass(CacheManagerCustomizer.class, customizer.getClass())
					.resolveGeneric();
			if (generic.isInstance(cacheManager)) {
				customize(cacheManager, customizer);
			}
		}
		return cacheManager;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void customize(CacheManager cacheManager, CacheManagerCustomizer customizer) {
		try {
			customizer.customize(cacheManager);
		}
		catch (ClassCastException ex) {
			// Possibly a lambda-defined customizer which we could not resolve the generic
			// cache manager type for
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Non-matching cache manager type for customizer: " + customizer,
						ex);
			}
		}
	}

}
