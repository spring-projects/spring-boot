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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.springframework.boot.buildpack.platform.docker.type.BlobReference;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchiveIndex;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchiveManifest;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.Manifest;
import org.springframework.boot.buildpack.platform.docker.type.ManifestList;
import org.springframework.boot.buildpack.platform.io.IOBiConsumer;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.buildpack.platform.io.TarArchive.Compression;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingFunction;

/**
 * Internal helper class used by the {@link DockerApi} to extract layers from an exported
 * image tar.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @author Scott Frederick
 */
class ExportedImageTar implements Closeable {

	private final Path tarFile;

	private final LayerArchiveFactory layerArchiveFactory;

	ExportedImageTar(ImageReference reference, InputStream inputStream) throws IOException {
		this.tarFile = Files.createTempFile("docker-layers-", null);
		Files.copy(inputStream, this.tarFile, StandardCopyOption.REPLACE_EXISTING);
		this.layerArchiveFactory = LayerArchiveFactory.create(reference, this.tarFile);
	}

	void exportLayers(IOBiConsumer<String, TarArchive> exports) throws IOException {
		try (TarArchiveInputStream tar = openTar(this.tarFile)) {
			TarArchiveEntry entry = tar.getNextEntry();
			while (entry != null) {
				TarArchive layerArchive = this.layerArchiveFactory.getLayerArchive(tar, entry);
				if (layerArchive != null) {
					exports.accept(entry.getName(), layerArchive);
				}
				entry = tar.getNextEntry();
			}
		}
	}

	private static TarArchiveInputStream openTar(Path path) throws IOException {
		return new TarArchiveInputStream(Files.newInputStream(path));
	}

	@Override
	public void close() throws IOException {
		Files.delete(this.tarFile);
	}

	/**
	 * Factory class used to create a {@link TarArchiveEntry} for layer.
	 */
	private abstract static class LayerArchiveFactory {

		/**
		 * Create a new {@link TarArchive} if the given entry represents a layer.
		 * @param tar the tar input stream
		 * @param entry the candidate entry
		 * @return a new {@link TarArchive} instance or {@code null} if this entry is not
		 * a layer.
		 */
		abstract TarArchive getLayerArchive(TarArchiveInputStream tar, TarArchiveEntry entry);

		/**
		 * Create a new {@link LayerArchiveFactory} for the given tar file using either
		 * the {@code index.json} or {@code manifest.json} to detect layers.
		 * @param reference the image that was referenced
		 * @param tarFile the source tar file
		 * @return a new {@link LayerArchiveFactory} instance
		 * @throws IOException on IO error
		 */
		static LayerArchiveFactory create(ImageReference reference, Path tarFile) throws IOException {
			try (TarArchiveInputStream tar = openTar(tarFile)) {
				ImageArchiveIndex index = null;
				ImageArchiveManifest manifest = null;
				TarArchiveEntry entry = tar.getNextEntry();
				while (entry != null) {
					if ("index.json".equals(entry.getName())) {
						index = ImageArchiveIndex.of(tar);
						break;
					}
					if ("manifest.json".equals(entry.getName())) {
						manifest = ImageArchiveManifest.of(tar);
					}
					entry = tar.getNextEntry();
				}
				Assert.state(index != null || manifest != null,
						"Exported image '%s' does not contain 'index.json' or 'manifest.json'".formatted(reference));
				return (index != null) ? new IndexLayerArchiveFactory(tarFile, index)
						: new ManifestLayerArchiveFactory(tarFile, manifest);
			}
		}

	}

	/**
	 * {@link LayerArchiveFactory} backed by the more recent {@code index.json} file.
	 */
	private static class IndexLayerArchiveFactory extends LayerArchiveFactory {

		private final Map<String, String> layerMediaTypes;

		IndexLayerArchiveFactory(Path tarFile, ImageArchiveIndex index) throws IOException {
			Set<String> manifestDigests = getDigests(index, this::isManifest);
			List<ManifestList> manifestLists = getManifestLists(tarFile, getDigests(index, this::isManifestList));
			List<Manifest> manifests = getManifests(tarFile, manifestDigests, manifestLists);
			this.layerMediaTypes = manifests.stream()
				.flatMap((manifest) -> manifest.getLayers().stream())
				.collect(Collectors.toMap(this::getEntryName, BlobReference::getMediaType));
		}

