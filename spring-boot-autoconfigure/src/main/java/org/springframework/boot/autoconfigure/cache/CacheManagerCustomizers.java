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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Invokes the available {@link CacheManagerCustomizer} instances in the context for a
 * given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 */
class CacheManagerCustomizers implements ApplicationContextAware {

	private ConfigurableApplicationContext applicationContext;

	/**
	 * Customize the specified {@link CacheManager}. Locates all
	 * {@link CacheManagerCustomizer} beans able to handle the specified instance and
	 * invoke {@link CacheManagerCustomizer#customize(CacheManager)} on them.
	 * @param <T> the type of cache manager
	 * @param cacheManager the cache manager to customize
	 * @return the cache manager
	 */
	public <T extends CacheManager> T customize(T cacheManager) {
		List<CacheManagerCustomizer<CacheManager>> customizers = findCustomizers(
				cacheManager);
		AnnotationAwareOrderComparator.sort(customizers);
		for (CacheManagerCustomizer<CacheManager> customizer : customizers) {
			customizer.customize(cacheManager);
		}
		return cacheManager;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<CacheManagerCustomizer<CacheManager>> findCustomizers(
			CacheManager cacheManager) {
		if (this.applicationContext == null) {
			return Collections.emptyList();
		}
		Class<?> cacheManagerClass = cacheManager.getClass();
		List<CacheManagerCustomizer<CacheManager>> customizers = new ArrayList<CacheManagerCustomizer<CacheManager>>();
		for (CacheManagerCustomizer customizer : getBeans(CacheManagerCustomizer.class)) {
			if (canCustomize(customizer, cacheManagerClass)) {
				customizers.add(customizer);
			}
		}
		return customizers;
	}

	private <T> Collection<T> getBeans(Class<T> type) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(
				this.applicationContext.getBeanFactory(), type).values();
	}

	private boolean canCustomize(CacheManagerCustomizer<?> customizer,
			Class<?> cacheManagerClass) {
		Class<?> target = GenericTypeResolver.resolveTypeArgument(customizer.getClass(),
				CacheManagerCustomizer.class);
		return (target == null || target.isAssignableFrom(cacheManagerClass));
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			this.applicationContext = (ConfigurableApplicationContext) applicationContext;
		}
	}

}
