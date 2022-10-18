/*
 * Copyright 2012-2022 the original author or authors.
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

	private final List<ResourceWithLocation> resources = new ArrayList<>();

	private final Lock lock = new ReentrantLock();

	private boolean initialized;

	NativeImageResourceProvider(Scanner<?> scanner, ClassLoader classLoader, Collection<Location> locations,
			Charset encoding, boolean failOnMissingLocations) {
		this.scanner = scanner;
		this.classLoader = classLoader;
		this.locations = locations;
		this.encoding = encoding;
		this.failOnMissingLocations = failOnMissingLocations;
	}

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

	@Override
	public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
		if (!NativeDetector.inNativeImage()) {
			return this.scanner.getResources(prefix, suffixes);
		}
		ensureInitialized();
		List<LoadableResource> result = new ArrayList<>(this.scanner.getResources(prefix, suffixes));
		this.resources.stream().filter((r) -> StringUtils.startsAndEndsWith(r.resource.getFilename(), prefix, suffixes))
				.map((r) -> (LoadableResource) new ClassPathResource(r.location(),
						r.location().getPath() + "/" + r.resource().getFilename(), this.classLoader, this.encoding))
				.forEach(result::add);
		return result;
	}

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
			Resource[] resources;
			try {
				resources = resolver.getResources(root.getURI() + "/*");
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to list resources for " + location.getDescriptor(), ex);
			}
			for (Resource resource : resources) {
				this.resources.add(new ResourceWithLocation(resource, location));
			}
		}
	}

	private record ResourceWithLocation(Resource resource, Location location) {
	}

}
