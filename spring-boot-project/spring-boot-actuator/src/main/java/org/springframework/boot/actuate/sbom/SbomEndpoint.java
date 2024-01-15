/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.sbom;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint @Endpoint} to expose an SBOM.
 *
 * @author Moritz Halbritter
 * @since 3.3.0
 */
@Endpoint(id = "sbom")
public class SbomEndpoint {

	private static final List<String> DEFAULT_APPLICATION_SBOM_LOCATIONS = List.of("classpath:META-INF/sbom/bom.json",
			"classpath:META-INF/sbom/application.cdx.json");

	static final String APPLICATION_SBOM_ID = "application";

	private final SbomProperties properties;

	private final ResourceLoader resourceLoader;

	private final Map<String, Resource> sboms;

	public SbomEndpoint(SbomProperties properties, ResourceLoader resourceLoader) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
		this.sboms = Collections.unmodifiableMap(getSboms());
	}

	private Map<String, Resource> getSboms() {
		Map<String, Resource> result = new HashMap<>();
		addKnownSboms(result);
		addAdditionalSboms(result);
		return result;
	}

	private void addAdditionalSboms(Map<String, Resource> result) {
		this.properties.getAdditional().forEach((id, sbom) -> {
			Resource resource = loadResource(sbom.getLocation());
			if (resource != null) {
				if (result.putIfAbsent(id, resource) != null) {
					throw new IllegalStateException("Duplicate SBOM registration with id '%s'".formatted(id));
				}
			}
		});
	}

	private void addKnownSboms(Map<String, Resource> result) {
		Resource applicationSbom = getApplicationSbom();
		if (applicationSbom != null) {
			result.put(APPLICATION_SBOM_ID, applicationSbom);
		}
	}

	@ReadOperation
	Sboms sboms() {
		return new Sboms(new TreeSet<>(this.sboms.keySet()));
	}

	@ReadOperation
	Resource sbom(@Selector String id) {
		return this.sboms.get(id);
	}

	private Resource getApplicationSbom() {
		if (StringUtils.hasLength(this.properties.getApplication().getLocation())) {
			return loadResource(this.properties.getApplication().getLocation());
		}
		for (String location : DEFAULT_APPLICATION_SBOM_LOCATIONS) {
			Resource resource = this.resourceLoader.getResource(location);
			if (resource.exists()) {
				return resource;
			}
		}
		return null;
	}

	private Resource loadResource(String location) {
		if (location == null) {
			return null;
		}
		Location parsedLocation = Location.of(location);
		Resource resource = this.resourceLoader.getResource(parsedLocation.location());
		if (resource.exists()) {
			return resource;
		}
		if (parsedLocation.optional()) {
			return null;
		}
		throw new IllegalStateException("Resource '%s' doesn't exist and it's not marked optional".formatted(location));
	}

	record Sboms(Collection<String> ids) implements OperationResponseBody {
	}

	private record Location(String location, boolean optional) {

		private static final String OPTIONAL_PREFIX = "optional:";

		static Location of(String location) {
			boolean optional = isOptional(location);
			return new Location(optional ? stripOptionalPrefix(location) : location, optional);
		}

		private static boolean isOptional(String location) {
			return location.startsWith(OPTIONAL_PREFIX);
		}

		private static String stripOptionalPrefix(String location) {
			return location.substring(OPTIONAL_PREFIX.length());
		}
	}

}
