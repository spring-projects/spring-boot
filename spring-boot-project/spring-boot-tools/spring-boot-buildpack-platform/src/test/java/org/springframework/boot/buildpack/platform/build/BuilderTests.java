/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ContainerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ImageApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPullListener;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Builder}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class BuilderTests {

	@Test
	void createWhenLogIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Builder(null)).withMessage("Log must not be null");
	}

	@Test
	void buildWhenRequestIsNullThrowsException() {
		Builder builder = new Builder();
		assertThatIllegalArgumentException().isThrownBy(() -> builder.build(null))
				.withMessage("Request must not be null");
	}

	@Test
	void buildInvokesBuilder() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any()))
				.willAnswer(withPulledImage(builderImage));
		given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any()))
				.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker);
		BuildRequest request = getTestRequest();
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		verify(docker.image()).load(archive.capture(), any());
		verify(docker.image()).remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildInvokesBuilderWithDefaultImageTags() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image-with-no-run-image-tag.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image().pull(eq(ImageReference.of("gcr.io/paketo-buildpacks/builder:latest")), any()))
				.willAnswer(withPulledImage(builderImage));
		given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:latest")), any()))
				.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker);
		BuildRequest request = getTestRequest().withBuilder(ImageReference.of("gcr.io/paketo-buildpacks/builder"));
		builder.build(request);
		assertThat(out.toString()).contains("Running creator");
		assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
		ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
		verify(docker.image()).load(archive.capture(), any());
		verify(docker.image()).remove(archive.getValue().getTag(), true);
	}

	@Test
	void buildWhenStackIdDoesNotMatchThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApi();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image-with-bad-stack.json");
		given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any()))
				.willAnswer(withPulledImage(builderImage));
		given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any()))
				.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker);
		BuildRequest request = getTestRequest();
		assertThatIllegalStateException().isThrownBy(() -> builder.build(request)).withMessage(
				"Run image stack 'org.cloudfoundry.stacks.cfwindowsfs3' does not match builder stack 'io.buildpacks.stacks.bionic'");
	}

	@Test
	void buildWhenBuilderReturnsErrorThrowsException() throws Exception {
		TestPrintStream out = new TestPrintStream();
		DockerApi docker = mockDockerApiLifecycleError();
		Image builderImage = loadImage("image.json");
		Image runImage = loadImage("run-image.json");
		given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any()))
				.willAnswer(withPulledImage(builderImage));
		given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any()))
				.willAnswer(withPulledImage(runImage));
		Builder builder = new Builder(BuildLog.to(out), docker);
		BuildRequest request = getTestRequest();
		assertThatExceptionOfType(BuilderException.class).isThrownBy(() -> builder.build(request))
				.withMessage("Builder lifecycle 'creator' failed with status code 9");
	}

	private DockerApi mockDockerApi() throws IOException {
		ContainerApi containerApi = mock(ContainerApi.class);
		ContainerReference reference = ContainerReference.of("container-ref");
		given(containerApi.create(any(), any())).willReturn(reference);
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
		given(containerApi.create(any(), any())).willReturn(reference);
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
		BuildRequest request = BuildRequest.of(name, (owner) -> content);
		return request;
	}

	private Image loadImage(String name) throws IOException {
		return Image.of(getClass().getResourceAsStream(name));
	}

	private Answer<Image> withPulledImage(Image image) {
		return (invocation) -> {
			TotalProgressPullListener listener = invocation.getArgument(1, TotalProgressPullListener.class);
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
