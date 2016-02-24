/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Invoke the available {@link CacheManagerCustomizer} instances in the context for a
 * given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 */
class CacheManagerCustomizerInvoker implements ApplicationContextAware {

	private ConfigurableApplicationContext applicationContext;

	/**
	 * Customize the specified {@link CacheManager}. Locates all
	 * {@link CacheManagerCustomizer} beans able to handle the specified instance and
	 * invoke {@link CacheManagerCustomizer#customize(CacheManager)} on them.
	 * @param cacheManager the cache manager to customize
	 */
	public void customize(CacheManager cacheManager) {
		List<CacheManagerCustomizer<CacheManager>> customizers = findCustomizers(
				cacheManager);
		AnnotationAwareOrderComparator.sort(customizers);
		for (CacheManagerCustomizer<CacheManager> customizer : customizers) {
			customizer.customize(cacheManager);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<CacheManagerCustomizer<CacheManager>> findCustomizers(
			CacheManager cacheManager) {
		if (this.applicationContext == null) {
			return Collections.emptyList();
		}
		Map<String, CacheManagerCustomizer> map = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(this.applicationContext.getBeanFactory(),
						CacheManagerCustomizer.class);
		List<CacheManagerCustomizer<CacheManager>> customizers = new ArrayList<CacheManagerCustomizer<CacheManager>>();
		for (CacheManagerCustomizer customizer : map.values()) {
			Class<?> target = GenericTypeResolver.resolveTypeArgument(
					customizer.getClass(), CacheManagerCustomizer.class);
			if (target == null || target.isAssignableFrom(cacheManager.getClass())) {
				customizers.add(customizer);
			}
		}
		return customizers;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			this.applicationContext = (ConfigurableApplicationContext) applicationContext;
		}
	}

}
