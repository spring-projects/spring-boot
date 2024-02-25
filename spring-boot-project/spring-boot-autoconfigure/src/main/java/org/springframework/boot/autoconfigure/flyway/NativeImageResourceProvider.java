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

package org.springframework.boot.autoconfigure.flyway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.resource.classpath.ClassPathResource;
import org.flywaydb.core.internal.scanner.Scanner;
import org.flywaydb.core.internal.util.StringUtils;

import org.springframework.core.NativeDetector;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * A Flyway {@link ResourceProvider} which supports GraalVM native-image.
 * <p>
 * It delegates work to Flyways {@link Scanner}, and additionally uses
 * {@link PathMatchingResourcePatternResolver} to find migration files in a native image.
 *
 * @author Moritz Halbritter
 */
class NativeImageResourceProvider implements ResourceProvider {

	private final Scanner<?> scanner;

	private final ClassLoader classLoader;

	private final Collection<Location> locations;

	private final Charset encoding;

	private final boolean failOnMissingLocations;

	private final List<LocatedResource> locatedResources = new ArrayList<>();

	private final Lock lock = new ReentrantLock();

	private boolean initialized;

	/**
     * Constructs a new NativeImageResourceProvider with the specified parameters.
     * 
     * @param scanner the scanner used to scan for native image resources
     * @param classLoader the class loader used to load the native image resources
     * @param locations the collection of locations where the native image resources are located
     * @param encoding the character encoding used to read the native image resources
     * @param failOnMissingLocations flag indicating whether to fail if any of the specified locations are missing
     */
    NativeImageResourceProvider(Scanner<?> scanner, ClassLoader classLoader, Collection<Location> locations,
			Charset encoding, boolean failOnMissingLocations) {
		this.scanner = scanner;
		this.classLoader = classLoader;
		this.locations = locations;
		this.encoding = encoding;
		this.failOnMissingLocations = failOnMissingLocations;
	}

	/**
     * Retrieves a loadable resource with the given name.
     * 
     * @param name the name of the resource to retrieve
     * @return the loadable resource with the given name, or null if not found
     */
    @Override
	public LoadableResource getResource(String name) {
		if (!NativeDetector.inNativeImage()) {
			return this.scanner.getResource(name);
		}
		LoadableResource resource = this.scanner.getResource(name);
		if (resource != null) {
			return resource;
		}
		if (this.classLoader.getResource(name) == null) {
			return null;
		}
		return new ClassPathResource(null, name, this.classLoader, this.encoding);
	}

	/**
     * Retrieves a collection of loadable resources based on the given prefix and suffixes.
     * If the application is not running in a native image, the resources are obtained using the scanner.
     * Otherwise, the resources are obtained from the located resources.
     * 
     * @param prefix   the prefix of the resource filenames
     * @param suffixes an array of suffixes to filter the resource filenames
     * @return a collection of loadable resources
     */
    @Override
	public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
		if (!NativeDetector.inNativeImage()) {
			return this.scanner.getResources(prefix, suffixes);
		}
		ensureInitialized();
		Predicate<LocatedResource> matchesPrefixAndSuffixes = (locatedResource) -> StringUtils
			.startsAndEndsWith(locatedResource.resource.getFilename(), prefix, suffixes);
		List<LoadableResource> result = new ArrayList<>();
		result.addAll(this.scanner.getResources(prefix, suffixes));
		this.locatedResources.stream()
			.filter(matchesPrefixAndSuffixes)
			.map(this::asClassPathResource)
			.forEach(result::add);
		return result;
	}

	/**
     * Converts a LocatedResource object to a ClassPathResource object.
     * 
     * @param locatedResource the LocatedResource object to convert
     * @return the converted ClassPathResource object
     */
    private ClassPathResource asClassPathResource(LocatedResource locatedResource) {
		Location location = locatedResource.location();
		String fileNameWithAbsolutePath = location.getPath() + "/" + locatedResource.resource().getFilename();
		return new ClassPathResource(location, fileNameWithAbsolutePath, this.classLoader, this.encoding);
	}

	/**
     * Ensures that the NativeImageResourceProvider is initialized.
     * 
     * This method acquires a lock to ensure thread safety. If the NativeImageResourceProvider
     * has not been initialized yet, it calls the initialize() method to perform the initialization
     * and sets the initialized flag to true.
     * 
     * @throws IllegalStateException if the NativeImageResourceProvider is already initialized
     * @throws InterruptedException if the thread is interrupted while waiting to acquire the lock
     */
    private void ensureInitialized() {
		this.lock.lock();
		try {
			if (!this.initialized) {
				initialize();
				this.initialized = true;
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
     * Initializes the NativeImageResourceProvider.
     * 
     * This method uses a PathMatchingResourcePatternResolver to resolve the locations of the resources.
     * It iterates through each location and checks if it is a classpath location.
     * If it is not a classpath location, it continues to the next location.
     * If the root resource for the location does not exist, it checks if the failOnMissingLocations flag is set.
     * If the flag is set, it throws a FlywayException with an appropriate message.
     * If the flag is not set, it continues to the next location.
     * If the root resource exists, it retrieves all the resources within the location using the getResources method.
     * It then adds each located resource to the locatedResources list.
     */
    private void initialize() {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		for (Location location : this.locations) {
			if (!location.isClassPath()) {
				continue;
			}
			Resource root = resolver.getResource(location.getDescriptor());
			if (!root.exists()) {
				if (this.failOnMissingLocations) {
					throw new FlywayException("Location " + location.getDescriptor() + " doesn't exist");
				}
				continue;
			}
			Resource[] resources = getResources(resolver, location, root);
			for (Resource resource : resources) {
				this.locatedResources.add(new LocatedResource(resource, location));
			}
		}
	}

	/**
     * Retrieves an array of resources located at the specified root path.
     * 
     * @param resolver the PathMatchingResourcePatternResolver used to resolve the resources
     * @param location the Location of the resources
     * @param root the root Resource from which to retrieve the resources
     * @return an array of Resource objects representing the resources located at the root path
     * @throws UncheckedIOException if an IOException occurs while listing the resources
     */
    private Resource[] getResources(PathMatchingResourcePatternResolver resolver, Location location, Resource root) {
		try {
			return resolver.getResources(root.getURI() + "/*");
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to list resources for " + location.getDescriptor(), ex);
		}
	}

	private record LocatedResource(Resource resource, Location location) {

	}

}
