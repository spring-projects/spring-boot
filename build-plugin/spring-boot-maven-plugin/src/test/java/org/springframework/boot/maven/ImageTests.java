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

package org.springframework.boot.maven;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.BuildpackReference;
import org.springframework.boot.buildpack.platform.build.Cache;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.maven.CacheInfo.BindCacheInfo;
import org.springframework.boot.maven.CacheInfo.VolumeCacheInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Image}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 * @author Moritz Halbritter
 */
class ImageTests {

	@Test
	void getBuildRequestWhenNameIsNullDeducesName() {
		BuildRequest request = new Image().getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getName()).hasToString("docker.io/library/my-app:0.0.1-SNAPSHOT");
	}

	@Test
	void getBuildRequestWhenNameIsSetUsesName() {
		Image image = new Image();
		image.name = "demo";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getName()).hasToString("docker.io/library/demo:latest");
	}

	@Test
	void getBuildRequestWhenNoCustomizationsUsesDefaults() {
		BuildRequest request = new Image().getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getName()).hasToString("docker.io/library/my-app:0.0.1-SNAPSHOT");
		assertThat(request.getBuilder().toString()).contains("paketobuildpacks/builder-noble-java-tiny");
		assertThat(request.isTrustBuilder()).isTrue();
		assertThat(request.getRunImage()).isNull();
		assertThat(request.getEnv()).isEmpty();
		assertThat(request.isCleanCache()).isFalse();
		assertThat(request.isVerboseLogging()).isFalse();
		assertThat(request.getPullPolicy()).isEqualTo(PullPolicy.ALWAYS);
		assertThat(request.isPublish()).isFalse();
		assertThat(request.getBuildpacks()).isEmpty();
		assertThat(request.getBindings()).isEmpty();
		assertThat(request.getNetwork()).isNull();
		assertThat(request.getImagePlatform()).isNull();
	}

	@Test
	void getBuildRequestWhenHasBuilderUsesBuilder() {
		Image image = new Image();
		image.builder = "springboot/builder:2.2.x";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuilder()).hasToString("docker.io/springboot/builder:2.2.x");
		assertThat(request.isTrustBuilder()).isFalse();
	}

	@Test
	void getBuildRequestWhenHasBuilderAndTrustBuilderUsesBuilderAndTrustBuilder() {
		Image image = new Image();
		image.builder = "springboot/builder:2.2.x";
		image.trustBuilder = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuilder()).hasToString("docker.io/springboot/builder:2.2.x");
		assertThat(request.isTrustBuilder()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasDefaultBuilderAndTrustBuilderUsesTrustBuilder() {
		Image image = new Image();
		image.trustBuilder = false;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuilder().toString()).contains("paketobuildpacks/builder-noble-java-tiny");
		assertThat(request.isTrustBuilder()).isFalse();
	}

	@Test
	void getBuildRequestWhenHasRunImageUsesRunImage() {
		Image image = new Image();
		image.runImage = "springboot/run:latest";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getRunImage()).hasToString("docker.io/springboot/run:latest");
	}

	@Test
	void getBuildRequestWhenHasEnvUsesEnv() {
		Image image = new Image();
		image.env = Collections.singletonMap("test", "test");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getEnv()).containsExactly(entry("test", "test"));
	}

	@Test
	void getBuildRequestWhenHasCleanCacheUsesCleanCache() {
		Image image = new Image();
		image.cleanCache = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.isCleanCache()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasVerboseLoggingUsesVerboseLogging() {
		Image image = new Image();
		image.verboseLogging = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.isVerboseLogging()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasPullPolicyUsesPullPolicy() {
		Image image = new Image();
		image.setPullPolicy(PullPolicy.NEVER);
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getPullPolicy()).isEqualTo(PullPolicy.NEVER);
	}

	@Test
	void getBuildRequestWhenHasPublishUsesPublish() {
		Image image = new Image();
		image.publish = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.isPublish()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasBuildpacksUsesBuildpacks() {
		Image image = new Image();
		image.buildpacks = Arrays.asList("example/buildpack1@0.0.1", "example/buildpack2@0.0.2");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildpacks()).containsExactly(BuildpackReference.of("example/buildpack1@0.0.1"),
				BuildpackReference.of("example/buildpack2@0.0.2"));
	}

	@Test
	void getBuildRequestWhenHasBindingsUsesBindings() {
		Image image = new Image();
		image.bindings = Arrays.asList("host-src:container-dest:ro", "volume-name:container-dest:rw");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBindings()).containsExactly(Binding.of("host-src:container-dest:ro"),
				Binding.of("volume-name:container-dest:rw"));
	}

	@Test
	void getBuildRequestWhenNetworkUsesNetwork() {
		Image image = new Image();
		image.network = "test";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getNetwork()).isEqualTo("test");
	}

	@Test
	void getBuildRequestWhenHasTagsUsesTags() {
		Image image = new Image();
		image.tags = Arrays.asList("my-app:latest", "example.com/my-app:0.0.1-SNAPSHOT", "example.com/my-app:latest");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getTags()).containsExactly(ImageReference.of("my-app:latest"),
				ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
	}

	@Test
	void getBuildRequestWhenHasBuildWorkspaceVolumeUsesWorkspace() {
		Image image = new Image();
		image.buildWorkspace = CacheInfo.fromVolume(new VolumeCacheInfo("build-work-vol"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildWorkspace()).isEqualTo(Cache.volume("build-work-vol"));
	}

	@Test
	void getBuildRequestWhenHasBuildCacheVolumeUsesCache() {
		Image image = new Image();
		image.buildCache = CacheInfo.fromVolume(new VolumeCacheInfo("build-cache-vol"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildCache()).isEqualTo(Cache.volume("build-cache-vol"));
	}

	@Test
	void getBuildRequestWhenHasLaunchCacheVolumeUsesCache() {
		Image image = new Image();
		image.launchCache = CacheInfo.fromVolume(new VolumeCacheInfo("launch-cache-vol"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getLaunchCache()).isEqualTo(Cache.volume("launch-cache-vol"));
	}

	@Test
	void getBuildRequestWhenHasBuildWorkspaceBindUsesWorkspace() {
		Image image = new Image();
		image.buildWorkspace = CacheInfo.fromBind(new BindCacheInfo("build-work-dir"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildWorkspace()).isEqualTo(Cache.bind("build-work-dir"));
	}

	@Test
	void getBuildRequestWhenHasBuildCacheBindUsesCache() {
		Image image = new Image();
		image.buildCache = CacheInfo.fromBind(new BindCacheInfo("build-cache-dir"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildCache()).isEqualTo(Cache.bind("build-cache-dir"));
	}

	@Test
	void getBuildRequestWhenHasLaunchCacheBindUsesCache() {
		Image image = new Image();
		image.launchCache = CacheInfo.fromBind(new BindCacheInfo("launch-cache-dir"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getLaunchCache()).isEqualTo(Cache.bind("launch-cache-dir"));
	}

	@Test
	void getBuildRequestWhenHasCreatedDateUsesCreatedDate() {
		Image image = new Image();
		image.createdDate = "2020-07-01T12:34:56Z";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getCreatedDate()).isEqualTo("2020-07-01T12:34:56Z");
	}

	@Test
	void getBuildRequestWhenHasApplicationDirectoryUsesApplicationDirectory() {
		Image image = new Image();
		image.applicationDirectory = "/application";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getApplicationDirectory()).isEqualTo("/application");
	}

	@Test
	void getBuildRequestWhenHasNoSecurityOptionsUsesNoSecurityOptions() {
		Image image = new Image();
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getSecurityOptions()).isNull();
	}

	@Test
	void getBuildRequestWhenHasSecurityOptionsUsesSecurityOptions() {
		Image image = new Image();
		image.securityOptions = List.of("label=user:USER", "label=role:ROLE");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getSecurityOptions()).containsExactly("label=user:USER", "label=role:ROLE");
	}

	@Test
	void getBuildRequestWhenHasEmptySecurityOptionsUsesSecurityOptions() {
		Image image = new Image();
		image.securityOptions = Collections.emptyList();
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getSecurityOptions()).isEmpty();
	}

	@Test
	void getBuildRequestWhenHasImagePlatformUsesImagePlatform() {
		Image image = new Image();
		image.imagePlatform = "linux/arm64";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getImagePlatform()).isEqualTo(ImagePlatform.of("linux/arm64"));
	}

	@Test
	void getBuildRequestWhenImagePlatformIsEmptyDoesntSetImagePlatform() {
		Image image = new Image();
		image.imagePlatform = "";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getImagePlatform()).isNull();
	}

	private Artifact createArtifact() {
		return new DefaultArtifact("com.example", "my-app", VersionRange.createFromVersion("0.0.1-SNAPSHOT"), "compile",
				"jar", null, new DefaultArtifactHandler());
	}

	private Function<Owner, TarArchive> mockApplicationContent() {
		return (owner) -> null;
	}

}
