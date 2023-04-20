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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.InspectedContent;
import org.springframework.boot.buildpack.platform.io.Layout;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.util.Assert;

/**
 * An image archive that can be loaded into Docker.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 * @see #from(Image, IOConsumer)
 * @see <a href="https://github.com/moby/moby/blob/master/image/spec/v1.2.md">Docker Image
 * Specification</a>
 */
public class ImageArchive implements TarArchive {

	private static final Instant WINDOWS_EPOCH_PLUS_SECOND = OffsetDateTime.of(1980, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC)
		.toInstant();

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME
		.withZone(ZoneOffset.UTC);

	private static final String EMPTY_LAYER_NAME_PREFIX = "blank_";

	private static final IOConsumer<Update> NO_UPDATES = (update) -> {
	};

	private final ObjectMapper objectMapper;

	private final ImageConfig imageConfig;

	private final Instant createDate;

	private final ImageReference tag;

	private final String os;

	private final List<LayerId> existingLayers;

	private final List<Layer> newLayers;

	ImageArchive(ObjectMapper objectMapper, ImageConfig imageConfig, Instant createDate, ImageReference tag, String os,
			List<LayerId> existingLayers, List<Layer> newLayers) {
		this.objectMapper = objectMapper;
		this.imageConfig = imageConfig;
		this.createDate = createDate;
		this.tag = tag;
		this.os = os;
		this.existingLayers = existingLayers;
		this.newLayers = newLayers;
	}

	/**
	 * Return the image config for the archive.
	 * @return the image config
	 */
	public ImageConfig getImageConfig() {
		return this.imageConfig;
	}

	/**
	 * Return the create date of the archive.
	 * @return the create date
	 */
	public Instant getCreateDate() {
		return this.createDate;
	}

