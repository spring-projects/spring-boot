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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.sbom.SbomEndpointWebExtension.SbomType;
import org.springframework.boot.actuate.sbom.SbomProperties.Sbom;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SbomEndpointWebExtension}.
 *
 * @author Moritz Halbritter
 */
class SbomEndpointWebExtensionTests {

	private SbomProperties properties;

	@BeforeEach
	void setUp() {
		this.properties = new SbomProperties();
	}

	@Test
	void shouldReturnHttpOk() {
		this.properties.getApplication().setLocation("classpath:sbom/cyclonedx.json");
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldReturnNotFoundIfResourceDoesntExist() {
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	void shouldAutoDetectContentTypeForCycloneDx() {
		this.properties.getApplication().setLocation("classpath:sbom/cyclonedx.json");
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getContentType()).isEqualTo(MimeType.valueOf("application/vnd.cyclonedx+json"));
	}

	@Test
	void shouldAutoDetectContentTypeForSpdx() {
		this.properties.getApplication().setLocation("classpath:sbom/spdx.json");
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getContentType()).isEqualTo(MimeType.valueOf("application/spdx+json"));
	}

	@Test
	void shouldAutoDetectContentTypeForSyft() {
		this.properties.getApplication().setLocation("classpath:sbom/syft.json");
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getContentType()).isEqualTo(MimeType.valueOf("application/vnd.syft+json"));
	}

	@Test
	void shouldSupportUnknownFiles() {
		this.properties.getApplication().setLocation("classpath:git.properties");
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getContentType()).isNull();
	}

	@Test
	void shouldUseContentTypeIfSet() {
		this.properties.getApplication().setLocation("classpath:sbom/cyclonedx.json");
		this.properties.getApplication().setMediaType(MimeType.valueOf("text/plain"));
		WebEndpointResponse<Resource> response = createWebExtension().sbom("application");
		assertThat(response.getContentType()).isEqualTo(MimeType.valueOf("text/plain"));
	}

	@Test
	void shouldUseContentTypeForAdditionalSbomsIfSet() {
		this.properties.getAdditional()
			.put("alpha", sbom("classpath:sbom/cyclonedx.json", MediaType.valueOf("text/plain")));
		WebEndpointResponse<Resource> response = createWebExtension().sbom("alpha");
		assertThat(response.getContentType()).isEqualTo(MimeType.valueOf("text/plain"));
	}

	@ParameterizedTest
	@EnumSource(value = SbomType.class, names = "UNKNOWN", mode = Mode.EXCLUDE)
	void shouldAutodetectFormats(SbomType type) throws IOException {
		String content = getSbomContent(type);
		assertThat(type.matches(content)).isTrue();
		Arrays.stream(SbomType.values())
			.filter((candidate) -> candidate != type)
			.forEach((notType) -> assertThat(notType.matches(content)).isFalse());
	}

	private String getSbomContent(SbomType type) throws IOException {
		return switch (type) {
			case CYCLONE_DX -> readResource("/sbom/cyclonedx.json");
			case SPDX -> readResource("/sbom/spdx.json");
			case SYFT -> readResource("/sbom/syft.json");
			case UNKNOWN -> throw new IllegalArgumentException("UNKNOWN is not supported");
		};
	}

	private String readResource(String resource) throws IOException {
		try (InputStream stream = getClass().getResourceAsStream(resource)) {
			assertThat(stream).as("Resource '%s'", resource).isNotNull();
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private Sbom sbom(String location, MimeType mediaType) {
		Sbom sbom = new Sbom();
		sbom.setLocation(location);
		sbom.setMediaType(mediaType);
		return sbom;
	}

	private SbomEndpointWebExtension createWebExtension() {
		SbomEndpoint endpoint = new SbomEndpoint(this.properties, new GenericApplicationContext());
		return new SbomEndpointWebExtension(endpoint, this.properties);
	}

}
