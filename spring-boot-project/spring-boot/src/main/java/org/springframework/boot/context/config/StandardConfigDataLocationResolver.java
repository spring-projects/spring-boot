/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import org.springframework.boot.context.config.LocationResourceLoader.ResourceType;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigDataLocationResolver} for standard locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.4.0
 */
public class StandardConfigDataLocationResolver
		implements ConfigDataLocationResolver<StandardConfigDataResource>, Ordered {

	private static final String PREFIX = "resource:";

	static final String CONFIG_NAME_PROPERTY = "spring.config.name";

	private static final String[] DEFAULT_CONFIG_NAMES = { "application" };

	private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9*]*?:)(.*$)");

	private static final Pattern EXTENSION_HINT_PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)\\](?!\\[)$");

	private static final String NO_PROFILE = null;

	private final Log logger;

	private final List<PropertySourceLoader> propertySourceLoaders;

	private final String[] configNames;

	private final LocationResourceLoader resourceLoader;

	/**
	 * Create a new {@link StandardConfigDataLocationResolver} instance.
	 * @param logger the logger to use
	 * @param binder a binder backed by the initial {@link Environment}
	 * @param resourceLoader a {@link ResourceLoader} used to load resources
	 */
	public StandardConfigDataLocationResolver(Log logger, Binder binder, ResourceLoader resourceLoader) {
		this.logger = logger;
		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				getClass().getClassLoader());
		this.configNames = getConfigNames(binder);
		this.resourceLoader = new LocationResourceLoader(resourceLoader);
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
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return true;
	}

	@Override
	public List<StandardConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataNotFoundException {
		return resolve(getReferences(context, location));
	}

	private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation configDataLocation) {
		String resourceLocation = getResourceLocation(context, configDataLocation);
		try {
			if (isDirectory(resourceLocation)) {
				return getReferencesForDirectory(configDataLocation, resourceLocation, NO_PROFILE);
			}
			return getReferencesForFile(configDataLocation, resourceLocation, NO_PROFILE);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to load config data from '" + configDataLocation + "'", ex);
		}
	}

	@Override
	public List<StandardConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
			ConfigDataLocation location, Profiles profiles) {
		return resolve(getProfileSpecificReferences(context, location, profiles));
	}

	private Set<StandardConfigDataReference> getProfileSpecificReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation configDataLocation, Profiles profiles) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		String resourceLocation = getResourceLocation(context, configDataLocation);
		for (String profile : profiles) {
			references.addAll(getReferences(configDataLocation, resourceLocation, profile));
		}
		return references;
	}

	private String getResourceLocation(ConfigDataLocationResolverContext context,
			ConfigDataLocation configDataLocation) {
		String resourceLocation = configDataLocation.getNonPrefixedValue(PREFIX);
		boolean isAbsolute = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
		if (isAbsolute) {
			return resourceLocation;
		}
		ConfigDataResource parent = context.getParent();
		if (parent instanceof StandardConfigDataResource) {
			String parentResourceLocation = ((StandardConfigDataResource) parent).getReference().getResourceLocation();
			String parentDirectory = parentResourceLocation.substring(0, parentResourceLocation.lastIndexOf("/") + 1);
			return parentDirectory + resourceLocation;
		}
		return resourceLocation;
	}

	private Set<StandardConfigDataReference> getReferences(ConfigDataLocation configDataLocation,
			String resourceLocation, String profile) {
		if (isDirectory(resourceLocation)) {
			return getReferencesForDirectory(configDataLocation, resourceLocation, profile);
		}
		return getReferencesForFile(configDataLocation, resourceLocation, profile);
	}

	private Set<StandardConfigDataReference> getReferencesForDirectory(ConfigDataLocation configDataLocation,
			String directory, String profile) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		for (String name : this.configNames) {
			Deque<StandardConfigDataReference> referencesForName = getReferencesForConfigName(name, configDataLocation,
					directory, profile);
			references.addAll(referencesForName);
		}
		return references;
	}

	private Deque<StandardConfigDataReference> getReferencesForConfigName(String name,
			ConfigDataLocation configDataLocation, String directory, String profile) {
		Deque<StandardConfigDataReference> references = new ArrayDeque<>();
		for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
			for (String extension : propertySourceLoader.getFileExtensions()) {
				StandardConfigDataReference reference = new StandardConfigDataReference(configDataLocation, directory,
						directory + name, profile, extension, propertySourceLoader);
				if (!references.contains(reference)) {
					references.addFirst(reference);
				}
			}
		}
		return references;
	}

	private Set<StandardConfigDataReference> getReferencesForFile(ConfigDataLocation configDataLocation, String file,
			String profile) {
		Matcher extensionHintMatcher = EXTENSION_HINT_PATTERN.matcher(file);
		boolean extensionHintLocation = extensionHintMatcher.matches();
		if (extensionHintLocation) {
			file = extensionHintMatcher.group(1) + extensionHintMatcher.group(2);
		}
		for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
			String extension = getLoadableFileExtension(propertySourceLoader, file);
			if (extension != null) {
				String root = file.substring(0, file.length() - extension.length() - 1);
				StandardConfigDataReference reference = new StandardConfigDataReference(configDataLocation, null, root,
						profile, (!extensionHintLocation) ? extension : null, propertySourceLoader);
				return Collections.singleton(reference);
			}
		}
		throw new IllegalStateException("File extension is not known to any PropertySourceLoader. "
				+ "If the location is meant to reference a directory, it must end in '/'");
	}

	private String getLoadableFileExtension(PropertySourceLoader loader, String file) {
		for (String fileExtension : loader.getFileExtensions()) {
			if (StringUtils.endsWithIgnoreCase(file, fileExtension)) {
				return fileExtension;
			}
		}
		return null;
	}

	private boolean isDirectory(String resourceLocation) {
		return resourceLocation.endsWith("/");
	}

	private List<StandardConfigDataResource> resolve(Set<StandardConfigDataReference> references) {
		List<StandardConfigDataResource> resolved = new ArrayList<>();
		for (StandardConfigDataReference reference : references) {
			resolved.addAll(resolve(reference));
		}
		if (resolved.isEmpty()) {
			resolved.addAll(resolveEmptyDirectories(references));
		}
		return resolved;
	}

	private Collection<StandardConfigDataResource> resolveEmptyDirectories(
			Set<StandardConfigDataReference> references) {
		Set<StandardConfigDataResource> empty = new LinkedHashSet<>();
		for (StandardConfigDataReference reference : references) {
			if (reference.isMandatoryDirectory()) {
				Resource resource = this.resourceLoader.getResource(reference.getDirectory());
				if (resource instanceof ClassPathResource) {
					continue;
				}
				StandardConfigDataResource configDataResource = new StandardConfigDataResource(reference, resource);
				ConfigDataResourceNotFoundException.throwIfDoesNotExist(configDataResource, resource);
				empty.add(new StandardConfigDataResource(reference, resource, true));
			}
		}
		return empty;
	}

	private List<StandardConfigDataResource> resolve(StandardConfigDataReference reference) {
		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
			return resolveNonPattern(reference);
		}
		return resolvePattern(reference);
	}

	private List<StandardConfigDataResource> resolveNonPattern(StandardConfigDataReference reference) {
		Resource resource = this.resourceLoader.getResource(reference.getResourceLocation());
		if (!resource.exists() && reference.isSkippable()) {
			logSkippingResource(reference);
			return Collections.emptyList();
		}
		return Collections.singletonList(createConfigResourceLocation(reference, resource));
	}

	private List<StandardConfigDataResource> resolvePattern(StandardConfigDataReference reference) {
		List<StandardConfigDataResource> resolved = new ArrayList<>();
		for (Resource resource : this.resourceLoader.getResources(reference.getResourceLocation(), ResourceType.FILE)) {
			if (!resource.exists() && reference.isSkippable()) {
				logSkippingResource(reference);
			}
			else {
				resolved.add(createConfigResourceLocation(reference, resource));
			}
		}
		return resolved;
	}

	private void logSkippingResource(StandardConfigDataReference reference) {
		this.logger.trace(LogMessage.format("Skipping missing resource %s", reference));
	}

	private StandardConfigDataResource createConfigResourceLocation(StandardConfigDataReference reference,
			Resource resource) {
		return new StandardConfigDataResource(reference, resource);
	}

}
