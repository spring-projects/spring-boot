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

package org.springframework.boot.buildpack.platform.docker;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.net.URIBuilder;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport;
import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport.Response;
import org.springframework.boot.buildpack.platform.docker.type.ApiVersion;
import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ContainerContent;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImagePlatform;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.json.JsonStream;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides access to the limited set of Docker APIs needed by pack.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Rafael Ceccone
 * @author Moritz Halbritter
 * @since 2.3.0
 */
public class DockerApi {

	private static final List<String> FORCE_PARAMS = Collections.unmodifiableList(Arrays.asList("force", "1"));

	static final ApiVersion API_VERSION = ApiVersion.of(1, 24);

	static final ApiVersion PLATFORM_API_VERSION = ApiVersion.of(1, 41);

	static final ApiVersion UNKNOWN_API_VERSION = ApiVersion.of(0, 0);

	static final String API_VERSION_HEADER_NAME = "API-Version";

	private final HttpTransport http;

	private final JsonStream jsonStream;

	private final ImageApi image;

	private final ContainerApi container;

	private final VolumeApi volume;

	private final SystemApi system;

	private volatile ApiVersion apiVersion = null;

	/**
	 * Create a new {@link DockerApi} instance.
	 */
	public DockerApi() {
		this(HttpTransport.create(null));
	}

	/**
	 * Create a new {@link DockerApi} instance.
	 * @param dockerHost the Docker daemon host information
	 * @since 2.4.0
	 */
	public DockerApi(DockerHostConfiguration dockerHost) {
		this(HttpTransport.create(dockerHost));
	}

	/**
	 * Create a new {@link DockerApi} instance backed by a specific {@link HttpTransport}
	 * implementation.
	 * @param http the http implementation
	 */
	DockerApi(HttpTransport http) {
		this.http = http;
		this.jsonStream = new JsonStream(SharedObjectMapper.get());
		this.image = new ImageApi();
		this.container = new ContainerApi();
		this.volume = new VolumeApi();
		this.system = new SystemApi();
	}

	private HttpTransport http() {
		return this.http;
	}

	private JsonStream jsonStream() {
		return this.jsonStream;
	}

	private URI buildUrl(String path, Collection<?> params) {
		return buildUrl(API_VERSION, path, (params != null) ? params.toArray() : null);
	}

	private URI buildUrl(String path, Object... params) {
		return buildUrl(API_VERSION, path, params);
	}

