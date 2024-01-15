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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.sbom.SbomProperties.Sbom;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the {@link SbomEndpoint}.
 *
 * @author Moritz Halbritter
 * @since 3.3.0
 */
@EndpointWebExtension(endpoint = SbomEndpoint.class)
public class SbomEndpointWebExtension {

	private final SbomEndpoint sbomEndpoint;

	private final SbomProperties properties;

	private final Map<String, SbomType> detectedMediaTypeCache = new ConcurrentHashMap<>();

	public SbomEndpointWebExtension(SbomEndpoint sbomEndpoint, SbomProperties properties) {
		this.sbomEndpoint = sbomEndpoint;
		this.properties = properties;
	}

	@ReadOperation
	WebEndpointResponse<Resource> sbom(@Selector String id) {
		Resource resource = this.sbomEndpoint.sbom(id);
		if (resource == null) {
			return new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
		}
		MimeType type = getMediaType(id, resource);
		return (type != null) ? new WebEndpointResponse<>(resource, type) : new WebEndpointResponse<>(resource);
	}

	private MimeType getMediaType(String id, Resource resource) {
		if (SbomEndpoint.APPLICATION_SBOM_ID.equals(id) && this.properties.getApplication().getMediaType() != null) {
			return this.properties.getApplication().getMediaType();
		}
		Sbom sbomProperties = this.properties.getAdditional().get(id);
		if (sbomProperties != null && sbomProperties.getMediaType() != null) {
			return sbomProperties.getMediaType();
		}
		return this.detectedMediaTypeCache.computeIfAbsent(id, (ignored) -> {
			try {
				return detectSbomType(resource);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to detect type of resource '%s'".formatted(resource), ex);
			}
		}).getMediaType();
	}

	private SbomType detectSbomType(Resource resource) throws IOException {
		String content = resource.getContentAsString(StandardCharsets.UTF_8);
		for (SbomType candidate : SbomType.values()) {
			if (candidate.matches(content)) {
				return candidate;
			}
		}
		return SbomType.UNKNOWN;
	}

	enum SbomType {

		CYCLONE_DX(MimeType.valueOf("application/vnd.cyclonedx+json")) {
			@Override
			boolean matches(String content) {
				return content.replaceAll("\\s", "").contains("\"bomFormat\":\"CycloneDX\"");
			}
		},
		SPDX(MimeType.valueOf("application/spdx+json")) {
			@Override
			boolean matches(String content) {
				return content.contains("\"spdxVersion\"");
			}
		},
		SYFT(MimeType.valueOf("application/vnd.syft+json")) {
			@Override
			boolean matches(String content) {
				return content.contains("\"FoundBy\"") || content.contains("\"foundBy\"");
			}
		},
		UNKNOWN(null) {
			@Override
			boolean matches(String content) {
				return false;
			}
		};

		private final MimeType mediaType;

		SbomType(MimeType mediaType) {
			this.mediaType = mediaType;
		}

		MimeType getMediaType() {
			return this.mediaType;
		}

		abstract boolean matches(String content);

	}

}
