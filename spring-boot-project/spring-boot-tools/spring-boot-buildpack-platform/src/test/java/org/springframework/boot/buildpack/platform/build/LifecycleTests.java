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

package org.springframework.boot.buildpack.platform.build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jna.Platform;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ContainerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ImageApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ContainerContent;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.boot.testsupport.junit.BooleanValueSource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Lifecycle}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 */
class LifecycleTests {

	private TestPrintStream out;

	private DockerApi docker;

	private final Map<String, ContainerConfig> configs = new LinkedHashMap<>();

	private final Map<String, ContainerContent> content = new LinkedHashMap<>();

	@BeforeEach
	void setup() {
		this.out = new TestPrintStream();
		this.docker = mockDockerApi();
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		createLifecycle(trustBuilder).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@Test
	void executeWithBindingsExecutesPhases() throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(true).withBindings(Binding.of("/host/src/path:/container/dest/path:ro"),
				Binding.of("volume-name:/container/volume/path:rw"));
		createLifecycle(request).execute();
		assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-bindings.json"));
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@Test
	void executeExecutesPhasesWithPlatformApi03() throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		createLifecycle(true, "builder-metadata-platform-api-0.3.json").execute();
		assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-platform-api-0.3.json"));
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeOnlyUploadsContentOnce(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		createLifecycle(trustBuilder).execute();
		assertThat(this.content).hasSize(1);
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWhenAlreadyRunThrowsException(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		Lifecycle lifecycle = createLifecycle(trustBuilder);
		lifecycle.execute();
		assertThatIllegalStateException().isThrownBy(lifecycle::execute)
			.withMessage("Lifecycle has already been executed");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWhenBuilderReturnsErrorThrowsException(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(9, null));
		assertThatExceptionOfType(BuilderException.class).isThrownBy(() -> createLifecycle(trustBuilder).execute())
			.withMessage(
					"Builder lifecycle '" + ((trustBuilder) ? "creator" : "analyzer") + "' failed with status code 9");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWhenCleanCacheClearsCache(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder).withCleanCache(true);
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-clean-cache.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter.json"));
			assertThat(this.out.toString()).contains("Skipping restorer because 'cleanCache' is enabled");
		}
		VolumeName name = VolumeName.of("pack-cache-b35197ac41ea.build");
		then(this.docker.volume()).should().delete(name, true);
	}

	@Test
	void executeWhenPlatformApiNotSupportedThrowsException() throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		assertThatIllegalStateException()
			.isThrownBy(() -> createLifecycle(true, "builder-metadata-unsupported-api.json").execute())
			.withMessageContaining("Detected platform API versions '0.2' are not included in supported versions");
	}

