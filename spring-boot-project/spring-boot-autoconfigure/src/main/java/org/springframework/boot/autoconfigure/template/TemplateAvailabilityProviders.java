/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * Collection of {@link TemplateAvailabilityProvider} beans that can be used to check
 * which (if any) templating engine supports a given view. Caches responses unless the
 * {@code spring.template.provider.cache} property is set to {@code false}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 1.4.0
 */
public class TemplateAvailabilityProviders {

	private final List<TemplateAvailabilityProvider> providers;

	private static final int CACHE_LIMIT = 1024;

	private static final TemplateAvailabilityProvider NONE = new NoTemplateAvailabilityProvider();

	/**
	 * Resolved template views, returning already cached instances without a global lock.
	 */
	private final Map<String, TemplateAvailabilityProvider> resolved = new ConcurrentHashMap<>(CACHE_LIMIT);

	/**
	 * Map from view name resolve template view, synchronized when accessed.
	 */
	private final Map<String, TemplateAvailabilityProvider> cache = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, TemplateAvailabilityProvider> eldest) {
			if (size() > CACHE_LIMIT) {
				TemplateAvailabilityProviders.this.resolved.remove(eldest.getKey());
				return true;
			}
			return false;
		}

	};

	/**
	 * Create a new {@link TemplateAvailabilityProviders} instance.
	 * @param applicationContext the source application context
	 */
	public TemplateAvailabilityProviders(ApplicationContext applicationContext) {
		this((applicationContext != null) ? applicationContext.getClassLoader() : null);
	}

	/**
	 * Create a new {@link TemplateAvailabilityProviders} instance.
	 * @param classLoader the source class loader
	 */
	public TemplateAvailabilityProviders(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.providers = SpringFactoriesLoader.loadFactories(TemplateAvailabilityProvider.class, classLoader);
	}

	/**
	 * Create a new {@link TemplateAvailabilityProviders} instance.
	 * @param providers the underlying providers
	 */
	protected TemplateAvailabilityProviders(Collection<? extends TemplateAvailabilityProvider> providers) {
		Assert.notNull(providers, "Providers must not be null");
		this.providers = new ArrayList<>(providers);
	}

	/**
	 * Return the underlying providers being used.
	 * @return the providers being used
	 */
	public List<TemplateAvailabilityProvider> getProviders() {
		return this.providers;
	}

	/**
	 * Get the provider that can be used to render the given view.
	 * @param view the view to render
	 * @param applicationContext the application context
	 * @return a {@link TemplateAvailabilityProvider} or null
	 */
	public TemplateAvailabilityProvider getProvider(String view, ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		return getProvider(view, applicationContext.getEnvironment(), applicationContext.getClassLoader(),
				applicationContext);
	}

	/**
	 * Get the provider that can be used to render the given view.
	 * @param view the view to render
	 * @param environment the environment
	 * @param classLoader the class loader
	 * @param resourceLoader the resource loader
	 * @return a {@link TemplateAvailabilityProvider} or null
	 */
	public TemplateAvailabilityProvider getProvider(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader) {
		Assert.notNull(view, "View must not be null");
		Assert.notNull(environment, "Environment must not be null");
		Assert.notNull(classLoader, "ClassLoader must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Boolean useCache = environment.getProperty("spring.template.provider.cache", Boolean.class, true);
		if (!useCache) {
			return findProvider(view, environment, classLoader, resourceLoader);
		}
		TemplateAvailabilityProvider provider = this.resolved.get(view);
		if (provider == null) {
			synchronized (this.cache) {
				provider = findProvider(view, environment, classLoader, resourceLoader);
				provider = (provider != null) ? provider : NONE;
				this.resolved.put(view, provider);
				this.cache.put(view, provider);
			}
		}
		return (provider != NONE) ? provider : null;
	}

	private TemplateAvailabilityProvider findProvider(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader) {
		for (TemplateAvailabilityProvider candidate : this.providers) {
			if (candidate.isTemplateAvailable(view, environment, classLoader, resourceLoader)) {
				return candidate;
			}
		}
		return null;
	}

	private static class NoTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

		@Override
		public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
				ResourceLoader resourceLoader) {
			return false;
		}

	}

}