		private Set<String> getDigests(ImageArchiveIndex index, Predicate<BlobReference> predicate) {
			return index.getManifests()
				.stream()
				.filter(predicate)
				.map(BlobReference::getDigest)
				.collect(Collectors.toUnmodifiableSet());
		}

		private List<ManifestList> getManifestLists(Path tarFile, Set<String> digests) throws IOException {
			return getDigestMatches(tarFile, digests, ManifestList::of);
		}

		private List<Manifest> getManifests(Path tarFile, Set<String> manifestDigests, List<ManifestList> manifestLists)
				throws IOException {
			Set<String> digests = new HashSet<>(manifestDigests);
			manifestLists.stream()
				.flatMap(ManifestList::streamManifests)
				.filter(this::isManifest)
				.map(BlobReference::getDigest)
				.forEach(digests::add);
			return getDigestMatches(tarFile, digests, Manifest::of);
		}

		private <T> List<T> getDigestMatches(Path tarFile, Set<String> digests,
				ThrowingFunction<InputStream, T> factory) throws IOException {
			if (digests.isEmpty()) {
				return Collections.emptyList();
			}
			Set<String> names = digests.stream().map(this::getEntryName).collect(Collectors.toUnmodifiableSet());
			List<T> result = new ArrayList<>();
			try (TarArchiveInputStream tar = openTar(tarFile)) {
				TarArchiveEntry entry = tar.getNextEntry();
				while (entry != null) {
					if (names.contains(entry.getName())) {
						result.add(factory.apply(tar));
					}
					entry = tar.getNextEntry();
				}
			}
			return Collections.unmodifiableList(result);
		}

		private boolean isManifest(BlobReference reference) {
			return isJsonWithPrefix(reference.getMediaType(), "application/vnd.oci.image.manifest.v")
					|| isJsonWithPrefix(reference.getMediaType(), "application/vnd.docker.distribution.manifest.v");
		}

		private boolean isManifestList(BlobReference reference) {
			return isJsonWithPrefix(reference.getMediaType(), "application/vnd.docker.distribution.manifest.list.v");
		}

		private boolean isJsonWithPrefix(String mediaType, String prefix) {
			return mediaType.startsWith(prefix) && mediaType.endsWith("+json");
		}

		private String getEntryName(BlobReference reference) {
			return getEntryName(reference.getDigest());
		}

		private String getEntryName(String digest) {
			return "blobs/" + digest.replace(':', '/');
		}

		@Override
		TarArchive getLayerArchive(TarArchiveInputStream tar, TarArchiveEntry entry) {
			String mediaType = this.layerMediaTypes.get(entry.getName());
			if (mediaType == null) {
				return null;
			}
			return TarArchive.fromInputStream(tar, getCompression(mediaType));
		}

		private Compression getCompression(String mediaType) {
			if (mediaType.endsWith(".tar.gzip")) {
				return Compression.GZIP;
			}
			if (mediaType.endsWith(".tar.zstd")) {
				return Compression.ZSTD;
			}
			return Compression.NONE;
		}

	}

	/**
	 * {@link LayerArchiveFactory} backed by the legacy {@code manifest.json} file.
	 */
	private static class ManifestLayerArchiveFactory extends LayerArchiveFactory {

		private Set<String> layers;

		ManifestLayerArchiveFactory(Path tarFile, ImageArchiveManifest manifest) {
			this.layers = manifest.getEntries()
				.stream()
				.flatMap((entry) -> entry.getLayers().stream())
				.collect(Collectors.toUnmodifiableSet());
		}

		@Override
		TarArchive getLayerArchive(TarArchiveInputStream tar, TarArchiveEntry entry) {
			if (!this.layers.contains(entry.getName())) {
				return null;
			}
			return TarArchive.fromInputStream(tar, Compression.NONE);
		}

	}

}
