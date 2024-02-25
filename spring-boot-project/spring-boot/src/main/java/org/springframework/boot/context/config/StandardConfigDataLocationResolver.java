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

package org.springframework.boot.context.config;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;

import org.springframework.boot.context.config.LocationResourceLoader.ResourceType;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
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

	static final String[] DEFAULT_CONFIG_NAMES = { "application" };

	private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9*]*?:)(.*$)");

	private static final Pattern EXTENSION_HINT_PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)\\](?!\\[)$");

	private static final String NO_PROFILE = null;

	private final Log logger;

	private final List<PropertySourceLoader> propertySourceLoaders;

	private final String[] configNames;

	private final LocationResourceLoader resourceLoader;

	/**
	 * Create a new {@link StandardConfigDataLocationResolver} instance.
	 * @param logFactory the factory for loggers to use
	 * @param binder a binder backed by the initial {@link Environment}
	 * @param resourceLoader a {@link ResourceLoader} used to load resources
	 */
	public StandardConfigDataLocationResolver(DeferredLogFactory logFactory, Binder binder,
			ResourceLoader resourceLoader) {
		this.logger = logFactory.getLog(StandardConfigDataLocationResolver.class);
		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				getClass().getClassLoader());
		this.configNames = getConfigNames(binder);
		this.resourceLoader = new LocationResourceLoader(resourceLoader);
	}

	/**
     * Retrieves the configuration names from the given binder.
     * 
     * @param binder the binder to retrieve the configuration names from
     * @return an array of configuration names
     * @throws IllegalArgumentException if any of the configuration names are invalid
     */
    private String[] getConfigNames(Binder binder) {
		String[] configNames = binder.bind(CONFIG_NAME_PROPERTY, String[].class).orElse(DEFAULT_CONFIG_NAMES);
		for (String configName : configNames) {
			validateConfigName(configName);
		}
		return configNames;
	}

	/**
     * Validates the configuration name to ensure it does not contain the '*' character.
     * 
     * @param name the configuration name to be validated
     * @throws IllegalStateException if the configuration name contains the '*' character
     */
    private void validateConfigName(String name) {
		Assert.state(!name.contains("*"), () -> "Config name '" + name + "' cannot contain '*'");
	}

	/**
     * Returns the order value for this object.
     * 
     * The order value indicates the position of this object in the execution order.
     * A lower value means a higher priority.
     * 
     * @return the order value for this object
     */
    @Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
     * Determines if the given ConfigDataLocation is resolvable.
     *
     * @param context  the ConfigDataLocationResolverContext
     * @param location the ConfigDataLocation to check
     * @return true if the location is resolvable, false otherwise
     */
    @Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return true;
	}

	/**
     * Resolves the given ConfigDataLocation by splitting it into individual references and resolving each reference.
     * 
     * @param context the ConfigDataLocationResolverContext
     * @param location the ConfigDataLocation to resolve
     * @return a list of resolved StandardConfigDataResource objects
     * @throws ConfigDataNotFoundException if the ConfigDataLocation cannot be resolved
     */
    @Override
	public List<StandardConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location) throws ConfigDataNotFoundException {
		return resolve(getReferences(context, location.split()));
	}

	/**
     * Retrieves the set of references for the given array of config data locations.
     * 
     * @param context The context for resolving config data locations.
     * @param configDataLocations The array of config data locations.
     * @return The set of references for the given config data locations.
     */
    private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation[] configDataLocations) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		for (ConfigDataLocation configDataLocation : configDataLocations) {
			references.addAll(getReferences(context, configDataLocation));
		}
		return references;
	}

	/**
     * Retrieves the set of references for a given config data location.
     * 
     * @param context The context for resolving the config data location.
     * @param configDataLocation The config data location to retrieve references for.
     * @return The set of references for the config data location.
     * @throws IllegalStateException if unable to load config data from the specified location.
     */
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

	/**
     * Resolves the profile-specific configuration data resources for the given location and profiles.
     * 
     * @param context The context for resolving the configuration data location.
     * @param location The configuration data location to resolve.
     * @param profiles The profiles for which to resolve the configuration data.
     * @return The list of resolved profile-specific configuration data resources.
     */
    @Override
	public List<StandardConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
			ConfigDataLocation location, Profiles profiles) {
		return resolve(getProfileSpecificReferences(context, location.split(), profiles));
	}

	/**
     * Retrieves the profile-specific references for the given config data locations and profiles.
     * 
     * @param context The context for resolving config data locations.
     * @param configDataLocations The array of config data locations.
     * @param profiles The profiles for which to retrieve the references.
     * @return The set of profile-specific references.
     */
    private Set<StandardConfigDataReference> getProfileSpecificReferences(ConfigDataLocationResolverContext context,
			ConfigDataLocation[] configDataLocations, Profiles profiles) {
		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
		for (String profile : profiles) {
			for (ConfigDataLocation configDataLocation : configDataLocations) {
				String resourceLocation = getResourceLocation(context, configDataLocation);
				references.addAll(getReferences(configDataLocation, resourceLocation, profile));
			}
		}
		return references;
	}

	/**
     * Returns the resource location for the given {@link ConfigDataLocation}.
     * 
     * @param context The {@link ConfigDataLocationResolverContext} object.
     * @param configDataLocation The {@link ConfigDataLocation} object.
     * @return The resource location.
     */
    private String getResourceLocation(ConfigDataLocationResolverContext context,
			ConfigDataLocation configDataLocation) {
		String resourceLocation = configDataLocation.getNonPrefixedValue(PREFIX);
		boolean isAbsolute = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
		if (isAbsolute) {
			return resourceLocation;
		}
		ConfigDataResource parent = context.getParent();
		if (parent instanceof StandardConfigDataResource resource) {
			String parentResourceLocation = resource.getReference().getResourceLocation();
			String parentDirectory = parentResourceLocation.substring(0, parentResourceLocation.lastIndexOf("/") + 1);
			return parentDirectory + resourceLocation;
		}
		return resourceLocation;
	}

	/**
     * Retrieves the set of references for the given config data location, resource location, and profile.
     * If the resource location is a directory, it calls the getReferencesForDirectory method to retrieve the references.
     * Otherwise, it calls the getReferencesForFile method.
     * 
     * @param configDataLocation The config data location to retrieve references for.
     * @param resourceLocation The resource location to retrieve references for.
     * @param profile The profile to retrieve references for.
     * @return The set of references for the given config data location, resource location, and profile.
     */
    private Set<StandardConfigDataReference> getReferences(ConfigDataLocation configDataLocation,
			String resourceLocation, String profile) {
		if (isDirectory(resourceLocation)) {
			return getReferencesForDirectory(configDataLocation, resourceLocation, profile);
		}
		return getReferencesForFile(configDataLocation, resourceLocation, profile);
	}

	/**
     * Retrieves the set of references for a given directory in a specific configuration data location and profile.
     * 
     * @param configDataLocation The configuration data location to retrieve references from.
     * @param directory The directory within the configuration data location to retrieve references for.
     * @param profile The profile to filter the references by.
     * @return The set of references for the specified directory, configuration data location, and profile.
     */
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

	/**
     * Retrieves a deque of StandardConfigDataReference objects for a given config name, config data location, directory, and profile.
     * 
     * @param name the name of the config
     * @param configDataLocation the config data location
     * @param directory the directory of the config
     * @param profile the profile of the config
     * @return a deque of StandardConfigDataReference objects
     */
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

	/**
     * Retrieves the set of references for a given file in a specific configuration data location and profile.
     * 
     * @param configDataLocation The configuration data location to search for the file.
     * @param file The file to retrieve references for.
     * @param profile The profile to filter the references by.
     * @return The set of references for the given file.
     * @throws IllegalStateException If the file extension is not known to any PropertySourceLoader.
     * @throws IllegalArgumentException If the location is meant to reference a directory but does not end in '/' or File.separator.
     */
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
		if (configDataLocation.isOptional()) {
			return Collections.emptySet();
		}
		throw new IllegalStateException("File extension is not known to any PropertySourceLoader. "
				+ "If the location is meant to reference a directory, it must end in '/' or File.separator");
	}

	/**
     * Returns the loadable file extension for the given loader and file.
     * 
     * @param loader the property source loader
     * @param file the file name
     * @return the loadable file extension, or null if not found
     */
    private String getLoadableFileExtension(PropertySourceLoader loader, String file) {
		for (String fileExtension : loader.getFileExtensions()) {
			if (StringUtils.endsWithIgnoreCase(file, fileExtension)) {
				return fileExtension;
			}
		}
		return null;
	}

	/**
     * Checks if the given resource location is a directory.
     * 
     * @param resourceLocation the resource location to check
     * @return true if the resource location is a directory, false otherwise
     */
    private boolean isDirectory(String resourceLocation) {
		return resourceLocation.endsWith("/") || resourceLocation.endsWith(File.separator);
	}

	/**
     * Resolves a set of StandardConfigDataReferences to a list of StandardConfigDataResources.
     * 
     * @param references the set of StandardConfigDataReferences to resolve
     * @return a list of resolved StandardConfigDataResources
     */
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

	/**
     * Resolves empty directories from a set of StandardConfigDataReferences.
     * 
     * @param references the set of StandardConfigDataReferences to resolve empty directories from
     * @return a collection of StandardConfigDataResources representing the resolved empty directories
     */
    private Collection<StandardConfigDataResource> resolveEmptyDirectories(
			Set<StandardConfigDataReference> references) {
		Set<StandardConfigDataResource> empty = new LinkedHashSet<>();
		for (StandardConfigDataReference reference : references) {
			if (reference.getDirectory() != null) {
				empty.addAll(resolveEmptyDirectories(reference));
			}
		}
		return empty;
	}

	/**
     * Resolves empty directories based on the given StandardConfigDataReference.
     * If the resource location is not a pattern, it calls resolveNonPatternEmptyDirectories method.
     * If the resource location is a pattern, it calls resolvePatternEmptyDirectories method.
     * 
     * @param reference the StandardConfigDataReference object containing the resource location
     * @return a Set of StandardConfigDataResource objects representing the resolved empty directories
     */
    private Set<StandardConfigDataResource> resolveEmptyDirectories(StandardConfigDataReference reference) {
		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
			return resolveNonPatternEmptyDirectories(reference);
		}
		return resolvePatternEmptyDirectories(reference);
	}

	/**
     * Resolves non-pattern empty directories based on the given reference.
     * 
     * @param reference the reference to the standard config data
     * @return a set of StandardConfigDataResource objects representing the resolved non-pattern empty directories
     */
    private Set<StandardConfigDataResource> resolveNonPatternEmptyDirectories(StandardConfigDataReference reference) {
		Resource resource = this.resourceLoader.getResource(reference.getDirectory());
		return (resource instanceof ClassPathResource || !resource.exists()) ? Collections.emptySet()
				: Collections.singleton(new StandardConfigDataResource(reference, resource, true));
	}

	/**
     * Resolves the pattern for empty directories in the given {@link StandardConfigDataReference}.
     * 
     * @param reference the {@link StandardConfigDataReference} containing the directory pattern
     * @return a {@link Set} of {@link StandardConfigDataResource} representing the empty directories
     * @throws ConfigDataLocationNotFoundException if the location is not optional and no subdirectories are found
     */
    private Set<StandardConfigDataResource> resolvePatternEmptyDirectories(StandardConfigDataReference reference) {
		Resource[] subdirectories = this.resourceLoader.getResources(reference.getDirectory(), ResourceType.DIRECTORY);
		ConfigDataLocation location = reference.getConfigDataLocation();
		if (!location.isOptional() && ObjectUtils.isEmpty(subdirectories)) {
			String message = String.format("Config data location '%s' contains no subdirectories", location);
			throw new ConfigDataLocationNotFoundException(location, message, null);
		}
		return Arrays.stream(subdirectories)
			.filter(Resource::exists)
			.map((resource) -> new StandardConfigDataResource(reference, resource, true))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
     * Resolves the given StandardConfigDataReference to a list of StandardConfigDataResource objects.
     * If the resource location is not a pattern, it calls the resolveNonPattern method.
     * If the resource location is a pattern, it calls the resolvePattern method.
     * 
     * @param reference The StandardConfigDataReference to be resolved.
     * @return A list of StandardConfigDataResource objects resolved from the given reference.
     */
    private List<StandardConfigDataResource> resolve(StandardConfigDataReference reference) {
		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
			return resolveNonPattern(reference);
		}
		return resolvePattern(reference);
	}

	/**
     * Resolves a non-pattern StandardConfigDataReference to a list of StandardConfigDataResource objects.
     * 
     * @param reference The StandardConfigDataReference to resolve.
     * @return A list of StandardConfigDataResource objects representing the resolved resources.
     */
    private List<StandardConfigDataResource> resolveNonPattern(StandardConfigDataReference reference) {
		Resource resource = this.resourceLoader.getResource(reference.getResourceLocation());
		if (!resource.exists() && reference.isSkippable()) {
			logSkippingResource(reference);
			return Collections.emptyList();
		}
		return Collections.singletonList(createConfigResourceLocation(reference, resource));
	}

	/**
     * Resolves the pattern specified in the given StandardConfigDataReference and returns a list of resolved StandardConfigDataResource objects.
     * 
     * @param reference The StandardConfigDataReference object containing the pattern to be resolved.
     * @return A list of resolved StandardConfigDataResource objects.
     */
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

	/**
     * Logs the skipping of a missing resource.
     * 
     * @param reference the reference to the missing resource
     */
    private void logSkippingResource(StandardConfigDataReference reference) {
		this.logger.trace(LogMessage.format("Skipping missing resource %s", reference));
	}

	/**
     * Creates a new StandardConfigDataResource object with the given reference and resource.
     * 
     * @param reference the reference to the config data
     * @param resource the resource containing the config data
     * @return a new StandardConfigDataResource object
     */
    private StandardConfigDataResource createConfigResourceLocation(StandardConfigDataReference reference,
			Resource resource) {
		return new StandardConfigDataResource(reference, resource);
	}

}
