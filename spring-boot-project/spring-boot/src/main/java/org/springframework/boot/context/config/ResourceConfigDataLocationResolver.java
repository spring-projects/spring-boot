/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigDataLocationResolver} for standard locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ResourceConfigDataLocationResolver implements ConfigDataLocationResolver<ResourceConfigDataLocation>, Ordered {

	private static final String PREFIX = "resource:";

	static final String CONFIG_NAME_PROPERTY = "spring.config.name";

	private static final String[] DEFAULT_CONFIG_NAMES = { "application" };

	private static final Resource[] EMPTY_RESOURCES = {};

	private static final Comparator<File> FILE_COMPARATOR = Comparator.comparing(File::getAbsolutePath);

	private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9*]*?:)(.*$)");

	private static final String NO_PROFILE = null;

	private final Log logger;

	private final List<PropertySourceLoader> propertySourceLoaders;

	private final String[] configNames;

	private final ResourceLoader resourceLoader;

	/**
	 * Create a new {@link ResourceConfigDataLocationResolver} instance.
	 * @param logger the logger to use
	 * @param binder a binder backed by the initial {@link Environment}
	 * @param resourceLoader a {@link ResourceLoader} used to load resources
	 */
	ResourceConfigDataLocationResolver(Log logger, Binder binder, ResourceLoader resourceLoader) {
		this.logger = logger;
		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				getClass().getClassLoader());
		this.configNames = getConfigNames(binder);
		this.resourceLoader = resourceLoader;
	}

	private String[] getConfigNames(Binder binder) {
		String[] configNames = binder.bind(CONFIG_NAME_PROPERTY, String[].class).orElse(DEFAULT_CONFIG_NAMES);
		for (String configName : configNames) {
			validateConfigName(configName);
		}
		return configNames;
	}

	private void validateConfigName(String name) {
		Assert.state(!name.contains("*"), () -> "Config name '" + name + "' cannot contain '*'");
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, String location) {
		return true;
	}

	@Override
	public List<ResourceConfigDataLocation> resolve(ConfigDataLocationResolverContext context, String location) {
		return resolve(location, getResolvables(context, location));
	}

	@Override
	public List<ResourceConfigDataLocation> resolveProfileSpecific(ConfigDataLocationResolverContext context,
			String location, Profiles profiles) {
		return resolve(location, getProfileSpecificResolvables(context, location, profiles));
	}

	private Set<Resolvable> getResolvables(ConfigDataLocationResolverContext context, String location) {
		String resourceLocation = getResourceLocation(context, location);
		try {
			if (isDirectoryLocation(resourceLocation)) {
				return getResolvablesForDirectory(resourceLocation, NO_PROFILE);
			}
			return getResolvablesForFile(resourceLocation, NO_PROFILE);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to load config data from '" + location + "'", ex);
		}
	}

	private Set<Resolvable> getProfileSpecificResolvables(ConfigDataLocationResolverContext context, String location,
			Profiles profiles) {
		Set<Resolvable> resolvables = new LinkedHashSet<>();
		String resourceLocation = getResourceLocation(context, location);
		for (String profile : profiles) {
			resolvables.addAll(getResolvables(resourceLocation, profile));
		}
		return resolvables;
	}

	private String getResourceLocation(ConfigDataLocationResolverContext context, String location) {
		String resourceLocation = (location.startsWith(PREFIX)) ? location.substring(PREFIX.length()) : location;
		boolean isAbsolute = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
		if (isAbsolute) {
			return resourceLocation;
		}
		ConfigDataLocation parent = context.getParent();
		if (parent instanceof ResourceConfigDataLocation) {
			String parentLocation = ((ResourceConfigDataLocation) parent).getLocation();
			String parentDirectory = parentLocation.substring(0, parentLocation.lastIndexOf("/") + 1);
			return parentDirectory + resourceLocation;
		}
		return resourceLocation;
	}

	private Set<Resolvable> getResolvables(String resourceLocation, String profile) {
		if (isDirectoryLocation(resourceLocation)) {
			return getResolvablesForDirectory(resourceLocation, profile);
		}
		return getResolvablesForFile(resourceLocation, profile);
	}

	private Set<Resolvable> getResolvablesForDirectory(String resourceLocation, String profile) {
		Set<Resolvable> resolvables = new LinkedHashSet<>();
		for (String name : this.configNames) {
			for (PropertySourceLoader loader : this.propertySourceLoaders) {
				for (String extension : loader.getFileExtensions()) {
					resolvables.add(new Resolvable(resourceLocation + name, profile, extension, loader));
				}
			}
		}
		return resolvables;
	}

	private Set<Resolvable> getResolvablesForFile(String resourceLocation, String profile) {
		for (PropertySourceLoader loader : this.propertySourceLoaders) {
			String extension = getLoadableFileExtension(loader, resourceLocation);
			if (extension != null) {
				String root = resourceLocation.substring(0, resourceLocation.length() - extension.length() - 1);
				return Collections.singleton(new Resolvable(root, profile, extension, loader));
			}
		}
		throw new IllegalStateException("File extension is not known to any PropertySourceLoader. "
				+ "If the location is meant to reference a directory, it must end in '/'");
	}

	private String getLoadableFileExtension(PropertySourceLoader loader, String resourceLocation) {
		for (String fileExtension : loader.getFileExtensions()) {
			if (StringUtils.endsWithIgnoreCase(resourceLocation, fileExtension)) {
				return fileExtension;
			}
		}
		return null;
	}

	private boolean isDirectoryLocation(String resourceLocation) {
		return resourceLocation.endsWith("/");
	}

	private List<ResourceConfigDataLocation> resolve(String location, Set<Resolvable> resolvables) {
		List<ResourceConfigDataLocation> resolved = new ArrayList<>();
		for (Resolvable resolvable : resolvables) {
			resolved.addAll(resolve(location, resolvable));
		}
		return resolved;
	}

	private List<ResourceConfigDataLocation> resolve(String location, Resolvable resolvable) {
		if (!resolvable.isPatternLocation()) {
			return resolveNonPattern(location, resolvable);
		}
		return resolvePattern(location, resolvable);
	}

	private List<ResourceConfigDataLocation> resolveNonPattern(String location, Resolvable resolvable) {
		Resource resource = loadResource(resolvable.getResourceLocation());
		if (resource.exists()) {
			ResourceConfigDataLocation resolved = createConfigResourceLocation(location, resolvable, resource);
			return Collections.singletonList(resolved);
		}
		logSkippingResource(resolvable);
		return Collections.emptyList();
	}

	private List<ResourceConfigDataLocation> resolvePattern(String location, Resolvable resolvable) {
		validatePatternLocation(resolvable.getResourceLocation());
		List<ResourceConfigDataLocation> resolved = new ArrayList<>();
		for (Resource resource : getResourcesFromResourceLocationPattern(resolvable.getResourceLocation())) {
			if (resource.exists()) {
				resolved.add(createConfigResourceLocation(location, resolvable, resource));
			}
			else {
				logSkippingResource(resolvable);
			}
		}
		return resolved;
	}

	private void logSkippingResource(Resolvable resolvable) {
		this.logger.trace(LogMessage.format("Skipping missing resource location %s", resolvable.getResourceLocation()));
	}

	private ResourceConfigDataLocation createConfigResourceLocation(String location, Resolvable resolvable,
			Resource resource) {
		String name = String.format("Resource config '%s' imported via location \"%s\"",
				resolvable.getResourceLocation(), location);
		return new ResourceConfigDataLocation(name, resource, resolvable.getLoader());
	}

	private void validatePatternLocation(String resourceLocation) {
		Assert.state(!resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX),
				"Classpath wildcard patterns cannot be used as a search location");
		Assert.state(StringUtils.countOccurrencesOf(resourceLocation, "*") == 1,
				() -> "Search location '" + resourceLocation + "' cannot contain multiple wildcards");
		String directoryPath = resourceLocation.substring(0, resourceLocation.lastIndexOf("/") + 1);
		Assert.state(directoryPath.endsWith("*/"),
				() -> "Search location '" + resourceLocation + "' must end with '*/'");
	}

	private Resource[] getResourcesFromResourceLocationPattern(String resourceLocationPattern) {
		String directoryPath = resourceLocationPattern.substring(0, resourceLocationPattern.indexOf("*/"));
		String fileName = resourceLocationPattern.substring(resourceLocationPattern.lastIndexOf("/") + 1);
		Resource directoryResource = loadResource(directoryPath);
		if (!directoryResource.exists()) {
			return EMPTY_RESOURCES;
		}
		File directory = getDirectory(resourceLocationPattern, directoryResource);
		File[] subDirectories = directory.listFiles(File::isDirectory);
		if (subDirectories == null) {
			return EMPTY_RESOURCES;
		}
		Arrays.sort(subDirectories, FILE_COMPARATOR);
		List<Resource> resources = new ArrayList<>();
		FilenameFilter filter = (dir, name) -> name.equals(fileName);
		for (File subDirectory : subDirectories) {
			File[] files = subDirectory.listFiles(filter);
			if (files != null) {
				Arrays.stream(files).map(FileSystemResource::new).forEach(resources::add);
			}
		}
		return resources.toArray(EMPTY_RESOURCES);
	}

	private Resource loadResource(String location) {
		location = StringUtils.cleanPath(location);
		if (!ResourceUtils.isUrl(location)) {
			location = ResourceUtils.FILE_URL_PREFIX + location;
		}
		return this.resourceLoader.getResource(location);
	}

	private File getDirectory(String patternLocation, Resource resource) {
		try {
			File directory = resource.getFile();
			Assert.state(directory.isDirectory(), () -> "'" + directory + "' is not a directory");
			return directory;
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Unable to load config data resource from pattern '" + patternLocation + "'", ex);
		}
	}

	/**
	 * A resource location that could be resolved by this resolver.
	 */
	private static class Resolvable {

		private final String resourceLocation;

		private final PropertySourceLoader loader;

		Resolvable(String rootLocation, String profile, String extension, PropertySourceLoader loader) {
			String profileSuffix = (StringUtils.hasText(profile)) ? "-" + profile : "";
			this.resourceLocation = rootLocation + profileSuffix + "." + extension;
			this.loader = loader;
		}

		boolean isPatternLocation() {
			return this.resourceLocation.contains("*");
		}

		String getResourceLocation() {
			return this.resourceLocation;
		}

		PropertySourceLoader getLoader() {
			return this.loader;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if ((obj == null) || (getClass() != obj.getClass())) {
				return false;
			}
			Resolvable other = (Resolvable) obj;
			return this.resourceLocation.equals(other.resourceLocation);
		}

		@Override
		public int hashCode() {
			return this.resourceLocation.hashCode();
		}

		@Override
		public String toString() {
			return this.resourceLocation;
		}

	}

}
