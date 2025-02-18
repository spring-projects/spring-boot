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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.sbom.SbomEndpoint.SbomEndpointRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
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
@ImportRuntimeHints(SbomEndpointRuntimeHints.class)
public class SbomEndpoint {

	static final String APPLICATION_SBOM_ID = "application";

	private static final List<AutodetectedSbom> AUTODETECTED_SBOMS = List.of(
			new AutodetectedSbom(APPLICATION_SBOM_ID, "classpath:META-INF/sbom/bom.json", true),
			new AutodetectedSbom(APPLICATION_SBOM_ID, "classpath:META-INF/sbom/application.cdx.json", true),
			new AutodetectedSbom("native-image", "classpath:META-INF/native-image/sbom.json", false));

	private final SbomProperties properties;

	private final ResourceLoader resourceLoader;

	private final Map<String, Resource> sboms;

	public SbomEndpoint(SbomProperties properties, ResourceLoader resourceLoader) {
		this.properties = properties;
		this.resourceLoader = resourceLoader;
		this.sboms = loadSboms();
	}

	private Map<String, Resource> loadSboms() {
		Map<String, Resource> sboms = new HashMap<>();
		addConfiguredApplicationSbom(sboms);
		addAdditionalSboms(sboms);
		addAutodetectedSboms(sboms);
		return Collections.unmodifiableMap(sboms);
	}

	private void addConfiguredApplicationSbom(Map<String, Resource> sboms) {
		String location = this.properties.getApplication().getLocation();
		if (!StringUtils.hasLength(location)) {
			return;
		}
		Resource resource = loadResource(location);
		if (resource != null) {
			sboms.put(APPLICATION_SBOM_ID, resource);
		}
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

	private void addAutodetectedSboms(Map<String, Resource> sboms) {
		for (AutodetectedSbom sbom : AUTODETECTED_SBOMS) {
			if (sboms.containsKey(sbom.id())) {
				continue;
			}
			Resource resource = this.resourceLoader.getResource(sbom.resource());
			if (resource.exists()) {
				sboms.put(sbom.id(), resource);
			}
		}
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

	@ReadOperation
	Sboms sboms() {
		return new Sboms(new TreeSet<>(this.sboms.keySet()));
	}

	@ReadOperation
	Resource sbom(@Selector String id) {
		return this.sboms.get(id);
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

	private record AutodetectedSbom(String id, String resource, boolean needsHints) {
		void registerHintsIfNeeded(RuntimeHints hints) {
			if (this.needsHints) {
				hints.resources().registerPattern(stripClasspathPrefix(this.resource));
			}
		}

		private String stripClasspathPrefix(String location) {
			return location.substring("classpath:".length());
		}
	}

	static class SbomEndpointRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			for (AutodetectedSbom sbom : AUTODETECTED_SBOMS) {
				sbom.registerHintsIfNeeded(hints);
			}
		}

	}

}
