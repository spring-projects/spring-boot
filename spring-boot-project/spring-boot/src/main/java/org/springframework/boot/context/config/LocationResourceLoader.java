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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Strategy interface for loading resources from a location. Supports single resource and
 * simple wildcard directory patterns.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Zhengsheng Xia
 */
class LocationResourceLoader {

	private static final Resource[] EMPTY_RESOURCES = {};

	private static final Comparator<File> FILE_PATH_COMPARATOR = Comparator.comparing(File::getAbsolutePath);

	private static final Comparator<File> FILE_NAME_COMPARATOR = Comparator.comparing(File::getName);

	private final ResourceLoader resourceLoader;

	/**
	 * Create a new {@link LocationResourceLoader} instance.
	 * @param resourceLoader the underlying resource loader
	 */
	LocationResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Returns if the location contains a pattern.
	 * @param location the location to check
	 * @return if the location is a pattern
	 */
	boolean isPattern(String location) {
		return StringUtils.hasLength(location) && location.contains("*");
	}

	/**
	 * Get a single resource from a non-pattern location.
	 * @param location the location
	 * @return the resource
	 * @see #isPattern(String)
	 */
	Resource getResource(String location) {
		validateNonPattern(location);
		location = StringUtils.cleanPath(location);
		if (!ResourceUtils.isUrl(location)) {
			location = ResourceUtils.FILE_URL_PREFIX + location;
		}
		return this.resourceLoader.getResource(location);
	}

	private void validateNonPattern(String location) {
		Assert.state(!isPattern(location), () -> String.format("Location '%s' must not be a pattern", location));
	}

	/**
	 * Get a multiple resources from a location pattern.
	 * @param location the location pattern
	 * @param type the type of resource to return
	 * @param allowClasspathAll the boolean value of allow param location contains
	 * 'classpath*:'
	 * @return the resources
	 * @see #isPattern(String)
	 */
	Resource[] getResources(String location, ResourceType type, boolean allowClasspathAll) {
		boolean startsWithClasspathAll = validatePatternAndCheckClasspathAll(location, type, allowClasspathAll);
		int wildcardlastIdx = location.indexOf("*/");
		String directoryPath = location.substring(0, wildcardlastIdx);
		if (startsWithClasspathAll) {
			return getResourcesFromClasspathAllPatternLocation(location, location.length() - wildcardlastIdx);
		}

		String fileName = location.substring(location.lastIndexOf("/") + 1);
		Resource directoryResource = getResource(directoryPath);
		if (!directoryResource.exists()) {
			return new Resource[] { directoryResource };
		}
		File directory = getDirectory(location, directoryResource);
		File[] subDirectories = directory.listFiles(this::isVisibleDirectory);
		if (subDirectories == null) {
			return EMPTY_RESOURCES;
		}
		Arrays.sort(subDirectories, FILE_PATH_COMPARATOR);
		if (type == ResourceType.DIRECTORY) {
			return Arrays.stream(subDirectories).map(FileSystemResource::new).toArray(Resource[]::new);
		}
		List<Resource> resources = new ArrayList<>();
		FilenameFilter filter = (dir, name) -> name.equals(fileName);
		for (File subDirectory : subDirectories) {
			File[] files = subDirectory.listFiles(filter);
			if (files != null) {
				Arrays.sort(files, FILE_NAME_COMPARATOR);
				Arrays.stream(files).map(FileSystemResource::new).forEach(resources::add);
			}
		}
		return resources.toArray(EMPTY_RESOURCES);
	}

	/**
	 * Get a multiple resources from a location pattern.
	 * @param location the location pattern
	 * @param type the type of resource to return
	 * @return the resources
	 * @see #isPattern(String)
	 */
	Resource[] getResources(String location, ResourceType type) {
		return getResources(location, type, false);
	}

	/**
	 * Get a multiple resources from a location pattern.
	 * @param location the location pattern
	 * @param allowClasspathAll the boolean value of allow param location contains
	 * 'classpath*:'
	 * @return the resources
	 * @see #isPattern(String)
	 */
	Resource[] getResources(String location, boolean allowClasspathAll) {
		ResourceType resourceType = location.endsWith("/") ? ResourceType.DIRECTORY : ResourceType.FILE;
		return getResources(location, resourceType, allowClasspathAll);
	}

	private Resource[] getResourcesFromClasspathAllPatternLocation(String location, int propNameLen) {
		PathMatchingResourcePatternResolver pathMatchingResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources;
		try {
			resources = pathMatchingResolver.getResources(location);
		}
		catch (IOException e) {
			throw new IllegalStateException(String.format("get location: '%s' error", location), e);
		}
		if (resources != null) {
			Arrays.sort(resources, Comparator.comparing(resource -> {
				String filename;
				FileSystemResource fileResource = (FileSystemResource) resource;
				filename = fileResource.getFile().getAbsolutePath();
				int idx = filename.lastIndexOf(File.separator, filename.length() - propNameLen);
				if (idx > -1) {
					filename = filename.substring(idx);
				}
				return filename;
			}));
		}
		return resources;
	}

	private boolean validatePatternAndCheckClasspathAll(String location, ResourceType type, boolean allowClasspathAll) {
		Assert.state(isPattern(location), () -> String.format("Location '%s' must be a pattern", location));
		boolean startsWithClasspathAll = location.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX);
		Assert.state((allowClasspathAll || !startsWithClasspathAll),
				() -> String.format("Location '%s' cannot use classpath wildcards", location));
		int countOfWilldcard = StringUtils.countOccurrencesOf(location, "*");
		Assert.state((startsWithClasspathAll ? (countOfWilldcard == 2) : countOfWilldcard == 1),
				() -> String.format("Location '%s' cannot contain multiple wildcards", location));
		if (type == ResourceType.DIRECTORY) {
			Assert.state(location.endsWith("*/"), () -> String.format("Location '%s' must end with '*/'", location));
		}
		return startsWithClasspathAll;
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

	private boolean isVisibleDirectory(File file) {
		return file.isDirectory() && !file.getName().startsWith("..");
	}

	/**
	 * Resource types that can be returned.
	 */
	enum ResourceType {

		/**
		 * Return file resources.
		 */
		FILE,

		/**
		 * Return directory resources.
		 */
		DIRECTORY

	}

}
