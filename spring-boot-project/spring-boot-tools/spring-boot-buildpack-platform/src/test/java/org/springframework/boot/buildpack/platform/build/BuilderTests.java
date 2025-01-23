/*
 * Copyright 2012-2025 the original author or authors.
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
import java.io.PrintStream;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ContainerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ImageApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPullListener;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link Builder}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Rafael Ceccone
 */
class BuilderTests {

	@Test
	void createWhenLogIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Builder((BuildLog) null))
			.withMessage("'log' must not be null");
	}

	@Test
	void createWithDockerConfiguration() {
		Builder builder = new Builder(BuildLog.toSystemOut());
		assertThat(builder).isNotNull();
	}

	@Test
	void buildWhenRequestIsNullThrowsException() {
		Builder builder = new Builder();
		assertThatIllegalArgumentException().isThrownBy(() -> builder.build(null))
			.withMessage("'request' must not be null");
	}

	@Test
	void buildInvokesBuilder() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest();
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull());
		then(docker.image()).should()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull());
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).shouldHaveNoMoreInteractions();
	}

	@Test
	void buildInvokesBuilderAndPublishesImage() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		DockerConfiguration dockerConfiguration = new DockerConfiguration()
			.withBuilderRegistryTokenAuthentication("builder token")
			.withPublishRegistryTokenAuthentication("publish token");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(),
					eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
		BuildRequest request = getTestRequest().withPublish(true);
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(),
					eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
		then(docker.image()).should()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
		then(docker.image()).should()
			.push(eq(request.getName()), any(),
					eq(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()));
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).shouldHaveNoMoreInteractions();
	}

	@Test
	void buildInvokesBuilderWithDefaultImageTags() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image-with-no-run-image-tag.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of("gcr.io/paketo-buildpacks/builder:latest")), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:latest")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withBuilder(ImageReference.of("gcr.io/paketo-buildpacks/builder"));
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildInvokesBuilderWithRunImageInDigestForm() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image-with-run-image-digest.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference
				.of("docker.io/cloudfoundry/run@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d")),
					eq(ImagePlatform.from(builderImage)), any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest();
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildInvokesBuilderWithNoStack() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image-with-empty-stack.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of("gcr.io/paketo-buildpacks/builder:latest")), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withBuilder(ImageReference.of("gcr.io/paketo-buildpacks/builder"));
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildInvokesBuilderWithRunImageFromRequest() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("example.com/custom/run:latest")), eq(ImagePlatform.from(builderImage)), any(),
					isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withRunImage(ImageReference.of("example.com/custom/run:latest"));
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildInvokesBuilderWithNeverPullPolicy() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		given(docker.image().inspect(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF))))
			.willReturn(builderImage);
		given(docker.image().inspect(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb"))))
			.willReturn(runImage);
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withPullPolicy(PullPolicy.NEVER);
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).should(never()).pull(any(), any(), any());
		then(docker.image()).should(times(2)).inspect(any());
	}

	@Test
	void buildInvokesBuilderWithAlwaysPullPolicy() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		given(docker.image().inspect(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF))))
			.willReturn(builderImage);
		given(docker.image().inspect(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb"))))
			.willReturn(runImage);
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withPullPolicy(PullPolicy.ALWAYS);
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).should(times(2)).pull(any(), any(), any(), isNull());
		then(docker.image()).should(never()).inspect(any());
	}

	@Test
	void buildInvokesBuilderWithIfNotPresentPullPolicy() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		given(docker.image().inspect(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF))))
			.willThrow(
					new DockerEngineException("docker://localhost/", new URI("example"), 404, "NOT FOUND", null, null))
			.willReturn(builderImage);
		given(docker.image().inspect(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb"))))
			.willThrow(
					new DockerEngineException("docker://localhost/", new URI("example"), 404, "NOT FOUND", null, null))
			.willReturn(runImage);
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withPullPolicy(PullPolicy.IF_NOT_PRESENT);
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).should(times(2)).inspect(any());
		then(docker.image()).should(times(2)).pull(any(), any(), any(), isNull());
	}

	@Test
	void buildInvokesBuilderWithTags() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withTags(ImageReference.of("my-application:1.2.3"));
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		assertThat(out.toString()).contains("Successfully created image tag 'docker.io/library/my-application:1.2.3'");
		then(docker.image()).should().tag(eq(request.getName()), eq(ImageReference.of("my-application:1.2.3")));
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildInvokesBuilderWithTagsAndPublishesImageAndTags() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		DockerConfiguration dockerConfiguration = new DockerConfiguration()
			.withBuilderRegistryTokenAuthentication("builder token")
			.withPublishRegistryTokenAuthentication("publish token");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(),
					eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
		BuildRequest request = getTestRequest().withPublish(true).withTags(ImageReference.of("my-application:1.2.3"));
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		assertThat(out.toString()).contains("Successfully created image tag 'docker.io/library/my-application:1.2.3'");

		then(docker.image()).should()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(),
					eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
		then(docker.image()).should()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
		then(docker.image()).should()
			.push(eq(request.getName()), any(),
					eq(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()));
		then(docker.image()).should().tag(eq(request.getName()), eq(ImageReference.of("my-application:1.2.3")));
		then(docker.image()).should()
			.push(eq(ImageReference.of("my-application:1.2.3")), any(),
					eq(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()));
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).shouldHaveNoMoreInteractions();
	}

	@Test
	void buildInvokesBuilderWithPlatform() throws Exception {
		TestPrintStream out = new TestPrintStream();
		ImagePlatform platform = ImagePlatform.of("linux/arm64/v1");
		DockerApi docker = mockDockerApi(platform);
		Image builderImage = loadImage("image-with-platform.json");
		Image runImage = loadImage("run-image-with-platform.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), eq(platform), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(platform), any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withImagePlatform("linux/arm64/v1");
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		then(docker.image()).should()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), eq(platform), any(), isNull());
		then(docker.image()).should()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(platform), any(), isNull());
		then(docker.image()).should().load(archive.capture(), any());
		then(docker.image()).should().remove(archive.getValue().getTag(), true);
		then(docker.image()).shouldHaveNoMoreInteractions();
	}

	@Test
	void buildWhenStackIdDoesNotMatchThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image-with-bad-stack.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest();
		assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
			.withMessage(
					"Run image stack 'org.cloudfoundry.stacks.cfwindowsfs3' does not match builder stack 'io.buildpacks.stacks.bionic'");
	}

	@Test
	void buildWhenBuilderReturnsErrorThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApiLifecycleError();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest();
		assertThatExceptionOfType(BuilderException.class).isThrownBy(() -> builder.build(request))
			.withMessage("Builder lifecycle 'creator' failed with status code 9");
	}

	@Test
	void buildWhenDetectedRunImageInDifferentAuthenticatedRegistryThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image-with-run-image-different-registry.json");
		DockerConfiguration dockerConfiguration = new DockerConfiguration()
			.withBuilderRegistryTokenAuthentication("builder token");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), any(), any(),
					eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
			.willAnswer(withPulledImage(builderImage));
		Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
		BuildRequest request = getTestRequest();
		assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
			.withMessage(
					"Run image 'example.com/custom/run:latest' must be pulled from the 'docker.io' authenticated registry");
	}

	@Test
	void buildWhenRequestedRunImageInDifferentAuthenticatedRegistryThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		DockerConfiguration dockerConfiguration = new DockerConfiguration()
			.withBuilderRegistryTokenAuthentication("builder token");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), any(), any(),
					eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
			.willAnswer(withPulledImage(builderImage));
		Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
		BuildRequest request = getTestRequest().withRunImage(ImageReference.of("example.com/custom/run:latest"));
		assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
			.withMessage(
					"Run image 'example.com/custom/run:latest' must be pulled from the 'docker.io' authenticated registry");
	}

	@Test
	void buildWhenRequestedBuildpackNotInBuilderThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApiLifecycleError();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), any(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:example/buildpack@1.2.3");
		BuildRequest request = getTestRequest().withBuildpacks(reference);
		assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
			.withMessageContaining("'urn:cnb:builder:example/buildpack@1.2.3'")
			.withMessageContaining("not found in builder");
	}

	@Test
	void logsWarningIfBindingWithSensitiveTargetIsDetected() throws IOException {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image()
			.pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_REF)), isNull(), any(), isNull()))
			.willAnswer(withPulledImage(builderImage));
		given(docker.image()
			.pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), eq(ImagePlatform.from(builderImage)),
					any(), isNull()))
			.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker, null);
		BuildRequest request = getTestRequest().withBindings(Binding.from("/host", "/cnb"));
		builder.build(request);
		assertThat(out.toString()).contains(
				"Warning: Binding '/host:/cnb' uses a container path which is used by buildpacks while building. Binding to it can cause problems!");
	}

	private DockerApi mockDockerApi() throws IOException {
		return mockDockerApi(null);
	}

	private DockerApi mockDockerApi(ImagePlatform platform) throws IOException {
		ContainerApi containerApi = mock(ContainerApi.class);
		ContainerReference reference = ContainerReference.of("container-ref");
		given(containerApi.create(any(), eq(platform), any())).willReturn(reference);
		given(containerApi.wait(eq(reference))).willReturn(ContainerStatus.of(0, null));
		ImageApi imageApi = mock(ImageApi.class);
		VolumeApi volumeApi = mock(VolumeApi.class);
		DockerApi docker = mock(DockerApi.class);
		given(docker.image()).willReturn(imageApi);
		given(docker.container()).willReturn(containerApi);
		given(docker.volume()).willReturn(volumeApi);
		return docker;
	}

	private DockerApi mockDockerApiLifecycleError() throws IOException {
		ContainerApi containerApi = mock(ContainerApi.class);
		ContainerReference reference = ContainerReference.of("container-ref");
		given(containerApi.create(any(), isNull(), any())).willReturn(reference);
		given(containerApi.wait(eq(reference))).willReturn(ContainerStatus.of(9, null));
		ImageApi imageApi = mock(ImageApi.class);
		VolumeApi volumeApi = mock(VolumeApi.class);
		DockerApi docker = mock(DockerApi.class);
		given(docker.image()).willReturn(imageApi);
		given(docker.container()).willReturn(containerApi);
		given(docker.volume()).willReturn(volumeApi);
		return docker;
	}

	private BuildRequest getTestRequest() {
		TarArchive content = mock(TarArchive.class);
		ImageReference name = ImageReference.of("my-application");
		return BuildRequest.of(name, (owner) -> content).withTrustBuilder(true);
	}

	private Image loadImage(String name) throws IOException {
		return Image.of(getClass().getResourceAsStream(name));
	}

	private Answer<Image> withPulledImage(Image image) {
		return (invocation) -> {
			TotalProgressPullListener listener = invocation.getArgument(2, TotalProgressPullListener.class);
			listener.onStart();
			listener.onFinish();
			return image;
		};

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