	private URI buildUrl(ApiVersion apiVersion, String path, Object... params) {
		verifyApiVersion(apiVersion);
		try {
			URIBuilder builder = new URIBuilder("/v" + apiVersion + path);
			int param = 0;
			while (param < params.length) {
				builder.addParameter(Objects.toString(params[param++]), Objects.toString(params[param++]));
			}
			return builder.build();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void verifyApiVersion(ApiVersion minimumVersion) {
		ApiVersion actualVersion = getApiVersion();
		Assert.state(actualVersion.equals(UNKNOWN_API_VERSION) || actualVersion.supports(minimumVersion),
				() -> "Docker API version must be at least " + minimumVersion
						+ " to support this feature, but current API version is " + actualVersion);
	}

	private ApiVersion getApiVersion() {
		ApiVersion apiVersion = this.apiVersion;
		if (this.apiVersion == null) {
			apiVersion = this.system.getApiVersion();
			this.apiVersion = apiVersion;
		}
		return apiVersion;
	}

	/**
	 * Return the Docker API for image operations.
	 * @return the image API
	 */
	public ImageApi image() {
		return this.image;
	}

	/**
	 * Return the Docker API for container operations.
	 * @return the container API
	 */
	public ContainerApi container() {
		return this.container;
	}

	public VolumeApi volume() {
		return this.volume;
	}

	SystemApi system() {
		return this.system;
	}

	/**
	 * Docker API for image operations.
	 */
	public class ImageApi {

		ImageApi() {
		}

		/**
		 * Pull an image from a registry.
		 * @param reference the image reference to pull
		 * @param platform the platform (os/architecture/variant) of the image to pull
		 * @param listener a pull listener to receive update events
		 * @return the {@link ImageApi pulled image} instance
		 * @throws IOException on IO error
		 */
		public Image pull(ImageReference reference, ImagePlatform platform,
				UpdateListener<PullImageUpdateEvent> listener) throws IOException {
			return pull(reference, platform, listener, null);
		}

		/**
		 * Pull an image from a registry.
		 * @param reference the image reference to pull
		 * @param platform the platform (os/architecture/variant) of the image to pull
		 * @param listener a pull listener to receive update events
		 * @param registryAuth registry authentication credentials
		 * @return the {@link ImageApi pulled image} instance
		 * @throws IOException on IO error
		 */
		public Image pull(ImageReference reference, ImagePlatform platform,
				UpdateListener<PullImageUpdateEvent> listener, String registryAuth) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			Assert.notNull(listener, "'listener' must not be null");
			URI createUri = (platform != null)
					? buildUrl(PLATFORM_API_VERSION, "/images/create", "fromImage", reference, "platform", platform)
					: buildUrl("/images/create", "fromImage", reference);
			DigestCaptureUpdateListener digestCapture = new DigestCaptureUpdateListener();
			listener.onStart();
			try {
				try (Response response = http().post(createUri, registryAuth)) {
					jsonStream().get(response.getContent(), PullImageUpdateEvent.class, (event) -> {
						digestCapture.onUpdate(event);
						listener.onUpdate(event);
					});
				}
				return inspect((platform != null) ? PLATFORM_API_VERSION : API_VERSION, reference);
			}
			finally {
				listener.onFinish();
			}
		}

		/**
		 * Push an image to a registry.
		 * @param reference the image reference to push
		 * @param listener a push listener to receive update events
		 * @param registryAuth registry authentication credentials
		 * @throws IOException on IO error
		 */
		public void push(ImageReference reference, UpdateListener<PushImageUpdateEvent> listener, String registryAuth)
				throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			Assert.notNull(listener, "'listener' must not be null");
			URI pushUri = buildUrl("/images/" + reference + "/push");
			ErrorCaptureUpdateListener errorListener = new ErrorCaptureUpdateListener();
			listener.onStart();
			try {
				try (Response response = http().post(pushUri, registryAuth)) {
					jsonStream().get(response.getContent(), PushImageUpdateEvent.class, (event) -> {
						errorListener.onUpdate(event);
						listener.onUpdate(event);
					});
				}
			}
			finally {
				listener.onFinish();
			}
		}

		/**
		 * Load an {@link ImageArchive} into Docker.
		 * @param archive the archive to load
		 * @param listener a pull listener to receive update events
		 * @throws IOException on IO error
		 */
		public void load(ImageArchive archive, UpdateListener<LoadImageUpdateEvent> listener) throws IOException {
			Assert.notNull(archive, "'archive' must not be null");
			Assert.notNull(listener, "'listener' must not be null");
			URI loadUri = buildUrl("/images/load");
			LoadImageUpdateListener streamListener = new LoadImageUpdateListener(archive);
			listener.onStart();
			try {
				try (Response response = http().post(loadUri, "application/x-tar", archive::writeTo)) {
					jsonStream().get(response.getContent(), LoadImageUpdateEvent.class, (event) -> {
						streamListener.onUpdate(event);
						listener.onUpdate(event);
					});
				}
				streamListener.assertValidResponseReceived();
			}
			finally {
				listener.onFinish();
			}
		}

		/**
		 * Export the layers of an image as {@link TarArchive TarArchives}.
		 * @param reference the reference to export
		 * @param exports a consumer to receive the layers (contents can only be accessed
		 * during the callback)
		 * @throws IOException on IO error
		 */
		public void exportLayers(ImageReference reference, IOBiConsumer<String, TarArchive> exports)
				throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			Assert.notNull(exports, "'exports' must not be null");
			URI uri = buildUrl("/images/" + reference + "/get");
			try (Response response = http().get(uri)) {
				try (ExportedImageTar exportedImageTar = new ExportedImageTar(reference, response.getContent())) {
					exportedImageTar.exportLayers(exports);
				}
			}
		}

