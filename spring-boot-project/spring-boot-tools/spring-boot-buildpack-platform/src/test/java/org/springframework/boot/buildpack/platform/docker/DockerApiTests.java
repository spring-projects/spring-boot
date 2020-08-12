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

package org.springframework.boot.buildpack.platform.docker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.buildpack.platform.docker.DockerApi.ContainerApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.ImageApi;
import org.springframework.boot.buildpack.platform.docker.DockerApi.VolumeApi;
import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport;
import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport.Response;
import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ContainerContent;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DockerApi}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@ExtendWith(MockitoExtension.class)
class DockerApiTests {

	private static final String API_URL = "/" + DockerApi.API_VERSION;

	private static final String IMAGES_URL = API_URL + "/images";

	private static final String CONTAINERS_URL = API_URL + "/containers";

	private static final String VOLUMES_URL = API_URL + "/volumes";

	@Mock
	private HttpTransport http;

	private DockerApi dockerApi;

	@BeforeEach
	void setup() {
		this.dockerApi = new DockerApi(this.http);
	}

	private HttpTransport http() {
		return this.http;
	}

	private Response emptyResponse() {
		return responseOf(null);
	}

	private Response responseOf(String name) {
		return new Response() {

			@Override
			public void close() {
			}

			@Override
			public InputStream getContent() {
				if (name == null) {
					return null;
				}
				return getClass().getResourceAsStream(name);
			}

		};
	}

	@Nested
	class ImageDockerApiTests {

		private ImageApi api;

		@Mock
		private UpdateListener<PullImageUpdateEvent> pullListener;

		@Mock
		private UpdateListener<LoadImageUpdateEvent> loadListener;

		@Captor
		private ArgumentCaptor<IOConsumer<OutputStream>> writer;

		@BeforeEach
		void setup() {
			this.api = DockerApiTests.this.dockerApi.image();
		}

