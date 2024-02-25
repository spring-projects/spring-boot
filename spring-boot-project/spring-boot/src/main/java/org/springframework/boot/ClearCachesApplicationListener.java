/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ApplicationListener} to cleanup caches once the context is loaded.
 *
 * @author Phillip Webb
 */
class ClearCachesApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

	/**
	 * This method is called when the application context is refreshed. It clears the
	 * reflection cache and class loader caches.
	 * @param event The context refreshed event
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ReflectionUtils.clearCache();
		clearClassLoaderCaches(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Clears the caches of the given class loader and its parent class loaders
	 * recursively.
	 * @param classLoader the class loader whose caches need to be cleared
	 */
	private void clearClassLoaderCaches(ClassLoader classLoader) {
		if (classLoader == null) {
			return;
		}
		try {
			Method clearCacheMethod = classLoader.getClass().getDeclaredMethod("clearCache");
			clearCacheMethod.invoke(classLoader);
		}
		catch (Exception ex) {
			// Ignore
		}
		clearClassLoaderCaches(classLoader.getParent());
	}

}
