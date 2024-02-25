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

package org.springframework.boot.buildpack.platform.docker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hc.core5.net.URIBuilder;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport;
import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport.Response;
import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig;
import org.springframework.boot.buildpack.platform.docker.type.ContainerContent;
import org.springframework.boot.buildpack.platform.docker.type.ContainerReference;
import org.springframework.boot.buildpack.platform.docker.type.ContainerStatus;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchiveManifest;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.json.JsonStream;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
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

	static final String API_VERSION = "v1.24";

	private final HttpTransport http;

	private final JsonStream jsonStream;

	private final ImageApi image;

	private final ContainerApi container;

	private final VolumeApi volume;

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
	}

	/**
	 * Returns the HttpTransport object used by the DockerApi class.
	 * @return the HttpTransport object used by the DockerApi class
	 */
	private HttpTransport http() {
		return this.http;
	}

	/**
	 * Returns the JSON stream associated with this DockerApi instance.
	 * @return the JSON stream
	 */
	private JsonStream jsonStream() {
		return this.jsonStream;
	}

	/**
	 * Builds a URL with the given path and parameters.
	 * @param path the path of the URL
	 * @param params the collection of parameters to be included in the URL
	 * @return the built URL as a URI object
	 */
	private URI buildUrl(String path, Collection<?> params) {
		return buildUrl(path, (params != null) ? params.toArray() : null);
	}

	/**
	 * Builds a URL with the given path and parameters.
	 * @param path the path of the URL
	 * @param params the parameters to be added to the URL
	 * @return the built URL
	 * @throws IllegalStateException if a URISyntaxException occurs
	 */
	private URI buildUrl(String path, Object... params) {
		try {
			URIBuilder builder = new URIBuilder("/" + API_VERSION + path);
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

	/**
	 * Returns the VolumeApi object associated with this DockerApi instance.
	 * @return the VolumeApi object
	 */
	public VolumeApi volume() {
		return this.volume;
	}

	/**
	 * Docker API for image operations.
	 */
	public class ImageApi {

		/**
		 * Constructs a new instance of the ImageApi class.
		 */
		ImageApi() {
		}

		/**
		 * Pull an image from a registry.
		 * @param reference the image reference to pull
		 * @param listener a pull listener to receive update events
		 * @return the {@link ImageApi pulled image} instance
		 * @throws IOException on IO error
		 */
		public Image pull(ImageReference reference, UpdateListener<PullImageUpdateEvent> listener) throws IOException {
			return pull(reference, listener, null);
		}

		/**
		 * Pull an image from a registry.
		 * @param reference the image reference to pull
		 * @param listener a pull listener to receive update events
		 * @param registryAuth registry authentication credentials
		 * @return the {@link ImageApi pulled image} instance
		 * @throws IOException on IO error
		 */
		public Image pull(ImageReference reference, UpdateListener<PullImageUpdateEvent> listener, String registryAuth)
				throws IOException {
			Assert.notNull(reference, "Reference must not be null");
			Assert.notNull(listener, "Listener must not be null");
			URI createUri = buildUrl("/images/create", "fromImage", reference);
			DigestCaptureUpdateListener digestCapture = new DigestCaptureUpdateListener();
			listener.onStart();
			try {
				try (Response response = http().post(createUri, registryAuth)) {
					jsonStream().get(response.getContent(), PullImageUpdateEvent.class, (event) -> {
						digestCapture.onUpdate(event);
						listener.onUpdate(event);
					});
				}
				return inspect(reference);
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
			Assert.notNull(reference, "Reference must not be null");
			Assert.notNull(listener, "Listener must not be null");
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
			Assert.notNull(archive, "Archive must not be null");
			Assert.notNull(listener, "Listener must not be null");
			URI loadUri = buildUrl("/images/load");
			StreamCaptureUpdateListener streamListener = new StreamCaptureUpdateListener();
			listener.onStart();
			try {
				try (Response response = http().post(loadUri, "application/x-tar", archive::writeTo)) {
					jsonStream().get(response.getContent(), LoadImageUpdateEvent.class, (event) -> {
						streamListener.onUpdate(event);
						listener.onUpdate(event);
					});
				}
				Assert.state(StringUtils.hasText(streamListener.getCapturedStream()),
						"Invalid response received when loading image "
								+ ((archive.getTag() != null) ? "\"" + archive.getTag() + "\"" : ""));
			}
			finally {
				listener.onFinish();
			}
		}

		/**
		 * Export the layers of an image as {@link TarArchive}s.
		 * @param reference the reference to export
		 * @param exports a consumer to receive the layers (contents can only be accessed
		 * during the callback)
		 * @throws IOException on IO error
		 */
		public void exportLayers(ImageReference reference, IOBiConsumer<String, TarArchive> exports)
				throws IOException {
			exportLayerFiles(reference, (name, path) -> {
				try (InputStream in = Files.newInputStream(path)) {
					TarArchive archive = (out) -> StreamUtils.copy(in, out);
					exports.accept(name, archive);
				}
			});
		}

		/**
		 * Export the layers of an image as paths to layer tar files.
		 * @param reference the reference to export
		 * @param exports a consumer to receive the layer tar file paths (file can only be
		 * accessed during the callback)
		 * @throws IOException on IO error
		 * @since 2.7.10
		 */
		public void exportLayerFiles(ImageReference reference, IOBiConsumer<String, Path> exports) throws IOException {
			Assert.notNull(reference, "Reference must not be null");
			Assert.notNull(exports, "Exports must not be null");
			URI saveUri = buildUrl("/images/" + reference + "/get");
			Response response = http().get(saveUri);
			Path exportFile = copyToTemp(response.getContent());
			ImageArchiveManifest manifest = getManifest(reference, exportFile);
			try (TarArchiveInputStream tar = new TarArchiveInputStream(new FileInputStream(exportFile.toFile()))) {
				TarArchiveEntry entry = tar.getNextEntry();
				while (entry != null) {
					if (manifestContainsLayerEntry(manifest, entry.getName())) {
						Path layerFile = copyToTemp(tar);
						exports.accept(entry.getName(), layerFile);
						Files.delete(layerFile);
					}
					entry = tar.getNextEntry();
				}
			}
			Files.delete(exportFile);
		}

		/**
		 * Remove a specific image.
		 * @param reference the reference the remove
		 * @param force if removal should be forced
		 * @throws IOException on IO error
		 */
		public void remove(ImageReference reference, boolean force) throws IOException {
			Assert.notNull(reference, "Reference must not be null");
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
			Assert.notNull(reference, "Reference must not be null");
			URI imageUri = buildUrl("/images/" + reference + "/json");
			try (Response response = http().get(imageUri)) {
				return Image.of(response.getContent());
			}
		}

		/**
		 * Tags an image with the specified source and target references.
		 * @param sourceReference the reference to the source image
		 * @param targetReference the reference to the target image
		 * @throws IOException if an I/O error occurs
		 * @throws IllegalArgumentException if either sourceReference or targetReference
		 * is null
		 */
		public void tag(ImageReference sourceReference, ImageReference targetReference) throws IOException {
			Assert.notNull(sourceReference, "SourceReference must not be null");
			Assert.notNull(targetReference, "TargetReference must not be null");
			String tag = targetReference.getTag();
			String path = "/images/" + sourceReference + "/tag";
			URI uri = (tag != null) ? buildUrl(path, "repo", targetReference.inTaglessForm(), "tag", tag)
					: buildUrl(path, "repo", targetReference);
			http().post(uri).close();
		}

		/**
		 * Retrieves the manifest for a given image reference from an export file.
		 * @param reference the image reference to retrieve the manifest for
		 * @param exportFile the path to the export file containing the image data
		 * @return the ImageArchiveManifest object representing the manifest for the image
		 * @throws IOException if an I/O error occurs while reading the export file
		 * @throws IllegalArgumentException if the manifest is not found in the image
		 */
		private ImageArchiveManifest getManifest(ImageReference reference, Path exportFile) throws IOException {
			try (TarArchiveInputStream tar = new TarArchiveInputStream(new FileInputStream(exportFile.toFile()))) {
				TarArchiveEntry entry = tar.getNextEntry();
				while (entry != null) {
					if (entry.getName().equals("manifest.json")) {
						return readManifest(tar);
					}
					entry = tar.getNextEntry();
				}
			}
			throw new IllegalArgumentException("Manifest not found in image " + reference);
		}

		/**
		 * Reads the manifest file from the given TarArchiveInputStream and returns an
		 * ImageArchiveManifest object.
		 * @param tar The TarArchiveInputStream from which to read the manifest file.
		 * @return The ImageArchiveManifest object representing the manifest file.
		 * @throws IOException If an I/O error occurs while reading the manifest file.
		 */
		private ImageArchiveManifest readManifest(TarArchiveInputStream tar) throws IOException {
			String manifestContent = new BufferedReader(new InputStreamReader(tar, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining());
			return ImageArchiveManifest.of(new ByteArrayInputStream(manifestContent.getBytes(StandardCharsets.UTF_8)));
		}

		/**
		 * Copies the contents of the given input stream to a temporary file and returns
		 * the path of the temporary file.
		 * @param in the input stream to copy from
		 * @return the path of the temporary file
		 * @throws IOException if an I/O error occurs during the copy process
		 */
		private Path copyToTemp(InputStream in) throws IOException {
			Path path = Files.createTempFile("create-builder-scratch-", null);
			try (OutputStream out = Files.newOutputStream(path)) {
				StreamUtils.copy(in, out);
			}
			return path;
		}

		/**
		 * Checks if the given ImageArchiveManifest contains a layer entry with the
		 * specified layerId.
		 * @param manifest the ImageArchiveManifest to check
		 * @param layerId the layerId to search for
		 * @return true if the manifest contains a layer entry with the specified layerId,
		 * false otherwise
		 */
		private boolean manifestContainsLayerEntry(ImageArchiveManifest manifest, String layerId) {
			return manifest.getEntries().stream().anyMatch((content) -> content.getLayers().contains(layerId));
		}

	}

	/**
	 * Docker API for container operations.
	 */
	public class ContainerApi {

		/**
		 * Creates a new instance of ContainerApi.
		 */
		ContainerApi() {
		}

		/**
		 * Create a new container a {@link ContainerConfig}.
		 * @param config the container config
		 * @param contents additional contents to include
		 * @return a {@link ContainerReference} for the newly created container
		 * @throws IOException on IO error
		 */
		public ContainerReference create(ContainerConfig config, ContainerContent... contents) throws IOException {
			Assert.notNull(config, "Config must not be null");
			Assert.noNullElements(contents, "Contents must not contain null elements");
			ContainerReference containerReference = createContainer(config);
			for (ContainerContent content : contents) {
				uploadContainerContent(containerReference, content);
			}
			return containerReference;
		}

		/**
		 * Creates a container with the given configuration.
		 * @param config the configuration for the container
		 * @return a reference to the created container
		 * @throws IOException if an I/O error occurs during the creation of the container
		 */
		private ContainerReference createContainer(ContainerConfig config) throws IOException {
			URI createUri = buildUrl("/containers/create");
			try (Response response = http().post(createUri, "application/json", config::writeTo)) {
				return ContainerReference
					.of(SharedObjectMapper.get().readTree(response.getContent()).at("/Id").asText());
			}
		}

		/**
		 * Uploads the content of a container to a specified destination path.
		 * @param reference the reference to the container
		 * @param content the container content to be uploaded
		 * @throws IOException if an I/O error occurs during the upload process
		 */
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
			Assert.notNull(reference, "Reference must not be null");
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
			Assert.notNull(reference, "Reference must not be null");
			Assert.notNull(listener, "Listener must not be null");
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
			Assert.notNull(reference, "Reference must not be null");
			URI uri = buildUrl("/containers/" + reference + "/wait");
			Response response = http().post(uri);
			return ContainerStatus.of(response.getContent());
		}

		/**
		 * Remove a specific container.
		 * @param reference the container to remove
		 * @param force if removal should be forced
		 * @throws IOException on IO error
		 */
		public void remove(ContainerReference reference, boolean force) throws IOException {
			Assert.notNull(reference, "Reference must not be null");
			Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
			URI uri = buildUrl("/containers/" + reference, params);
			http().delete(uri).close();
		}

	}

	/**
	 * Docker API for volume operations.
	 */
	public class VolumeApi {

		/**
		 * This method is used to perform volume operations.
		 */
		VolumeApi() {
		}

		/**
		 * Delete a volume.
		 * @param name the name of the volume to delete
		 * @param force if the deletion should be forced
		 * @throws IOException on IO error
		 */
		public void delete(VolumeName name, boolean force) throws IOException {
			Assert.notNull(name, "Name must not be null");
			Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
			URI uri = buildUrl("/volumes/" + name, params);
			http().delete(uri).close();
		}

	}

	/**
	 * {@link UpdateListener} used to capture the image digest.
	 */
	private static final class DigestCaptureUpdateListener implements UpdateListener<ProgressUpdateEvent> {

		private static final String PREFIX = "Digest:";

		private String digest;

		/**
		 * This method is called when a progress update event occurs. It updates the
		 * digest status based on the event status.
		 * @param event The progress update event
		 */
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
	 * {@link UpdateListener} used to ensure an image load response stream.
	 */
	private static final class StreamCaptureUpdateListener implements UpdateListener<LoadImageUpdateEvent> {

		private String stream;

		/**
		 * Updates the stream with the provided image.
		 * @param event the LoadImageUpdateEvent containing the updated stream
		 */
		@Override
		public void onUpdate(LoadImageUpdateEvent event) {
			this.stream = event.getStream();
		}

		/**
		 * Returns the captured stream.
		 * @return the captured stream
		 */
		String getCapturedStream() {
			return this.stream;
		}

	}

	/**
	 * {@link UpdateListener} used to capture the details of an error in a response
	 * stream.
	 */
	private static final class ErrorCaptureUpdateListener implements UpdateListener<PushImageUpdateEvent> {

		/**
		 * This method is called when an image update event occurs. It checks if there is
		 * any error detail in the event and throws an exception if an error response is
		 * received.
		 * @param event The PushImageUpdateEvent object representing the image update
		 * event.
		 * @throws IllegalStateException if an error response is received when pushing the
		 * image.
		 */
		@Override
		public void onUpdate(PushImageUpdateEvent event) {
			Assert.state(event.getErrorDetail() == null,
					() -> "Error response received when pushing image: " + event.getErrorDetail().getMessage());
		}

	}

}