		/**
		 * Remove a specific image.
		 * @param reference the reference the remove
		 * @param force if removal should be forced
		 * @throws IOException on IO error
		 */
		public void remove(ImageReference reference, boolean force) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
			URI uri = buildUrl("/images/" + reference, params);
			http().delete(uri).close();
		}

		/**
		 * Inspect an image.
		 * @param reference the image reference
		 * @return the image from the local repository
		 * @throws IOException on IO error
		 */
		public Image inspect(ImageReference reference) throws IOException {
			return inspect(API_VERSION, reference);
		}

		private Image inspect(ApiVersion apiVersion, ImageReference reference) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			URI imageUri = buildUrl(apiVersion, "/images/" + reference + "/json");
			try (Response response = http().get(imageUri)) {
				return Image.of(response.getContent());
			}
		}

		public void tag(ImageReference sourceReference, ImageReference targetReference) throws IOException {
			Assert.notNull(sourceReference, "'sourceReference' must not be null");
			Assert.notNull(targetReference, "'targetReference' must not be null");
			String tag = targetReference.getTag();
			String path = "/images/" + sourceReference + "/tag";
			URI uri = (tag != null) ? buildUrl(path, "repo", targetReference.inTaglessForm(), "tag", tag)
					: buildUrl(path, "repo", targetReference);
			http().post(uri).close();
		}

	}

	/**
	 * Docker API for container operations.
	 */
	public class ContainerApi {

		ContainerApi() {
		}

		/**
		 * Create a new container a {@link ContainerConfig}.
		 * @param config the container config
		 * @param platform the platform (os/architecture/variant) of the image the
		 * container should be created from
		 * @param contents additional contents to include
		 * @return a {@link ContainerReference} for the newly created container
		 * @throws IOException on IO error
		 */
		public ContainerReference create(ContainerConfig config, ImagePlatform platform, ContainerContent... contents)
				throws IOException {
			Assert.notNull(config, "'config' must not be null");
			Assert.noNullElements(contents, "'contents' must not contain null elements");
			ContainerReference containerReference = createContainer(config, platform);
			for (ContainerContent content : contents) {
				uploadContainerContent(containerReference, content);
			}
			return containerReference;
		}

		private ContainerReference createContainer(ContainerConfig config, ImagePlatform platform) throws IOException {
			URI createUri = (platform != null)
					? buildUrl(PLATFORM_API_VERSION, "/containers/create", "platform", platform)
					: buildUrl("/containers/create");
			try (Response response = http().post(createUri, "application/json", config::writeTo)) {
				return ContainerReference
					.of(SharedObjectMapper.get().readTree(response.getContent()).at("/Id").asText());
			}
		}

		private void uploadContainerContent(ContainerReference reference, ContainerContent content) throws IOException {
			URI uri = buildUrl("/containers/" + reference + "/archive", "path", content.getDestinationPath());
			http().put(uri, "application/x-tar", content.getArchive()::writeTo).close();
		}

		/**
		 * Start a specific container.
		 * @param reference the container reference to start
		 * @throws IOException on IO error
		 */
		public void start(ContainerReference reference) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			URI uri = buildUrl("/containers/" + reference + "/start");
			http().post(uri).close();
		}

		/**
		 * Return and follow logs for a specific container.
		 * @param reference the container reference
		 * @param listener a listener to receive log update events
		 * @throws IOException on IO error
		 */
		public void logs(ContainerReference reference, UpdateListener<LogUpdateEvent> listener) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			Assert.notNull(listener, "'listener' must not be null");
			Object[] params = { "stdout", "1", "stderr", "1", "follow", "1" };
			URI uri = buildUrl("/containers/" + reference + "/logs", params);
			listener.onStart();
			try {
				try (Response response = http().get(uri)) {
					LogUpdateEvent.readAll(response.getContent(), listener::onUpdate);
				}
			}
			finally {
				listener.onFinish();
			}
		}

		/**
		 * Wait for a container to stop and retrieve the status.
		 * @param reference the container reference
		 * @return a {@link ContainerStatus} indicating the exit status of the container
		 * @throws IOException on IO error
		 */
		public ContainerStatus wait(ContainerReference reference) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			URI uri = buildUrl("/containers/" + reference + "/wait");
			try (Response response = http().post(uri)) {
				return ContainerStatus.of(response.getContent());
			}
		}

		/**
		 * Remove a specific container.
		 * @param reference the container to remove
		 * @param force if removal should be forced
		 * @throws IOException on IO error
		 */
		public void remove(ContainerReference reference, boolean force) throws IOException {
			Assert.notNull(reference, "'reference' must not be null");
			Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
			URI uri = buildUrl("/containers/" + reference, params);
			http().delete(uri).close();
		}

	}

	/**
	 * Docker API for volume operations.
	 */
	public class VolumeApi {

		VolumeApi() {
		}

		/**
		 * Delete a volume.
		 * @param name the name of the volume to delete
		 * @param force if the deletion should be forced
		 * @throws IOException on IO error
		 */
		public void delete(VolumeName name, boolean force) throws IOException {
			Assert.notNull(name, "'name' must not be null");
			Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
			URI uri = buildUrl("/volumes/" + name, params);
			http().delete(uri).close();
		}

	}

	/**
	 * Docker API for system operations.
	 */
	class SystemApi {

		SystemApi() {
		}

		/**
		 * Get the API version supported by the Docker daemon.
		 * @return the Docker daemon API version
		 */
		ApiVersion getApiVersion() {
			try {
				URI uri = new URIBuilder("/_ping").build();
				try (Response response = http().head(uri)) {
					Header apiVersionHeader = response.getHeader(API_VERSION_HEADER_NAME);
					if (apiVersionHeader != null) {
						return ApiVersion.parse(apiVersionHeader.getValue());
					}
				}
				catch (Exception ex) {
					// fall through to return default value
				}
				return UNKNOWN_API_VERSION;
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	/**
	 * {@link UpdateListener} used to capture the image digest.
	 */
	private static final class DigestCaptureUpdateListener implements UpdateListener<ProgressUpdateEvent> {

		private static final String PREFIX = "Digest:";

		private String digest;

		@Override
		public void onUpdate(ProgressUpdateEvent event) {
			String status = event.getStatus();
			if (status != null && status.startsWith(PREFIX)) {
				String digest = status.substring(PREFIX.length()).trim();
				Assert.state(this.digest == null || this.digest.equals(digest), "Different digests IDs provided");
				this.digest = digest;
			}
		}

	}

	/**
	 * {@link UpdateListener} for an image load response stream.
	 */
	private static final class LoadImageUpdateListener implements UpdateListener<LoadImageUpdateEvent> {

		private final ImageArchive archive;

		private String stream;

		private LoadImageUpdateListener(ImageArchive archive) {
			this.archive = archive;
		}

		@Override
		public void onUpdate(LoadImageUpdateEvent event) {
			Assert.state(event.getErrorDetail() == null,
					() -> "Error response received when loading image" + image() + ": " + event.getErrorDetail());
			this.stream = event.getStream();
		}

		private String image() {
			ImageReference tag = this.archive.getTag();
			return (tag != null) ? " \"" + tag + "\"" : "";
		}

		private void assertValidResponseReceived() {
			Assert.state(StringUtils.hasText(this.stream),
					() -> "Invalid response received when loading image" + image());
		}

	}

	/**
	 * {@link UpdateListener} used to capture the details of an error in a response
	 * stream.
	 */
	private static final class ErrorCaptureUpdateListener implements UpdateListener<PushImageUpdateEvent> {

		@Override
		public void onUpdate(PushImageUpdateEvent event) {
			Assert.state(event.getErrorDetail() == null,
					() -> "Error response received when pushing image: " + event.getErrorDetail().getMessage());
		}

	}

}