	@Test
	void executeWhenMultiplePlatformApisNotSupportedThrowsException() throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		assertThatIllegalStateException()
			.isThrownBy(() -> createLifecycle(true, "builder-metadata-unsupported-apis.json").execute())
			.withMessageContaining("Detected platform API versions '0.1,0.2' are not included in supported versions");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWhenMultiplePlatformApisSupportedExecutesPhase(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		createLifecycle(trustBuilder, "builder-metadata-supported-apis.json").execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter.json"));
		}
	}

	@Test
	void closeClearsVolumes() throws Exception {
		createLifecycle(true).close();
		then(this.docker.volume()).should().delete(VolumeName.of("pack-layers-aaaaaaaaaa"), true);
		then(this.docker.volume()).should().delete(VolumeName.of("pack-app-aaaaaaaaaa"), true);
	}

	@Test
	void executeWithNetworkExecutesPhases() throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(true).withNetwork("test");
		createLifecycle(request).execute();
		assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-network.json"));
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithCacheVolumeNamesExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder).withBuildWorkspace(Cache.volume("work-volume"))
			.withBuildCache(Cache.volume("build-volume"))
			.withLaunchCache(Cache.volume("launch-volume"));
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-cache-volumes.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer-cache-volumes.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector-cache-volumes.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer-cache-volumes.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder-cache-volumes.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-cache-volumes.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithCacheBindMountsExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder).withBuildWorkspace(Cache.bind("/tmp/work"))
			.withBuildCache(Cache.bind("/tmp/build-cache"))
			.withLaunchCache(Cache.bind("/tmp/launch-cache"));
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-cache-bind-mounts.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer-cache-bind-mounts.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector-cache-bind-mounts.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer-cache-bind-mounts.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder-cache-bind-mounts.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-cache-bind-mounts.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithCreatedDateExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder).withCreatedDate("2020-07-01T12:34:56Z");
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-created-date.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-created-date.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithApplicationDirectoryExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder).withApplicationDirectory("/application");
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-app-dir.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector-app-dir.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder-app-dir.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-app-dir.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithSecurityOptionsExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder)
			.withSecurityOptions(List.of("label=user:USER", "label=role:ROLE"));
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-security-opts.json", true));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer-security-opts.json", true));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer-security-opts.json", true));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-security-opts.json", true));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithDockerHostAndRemoteAddressExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder);
		createLifecycle(request, ResolvedDockerHost.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376")))
			.execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-inherit-remote.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer-inherit-remote.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer-inherit-remote.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-inherit-remote.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithDockerHostAndLocalAddressExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), isNull())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), isNull(), any())).willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder);
		createLifecycle(request, ResolvedDockerHost.from(DockerHostConfiguration.forAddress("/var/alt.sock")))
			.execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-inherit-local.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer-inherit-local.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer-inherit-local.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter-inherit-local.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	@ParameterizedTest
	@BooleanValueSource
	void executeWithImagePlatformExecutesPhases(boolean trustBuilder) throws Exception {
		given(this.docker.container().create(any(), eq(ImagePlatform.of("linux/arm64"))))
			.willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().create(any(), eq(ImagePlatform.of("linux/arm64")), any()))
			.willAnswer(answerWithGeneratedContainerId());
		given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
		BuildRequest request = getTestRequest(trustBuilder).withImagePlatform("linux/arm64");
		createLifecycle(request).execute();
		if (trustBuilder) {
			assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator.json"));
		}
		else {
			assertPhaseWasRun("analyzer", withExpectedConfig("lifecycle-analyzer.json"));
			assertPhaseWasRun("detector", withExpectedConfig("lifecycle-detector.json"));
			assertPhaseWasRun("restorer", withExpectedConfig("lifecycle-restorer.json"));
			assertPhaseWasRun("builder", withExpectedConfig("lifecycle-builder.json"));
			assertPhaseWasRun("exporter", withExpectedConfig("lifecycle-exporter.json"));
		}
		assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
	}

	private DockerApi mockDockerApi() {
		DockerApi docker = mock(DockerApi.class);
		ImageApi imageApi = mock(ImageApi.class);
		ContainerApi containerApi = mock(ContainerApi.class);
		VolumeApi volumeApi = mock(VolumeApi.class);
		given(docker.image()).willReturn(imageApi);
		given(docker.container()).willReturn(containerApi);
		given(docker.volume()).willReturn(volumeApi);
		return docker;
	}

	private BuildRequest getTestRequest(boolean trustBuilder) {
		TarArchive content = mock(TarArchive.class);
		ImageReference name = ImageReference.of("my-application");
		return BuildRequest.of(name, (owner) -> content)
			.withRunImage(ImageReference.of("cloudfoundry/run"))
			.withTrustBuilder(trustBuilder);
	}

	private Lifecycle createLifecycle(boolean trustBuilder) throws IOException {
		return createLifecycle(getTestRequest(trustBuilder));
	}

	private Lifecycle createLifecycle(BuildRequest request) throws IOException {
		EphemeralBuilder builder = mockEphemeralBuilder();
		return createLifecycle(request, builder);
	}

	private Lifecycle createLifecycle(boolean trustBuilder, String builderMetadata) throws IOException {
		EphemeralBuilder builder = mockEphemeralBuilder(builderMetadata);
		return createLifecycle(getTestRequest(trustBuilder), builder);
	}

	private Lifecycle createLifecycle(BuildRequest request, ResolvedDockerHost dockerHost) throws IOException {
		EphemeralBuilder builder = mockEphemeralBuilder();
		return new TestLifecycle(BuildLog.to(this.out), this.docker, dockerHost, request, builder);
	}

	private Lifecycle createLifecycle(BuildRequest request, EphemeralBuilder ephemeralBuilder) {
		return new TestLifecycle(BuildLog.to(this.out), this.docker, null, request, ephemeralBuilder);
	}

	private EphemeralBuilder mockEphemeralBuilder() throws IOException {
		return mockEphemeralBuilder("builder-metadata.json");
	}

	private EphemeralBuilder mockEphemeralBuilder(String builderMetadata) throws IOException {
		EphemeralBuilder builder = mock(EphemeralBuilder.class);
		byte[] metadataContent = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream(builderMetadata));
		BuilderMetadata metadata = BuilderMetadata.fromJson(new String(metadataContent, StandardCharsets.UTF_8));
		given(builder.getName()).willReturn(ImageReference.of("pack.local/ephemeral-builder"));
		given(builder.getBuilderMetadata()).willReturn(metadata);
		return builder;
	}

	private Answer<ContainerReference> answerWithGeneratedContainerId() {
		return (invocation) -> {
			ContainerConfig config = invocation.getArgument(0, ContainerConfig.class);
			ArrayNode command = getCommand(config);
			String name = command.get(0).asText().substring(1).replaceAll("/", "-");
			this.configs.put(name, config);
			if (invocation.getArguments().length > 2) {
				this.content.put(name, invocation.getArgument(2, ContainerContent.class));
			}
			return ContainerReference.of(name);
		};
	}

	private ArrayNode getCommand(ContainerConfig config) throws JsonProcessingException {
		JsonNode node = SharedObjectMapper.get().readTree(config.toString());
		return (ArrayNode) node.at("/Cmd");
	}

	private void assertPhaseWasRun(String name, IOConsumer<ContainerConfig> configConsumer) throws IOException {
		ContainerReference containerReference = ContainerReference.of("cnb-lifecycle-" + name);
		then(this.docker.container()).should().start(containerReference);
		then(this.docker.container()).should().logs(eq(containerReference), any());
		then(this.docker.container()).should().remove(containerReference, true);
		configConsumer.accept(this.configs.get(containerReference.toString()));
	}

	private IOConsumer<ContainerConfig> withExpectedConfig(String name) {
		return withExpectedConfig(name, false);
	}

	private IOConsumer<ContainerConfig> withExpectedConfig(String name, boolean expectSecurityOptAlways) {
		return (config) -> {
			try {
				InputStream in = getClass().getResourceAsStream(name);
				String jsonString = FileCopyUtils.copyToString(new InputStreamReader(in, StandardCharsets.UTF_8));
				JSONObject json = new JSONObject(jsonString);
				if (!expectSecurityOptAlways && Platform.isWindows()) {
					JSONObject hostConfig = json.getJSONObject("HostConfig");
					hostConfig.remove("SecurityOpt");
				}
				JSONAssert.assertEquals(config.toString(), json, true);
			}
			catch (JSONException ex) {
				throw new IOException(ex);
			}
		};
	}

	static class TestLifecycle extends Lifecycle {

		TestLifecycle(BuildLog log, DockerApi docker, ResolvedDockerHost dockerHost, BuildRequest request,
				EphemeralBuilder builder) {
			super(log, docker, dockerHost, request, builder);
		}

		@Override
		protected VolumeName createRandomVolumeName(String prefix) {
			return VolumeName.of(prefix + "aaaaaaaaaa");
		}

	}

	static class TestPrintStream extends PrintStream {

		TestPrintStream() {
			super(new ByteArrayOutputStream());
		}

		@Override
		public String toString() {
			return this.out.toString();
		}

	}

}
