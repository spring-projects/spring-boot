/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.env;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * Utiltiy that can be used to {@link MutablePropertySources} using
 * {@link PropertySourceLoader}s.
 * 
 * @author Phillip Webb
 */
public class PropertySourcesLoader {

	private final MutablePropertySources propertySources;

	private final List<PropertySourceLoader> loaders;

	/**
	 * Create a new {@link PropertySourceLoader} instance backed by a new
	 * {@link MutablePropertySources}.
	 */
	public PropertySourcesLoader() {
		this(new MutablePropertySources());
	}

	/**
	 * Create a new {@link PropertySourceLoader} instance backed by the specified
	 * {@link MutablePropertySources}.
	 * @param propertySources the destination property sources
	 */
	public PropertySourcesLoader(MutablePropertySources propertySources) {
		Assert.notNull(propertySources, "PropertySources must not be null");
		this.propertySources = propertySources;
		this.loaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				null);
	}

	/**
	 * Load the specified resource (if possible) and add it as the first source.
	 * @param resource the source resource (may be {@code null}).
	 * @param name the root property name (may be {@code null}).
	 * @param profile a specific profile to load or {@code null} to load the default.
	 * @return the loaded property source or {@code null}
	 * @throws IOException
	 */
	public PropertySource<?> load(Resource resource, String name, String profile)
			throws IOException {
		if (resource != null && resource.exists()) {
			name = generatePropertySourceName(resource, name, profile);
			for (PropertySourceLoader loader : this.loaders) {
				if (canLoadFileExtension(loader, resource)) {
					PropertySource<?> source = loader.load(name, resource, profile);
					addPropertySource(source);
					return source;
				}
			}
		}
		return null;
	}

	private String generatePropertySourceName(Resource resource, String name,
			String profile) {
		if (name == null) {
			name = resource.getDescription();
		}
		return (profile == null ? name : name + "#" + profile);
	}

	private boolean canLoadFileExtension(PropertySourceLoader loader, Resource resource) {
		String filename = resource.getFilename().toLowerCase();
		for (String extension : loader.getFileExtensions()) {
			if (filename.endsWith("." + extension.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private void addPropertySource(PropertySource<?> propertySource) {
		if (propertySource != null) {
			this.propertySources.addLast(propertySource);
		}
	}

	/**
	 * Return the {@link MutablePropertySources} being loaded.
	 */
	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	/**
	 * Returns all file extensions that could be loaded.
	 */
	public Set<String> getAllFileExtensions() {
		Set<String> fileExtensions = new HashSet<String>();
		for (PropertySourceLoader loader : this.loaders) {
			fileExtensions.addAll(Arrays.asList(loader.getFileExtensions()));
		}
		return fileExtensions;
	}

}