		@Test
		void pullWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.pull(null, this.pullListener))
					.withMessage("Reference must not be null");
		}

		@Test
		void pullWhenListenerIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.pull(ImageReference.of("ubuntu"), null))
					.withMessage("Listener must not be null");
		}

		@Test
		void pullPullsImageAndProducesEvents() throws Exception {
			ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
			URI createUri = new URI(IMAGES_URL + "/create?fromImage=gcr.io%2Fpaketo-buildpacks%2Fbuilder%3Abase");
			String imageHash = "4acb6bfd6c4f0cabaf7f3690e444afe51f1c7de54d51da7e63fac709c56f1c30";
			URI imageUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder@sha256:" + imageHash + "/json");
			given(http().post(createUri)).willReturn(responseOf("pull-stream.json"));
			given(http().get(imageUri)).willReturn(responseOf("type/image.json"));
			Image image = this.api.pull(reference, this.pullListener);
			assertThat(image.getLayers()).hasSize(46);
			InOrder ordered = inOrder(this.pullListener);
			ordered.verify(this.pullListener).onStart();
			ordered.verify(this.pullListener, times(595)).onUpdate(any());
			ordered.verify(this.pullListener).onFinish();
		}

		@Test
		void loadWhenArchiveIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.load(null, UpdateListener.none()))
					.withMessage("Archive must not be null");
		}

		@Test
		void loadWhenListenerIsNullThrowsException() {
			ImageArchive archive = mock(ImageArchive.class);
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.load(archive, null))
					.withMessage("Listener must not be null");
		}

		@Test
		void loadLoadsImage() throws Exception {
			Image image = Image.of(getClass().getResourceAsStream("type/image.json"));
			ImageArchive archive = ImageArchive.from(image);
			URI loadUri = new URI(IMAGES_URL + "/load");
			given(http().post(eq(loadUri), eq("application/x-tar"), any())).willReturn(responseOf("load-stream.json"));
			this.api.load(archive, this.loadListener);
			InOrder ordered = inOrder(this.loadListener);
			ordered.verify(this.loadListener).onStart();
			ordered.verify(this.loadListener).onUpdate(any());
			ordered.verify(this.loadListener).onFinish();
			verify(http()).post(any(), any(), this.writer.capture());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			this.writer.getValue().accept(out);
			assertThat(out.toByteArray()).hasSizeGreaterThan(21000);
		}

		@Test
		void removeWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.remove(null, true))
					.withMessage("Reference must not be null");
		}

		@Test
		void removeRemovesContainer() throws Exception {
			ImageReference reference = ImageReference
					.of("ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
			URI removeUri = new URI(IMAGES_URL
					+ "/docker.io/library/ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
			given(http().delete(removeUri)).willReturn(emptyResponse());
			this.api.remove(reference, false);
			verify(http()).delete(removeUri);
		}

		@Test
		void removeWhenForceIsTrueRemovesContainer() throws Exception {
			ImageReference reference = ImageReference
					.of("ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
			URI removeUri = new URI(IMAGES_URL
					+ "/docker.io/library/ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d?force=1");
			given(http().delete(removeUri)).willReturn(emptyResponse());
			this.api.remove(reference, true);
			verify(http()).delete(removeUri);
		}

		@Test
		void inspectWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.inspect(null))
					.withMessage("Reference must not be null");
		}

		@Test
		void inspectInspectImage() throws Exception {
			ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
			URI imageUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/json");
			given(http().get(imageUri)).willReturn(responseOf("type/image.json"));
			Image image = this.api.inspect(reference);
			assertThat(image.getLayers()).hasSize(46);
		}

	}

	@Nested
	class ContainerDockerApiTests {

		private ContainerApi api;

		@Captor
		private ArgumentCaptor<IOConsumer<OutputStream>> writer;

		@Mock
		private UpdateListener<LogUpdateEvent> logListener;

		@BeforeEach
		void setup() {
			this.api = DockerApiTests.this.dockerApi.container();
		}

		@Test
		void createWhenConfigIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.create(null))
					.withMessage("Config must not be null");
		}

		@Test
		void createCreatesContainer() throws Exception {
			ImageReference imageReference = ImageReference.of("ubuntu:bionic");
			ContainerConfig config = ContainerConfig.of(imageReference, (update) -> update.withCommand("/bin/bash"));
			URI createUri = new URI(CONTAINERS_URL + "/create");
			given(http().post(eq(createUri), eq("application/json"), any()))
					.willReturn(responseOf("create-container-response.json"));
			ContainerReference containerReference = this.api.create(config);
			assertThat(containerReference.toString()).isEqualTo("e90e34656806");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			verify(http()).post(any(), any(), this.writer.capture());
			this.writer.getValue().accept(out);
			assertThat(out.toByteArray()).hasSizeGreaterThan(130);
		}

		@Test
		void createWhenHasContentContainerWithContent() throws Exception {
			ImageReference imageReference = ImageReference.of("ubuntu:bionic");
			ContainerConfig config = ContainerConfig.of(imageReference, (update) -> update.withCommand("/bin/bash"));
			TarArchive archive = TarArchive.of((layout) -> {
				layout.directory("/test", Owner.ROOT);
				layout.file("/test/file", Owner.ROOT, Content.of("test"));
			});
			ContainerContent content = ContainerContent.of(archive);
			URI createUri = new URI(CONTAINERS_URL + "/create");
			given(http().post(eq(createUri), eq("application/json"), any()))
					.willReturn(responseOf("create-container-response.json"));
			URI uploadUri = new URI(CONTAINERS_URL + "/e90e34656806/archive?path=%2F");
			given(http().put(eq(uploadUri), eq("application/x-tar"), any())).willReturn(emptyResponse());
			ContainerReference containerReference = this.api.create(config, content);
			assertThat(containerReference.toString()).isEqualTo("e90e34656806");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			verify(http()).post(any(), any(), this.writer.capture());
			this.writer.getValue().accept(out);
			assertThat(out.toByteArray()).hasSizeGreaterThan(130);
			verify(http()).put(any(), any(), this.writer.capture());
			this.writer.getValue().accept(out);
			assertThat(out.toByteArray()).hasSizeGreaterThan(2000);
		}

		@Test
		void startWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.start(null))
					.withMessage("Reference must not be null");
		}

		@Test
		void startStartsContainer() throws Exception {
			ContainerReference reference = ContainerReference.of("e90e34656806");
			URI startContainerUri = new URI(CONTAINERS_URL + "/e90e34656806/start");
			given(http().post(startContainerUri)).willReturn(emptyResponse());
			this.api.start(reference);
			verify(http()).post(startContainerUri);
		}

		@Test
		void logsWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.logs(null, UpdateListener.none()))
					.withMessage("Reference must not be null");
		}

		@Test
		void logsWhenListenerIsNullThrowsException() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> this.api.logs(ContainerReference.of("e90e34656806"), null))
					.withMessage("Listener must not be null");
		}

		@Test
		void logsProducesEvents() throws Exception {
			ContainerReference reference = ContainerReference.of("e90e34656806");
			URI logsUri = new URI(CONTAINERS_URL + "/e90e34656806/logs?stdout=1&stderr=1&follow=1");
			given(http().get(logsUri)).willReturn(responseOf("log-update-event.stream"));
			this.api.logs(reference, this.logListener);
			InOrder ordered = inOrder(this.logListener);
			ordered.verify(this.logListener).onStart();
			ordered.verify(this.logListener, times(7)).onUpdate(any());
			ordered.verify(this.logListener).onFinish();
		}

		@Test
		void waitWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.wait(null))
					.withMessage("Reference must not be null");
		}

		@Test
		void waitReturnsStatus() throws Exception {
			ContainerReference reference = ContainerReference.of("e90e34656806");
			URI waitUri = new URI(CONTAINERS_URL + "/e90e34656806/wait");
			given(http().post(waitUri)).willReturn(responseOf("container-wait-response.json"));
			ContainerStatus status = this.api.wait(reference);
			assertThat(status.getStatusCode()).isEqualTo(1);
		}

		@Test
		void removeWhenReferenceIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.remove(null, true))
					.withMessage("Reference must not be null");
		}

		@Test
		void removeRemovesContainer() throws Exception {
			ContainerReference reference = ContainerReference.of("e90e34656806");
			URI removeUri = new URI(CONTAINERS_URL + "/e90e34656806");
			given(http().delete(removeUri)).willReturn(emptyResponse());
			this.api.remove(reference, false);
			verify(http()).delete(removeUri);
		}

		@Test
		void removeWhenForceIsTrueRemovesContainer() throws Exception {
			ContainerReference reference = ContainerReference.of("e90e34656806");
			URI removeUri = new URI(CONTAINERS_URL + "/e90e34656806?force=1");
			given(http().delete(removeUri)).willReturn(emptyResponse());
			this.api.remove(reference, true);
			verify(http()).delete(removeUri);
		}

	}

	@Nested
	class VolumeDockerApiTests {

		private VolumeApi api;

		@BeforeEach
		void setup() {
			this.api = DockerApiTests.this.dockerApi.volume();
		}

		@Test
		void deleteWhenNameIsNullThrowsException() {
			assertThatIllegalArgumentException().isThrownBy(() -> this.api.delete(null, false))
					.withMessage("Name must not be null");
		}

		@Test
		void deleteDeletesContainer() throws Exception {
			VolumeName name = VolumeName.of("test");
			URI removeUri = new URI(VOLUMES_URL + "/test");
			given(http().delete(removeUri)).willReturn(emptyResponse());
			this.api.delete(name, false);
			verify(http()).delete(removeUri);
		}

		@Test
		void deleteWhenForceIsTrueDeletesContainer() throws Exception {
			VolumeName name = VolumeName.of("test");
			URI removeUri = new URI(VOLUMES_URL + "/test?force=1");
			given(http().delete(removeUri)).willReturn(emptyResponse());
			this.api.delete(name, true);
			verify(http()).delete(removeUri);
		}

	}

}
