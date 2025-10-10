/*
 * Copyright 2012-present the original author or authors.
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
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.sbom.SbomEndpoint.SbomEndpointRuntimeHints;
import org.springframework.boot.actuate.sbom.SbomEndpoint.Sboms;
import org.springframework.boot.actuate.sbom.SbomProperties.Sbom;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SbomEndpoint}.
 *
 * @author Moritz Halbritter
 */
class SbomEndpointTests {

	private SbomProperties properties;

	@BeforeEach
	void setUp() {
		this.properties = new SbomProperties();
	}

	@Test
	void shouldListSboms() {
		this.properties.getApplication().setLocation("classpath:org/springframework/boot/actuate/sbom/cyclonedx.json");
		this.properties.getAdditional()
			.put("alpha", sbom("classpath:org/springframework/boot/actuate/sbom/cyclonedx.json"));
		this.properties.getAdditional()
			.put("beta", sbom("classpath:org/springframework/boot/actuate/sbom/cyclonedx.json"));
		Sboms sboms = createEndpoint().sboms();
		assertThat(sboms.ids()).containsExactly("alpha", "application", "beta");
	}

	@Test
	void shouldFailIfDuplicateSbomIdIsRegistered() {
		// This adds an SBOM with id 'application'
		this.properties.getApplication().setLocation("classpath:org/springframework/boot/actuate/sbom/cyclonedx.json");
		this.properties.getAdditional()
			.put("application", sbom("classpath:org/springframework/boot/actuate/sbom/cyclonedx.json"));
		assertThatIllegalStateException().isThrownBy(this::createEndpoint)
			.withMessage("Duplicate SBOM registration with id 'application'");
	}

	@Test
	void shouldUseLocationFromProperties() throws IOException {
		this.properties.getApplication().setLocation("classpath:org/springframework/boot/actuate/sbom/cyclonedx.json");
		Resource sbom = createEndpoint().sbom("application");
		assertThat(sbom).isNotNull();
		String content = sbom.getContentAsString(StandardCharsets.UTF_8);
		assertThat(content).contains("\"bomFormat\" : \"CycloneDX\"");
	}

	@Test
	void shouldFailIfNonExistingLocationIsGiven() {
		this.properties.getApplication().setLocation("classpath:does-not-exist.json");
		assertThatIllegalStateException().isThrownBy(() -> createEndpoint().sbom("application"))
			.withMessageContaining("Resource 'classpath:does-not-exist.json' doesn't exist");
	}

	@Test
	void shouldNotFailIfNonExistingOptionalLocationIsGiven() {
		this.properties.getApplication().setLocation("optional:classpath:does-not-exist.json");
		assertThat(createEndpoint().sbom("application")).isNull();
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new SbomEndpointRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("META-INF/sbom/bom.json")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("META-INF/sbom/application.cdx.json")).accepts(hints);
	}

	private Sbom sbom(String location) {
		Sbom result = new Sbom();
		result.setLocation(location);
		return result;
	}

	private SbomEndpoint createEndpoint() {
		return new SbomEndpoint(this.properties, new GenericApplicationContext());
	}

}
