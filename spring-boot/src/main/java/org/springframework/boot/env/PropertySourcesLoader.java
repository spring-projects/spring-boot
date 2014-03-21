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
import org.springframework.util.StringUtils;

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
	 * @return the loaded property source or {@code null}
	 * @throws IOException
	 */
	public PropertySource<?> load(Resource resource) throws IOException {
		return load(resource, null);
	}

	/**
	 * Load the profile-specific properties from the specified resource (if any) and add
	 * it as the first source.
	 * @param resource the source resource (may be {@code null}).
	 * @param profile a specific profile to load or {@code null} to load the default.
	 * @return the loaded property source or {@code null}
	 * @throws IOException
	 */
	public PropertySource<?> load(Resource resource, String profile) throws IOException {
		return load(resource, resource.getDescription(), profile);
	}

	/**
	 * Load the profile-specific properties from the specified resource (if any), give the
	 * name provided and add it as the first source.
	 * @param resource the source resource (may be {@code null}).
	 * @param name the root property name (may be {@code null}).
	 * @param profile a specific profile to load or {@code null} to load the default.
	 * @return the loaded property source or {@code null}
	 * @throws IOException
	 */
	public PropertySource<?> load(Resource resource, String name, String profile)
			throws IOException {
		return load(resource, null, name, profile);
	}

	/**
	 * Load the profile-specific properties from the specified resource (if any), give the
	 * name provided and add it to a group of property sources identified by the group
	 * name. Property sources are added to the end of a group, but new groups are added as
	 * the first in the chain being assembled. This means the normal sequence of calls is
	 * to first create the group for the default (null) profile, and then add specific
	 * groups afterwards (with the highest priority last). Property resolution from the
	 * resulting sources will consider all keys for a given group first and then move to
	 * the next group.
	 * @param resource the source resource (may be {@code null}).
	 * @param group an identifier for the group that this source belongs to
	 * @param name the root property name (may be {@code null}).
	 * @param profile a specific profile to load or {@code null} to load the default.
	 * @return the loaded property source or {@code null}
	 * @throws IOException
	 */
	public PropertySource<?> load(Resource resource, String group, String name,
			String profile) throws IOException {
		if (isFile(resource)) {
			String sourceName = generatePropertySourceName(name, profile);
			for (PropertySourceLoader loader : this.loaders) {
				if (canLoadFileExtension(loader, resource)) {
					PropertySource<?> specific = loader.load(sourceName, resource,
							profile);
					addPropertySource(group, specific, profile);
					return specific;
				}
			}
		}
		return null;
	}

	private boolean isFile(Resource resource) {
		return resource != null
				&& resource.exists()
				&& StringUtils.hasText(StringUtils.getFilenameExtension(resource
						.getFilename()));
	}

	private String generatePropertySourceName(String name, String profile) {
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

	private void addPropertySource(String basename, PropertySource<?> source,
			String profile) {

		if (source == null) {
			return;
		}

		if (basename == null) {
			this.propertySources.addLast(source);
			return;
		}

		EnumerableCompositePropertySource group = getGeneric(basename);
		group.add(source);
		if (this.propertySources.contains(group.getName())) {
			this.propertySources.replace(group.getName(), group);
		}
		else {
			this.propertySources.addFirst(group);
		}

	}

	private EnumerableCompositePropertySource getGeneric(String name) {
		PropertySource<?> source = this.propertySources.get(name);
		if (source instanceof EnumerableCompositePropertySource) {
			return (EnumerableCompositePropertySource) source;
		}
		EnumerableCompositePropertySource composite = new EnumerableCompositePropertySource(
				name);
		return composite;
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