	/**
	 * Return the tag of the archive.
	 * @return the tag
	 */
	public ImageReference getTag() {
		return this.tag;
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException {
		TarArchive.of(this::write).writeTo(outputStream);
	}

	private void write(Layout writer) throws IOException {
		List<LayerId> writtenLayers = writeLayers(writer);
		String config = writeConfig(writer, writtenLayers);
		writeManifest(writer, config, writtenLayers);
	}

	private List<LayerId> writeLayers(Layout writer) throws IOException {
		for (int i = 0; i < this.existingLayers.size(); i++) {
			writeEmptyLayer(writer, EMPTY_LAYER_NAME_PREFIX + i);
		}
		List<LayerId> writtenLayers = new ArrayList<>();
		for (Layer layer : this.newLayers) {
			writtenLayers.add(writeLayer(writer, layer));
		}
		return Collections.unmodifiableList(writtenLayers);
	}

	private void writeEmptyLayer(Layout writer, String name) throws IOException {
		writer.file(name, Owner.ROOT, Content.of(""));
	}

	private LayerId writeLayer(Layout writer, Layer layer) throws IOException {
		LayerId id = layer.getId();
		writer.file(id.getHash() + ".tar", Owner.ROOT, layer);
		return id;
	}

	private String writeConfig(Layout writer, List<LayerId> writtenLayers) throws IOException {
		try {
			ObjectNode config = createConfig(writtenLayers);
			String json = this.objectMapper.writeValueAsString(config).replace("\r\n", "\n");
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			InspectedContent content = InspectedContent.of(Content.of(json), digest::update);
			String name = LayerId.ofSha256Digest(digest.digest()).getHash() + ".json";
			writer.file(name, Owner.ROOT, content);
			return name;
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ObjectNode createConfig(List<LayerId> writtenLayers) {
		ObjectNode config = this.objectMapper.createObjectNode();
		config.set("config", this.imageConfig.getNodeCopy());
		config.set("created", config.textNode(getCreatedDate()));
		config.set("history", createHistory(writtenLayers));
		config.set("os", config.textNode(this.os));
		config.set("rootfs", createRootFs(writtenLayers));
		return config;
	}

	private String getCreatedDate() {
		return DATE_FORMATTER.format(this.createDate);
	}

	private JsonNode createHistory(List<LayerId> writtenLayers) {
		ArrayNode history = this.objectMapper.createArrayNode();
		int size = this.existingLayers.size() + writtenLayers.size();
		for (int i = 0; i < size; i++) {
			history.addObject();
		}
		return history;
	}

	private JsonNode createRootFs(List<LayerId> writtenLayers) {
		ObjectNode rootFs = this.objectMapper.createObjectNode();
		ArrayNode diffIds = rootFs.putArray("diff_ids");
		this.existingLayers.stream().map(Object::toString).forEach(diffIds::add);
		writtenLayers.stream().map(Object::toString).forEach(diffIds::add);
		return rootFs;
	}

	private void writeManifest(Layout writer, String config, List<LayerId> writtenLayers) throws IOException {
		ArrayNode manifest = createManifest(config, writtenLayers);
		String manifestJson = this.objectMapper.writeValueAsString(manifest);
		writer.file("manifest.json", Owner.ROOT, Content.of(manifestJson));
	}

	private ArrayNode createManifest(String config, List<LayerId> writtenLayers) {
		ArrayNode manifest = this.objectMapper.createArrayNode();
		ObjectNode entry = manifest.addObject();
		entry.set("Config", entry.textNode(config));
		entry.set("Layers", getManifestLayers(writtenLayers));
		if (this.tag != null) {
			entry.set("RepoTags", entry.arrayNode().add(this.tag.toString()));
		}
		return manifest;
	}

	private ArrayNode getManifestLayers(List<LayerId> writtenLayers) {
		ArrayNode layers = this.objectMapper.createArrayNode();
		for (int i = 0; i < this.existingLayers.size(); i++) {
			layers.add(EMPTY_LAYER_NAME_PREFIX + i);
		}
		writtenLayers.stream().map((id) -> id.getHash() + ".tar").forEach(layers::add);
		return layers;
	}

	/**
	 * Create a new {@link ImageArchive} based on an existing {@link Image}.
	 * @param image the image that this archive is based on
	 * @return the new image archive.
	 * @throws IOException on IO error
	 */
	public static ImageArchive from(Image image) throws IOException {
		return from(image, NO_UPDATES);
	}

	/**
	 * Create a new {@link ImageArchive} based on an existing {@link Image}.
	 * @param image the image that this archive is based on
	 * @param update consumer to apply updates
	 * @return the new image archive.
	 * @throws IOException on IO error
	 */
	public static ImageArchive from(Image image, IOConsumer<Update> update) throws IOException {
		return new Update(image).applyTo(update);
	}

	/**
	 * Update class used to change data when creating an image archive.
	 */
	public static final class Update {

		private final Image image;

		private ImageConfig config;

		private Instant createDate;

		private ImageReference tag;

		private final List<Layer> newLayers = new ArrayList<>();

		private Update(Image image) {
			this.image = image;
			this.config = image.getConfig();
		}

		private ImageArchive applyTo(IOConsumer<Update> update) throws IOException {
			update.accept(this);
			Instant createDate = (this.createDate != null) ? this.createDate : WINDOWS_EPOCH_PLUS_SECOND;
			return new ImageArchive(SharedObjectMapper.get(), this.config, createDate, this.tag, this.image.getOs(),
					this.image.getLayers(), Collections.unmodifiableList(this.newLayers));
		}

		/**
		 * Apply updates to the {@link ImageConfig}.
		 * @param update consumer to apply updates
		 */
		public void withUpdatedConfig(Consumer<ImageConfig.Update> update) {
			this.config = this.config.copy(update);
		}

		/**
		 * Add a new layer to the image archive.
		 * @param layer the layer to add
		 */
		public void withNewLayer(Layer layer) {
			Assert.notNull(layer, "Layer must not be null");
			this.newLayers.add(layer);
		}

		/**
		 * Set the create date for the image archive.
		 * @param createDate the create date
		 */
		public void withCreateDate(Instant createDate) {
			Assert.notNull(createDate, "CreateDate must not be null");
			this.createDate = createDate;
		}

		/**
		 * Set the tag for the image archive.
		 * @param tag the tag
		 */
		public void withTag(ImageReference tag) {
			Assert.notNull(tag, "Tag must not be null");
			this.tag = tag.inTaggedForm();
		}

	}

}
