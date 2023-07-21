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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BuilderBuildpack}.
 *
 * @author Scott Frederick
 */
class BuilderBuildpackTests extends AbstractJsonTests {

	private BuildpackResolverContext resolverContext;

	@BeforeEach
	void setUp() throws Exception {
		BuilderMetadata metadata = BuilderMetadata.fromJson(getContentAsString("builder-metadata.json"));
		this.resolverContext = mock(BuildpackResolverContext.class);
		given(this.resolverContext.getBuildpackMetadata()).willReturn(metadata.getBuildpacks());
	}

	@Test
	void resolveWhenFullyQualifiedBuildpackWithVersionResolves() throws Exception {
		BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:paketo-buildpacks/spring-boot@3.5.0");
		Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack.getCoordinates())
			.isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
		assertThatNoLayersAreAdded(buildpack);
	}

	@Test
	void resolveWhenFullyQualifiedBuildpackWithoutVersionResolves() throws Exception {
		BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:paketo-buildpacks/spring-boot");
		Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack.getCoordinates())
			.isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
		assertThatNoLayersAreAdded(buildpack);
	}

	@Test
	void resolveWhenUnqualifiedBuildpackWithVersionResolves() throws Exception {
		BuildpackReference reference = BuildpackReference.of("paketo-buildpacks/spring-boot@3.5.0");
		Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack.getCoordinates())
			.isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
		assertThatNoLayersAreAdded(buildpack);
	}

	@Test
	void resolveWhenUnqualifiedBuildpackWithoutVersionResolves() throws Exception {
		BuildpackReference reference = BuildpackReference.of("paketo-buildpacks/spring-boot");
		Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack.getCoordinates())
			.isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
		assertThatNoLayersAreAdded(buildpack);
	}

	@Test
	void resolveWhenFullyQualifiedBuildpackWithVersionNotInBuilderThrowsException() {
		BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:example/buildpack1@1.2.3");
		assertThatIllegalArgumentException().isThrownBy(() -> BuilderBuildpack.resolve(this.resolverContext, reference))
			.withMessageContaining("'urn:cnb:builder:example/buildpack1@1.2.3'")
			.withMessageContaining("not found in builder");
	}

	@Test
	void resolveWhenFullyQualifiedBuildpackWithoutVersionNotInBuilderThrowsException() {
		BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:example/buildpack1");
		assertThatIllegalArgumentException().isThrownBy(() -> BuilderBuildpack.resolve(this.resolverContext, reference))
			.withMessageContaining("'urn:cnb:builder:example/buildpack1'")
			.withMessageContaining("not found in builder");
	}

	@Test
	void resolveWhenUnqualifiedBuildpackNotInBuilderReturnsNull() {
		BuildpackReference reference = BuildpackReference.of("example/buildpack1@1.2.3");
		Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
		assertThat(buildpack).isNull();
	}

	private void assertThatNoLayersAreAdded(Buildpack buildpack) throws IOException {
		List<Layer> layers = new ArrayList<>();
		buildpack.apply(layers::add);
		assertThat(layers).isEmpty();
	}

}
