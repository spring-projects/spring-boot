/*
 * Copyright 2012-2021 the original author or authors.
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.util.StreamUtils;

/**
 * A {@link Buildpack} that references a buildpack contained in a local gzipped tar
 * archive file.
 *
 * The archive must contain a buildpack descriptor named {@code buildpack.toml} at the
 * root of the archive. The contents of the archive will be provided as a single layer to
 * be included in the builder image.
 *
 * @author Scott Frederick
 */
final class TarGzipBuildpack implements Buildpack {

	private final Path path;

	private final BuildpackCoordinates coordinates;

	private TarGzipBuildpack(Path path) {
		this.path = path;
		this.coordinates = findBuildpackCoordinates(path);
	}

	private BuildpackCoordinates findBuildpackCoordinates(Path path) {
		try {
			try (TarArchiveInputStream tar = new TarArchiveInputStream(
					new GzipCompressorInputStream(Files.newInputStream(path)))) {
				ArchiveEntry entry = tar.getNextEntry();
				while (entry != null) {
					if ("buildpack.toml".equals(entry.getName())) {
						return BuildpackCoordinates.fromToml(tar, path);
					}
					entry = tar.getNextEntry();
				}
				throw new IllegalArgumentException(
						"Buildpack descriptor 'buildpack.toml' is required in buildpack '" + path + "'");
			}
		}
		catch (IOException ex) {
			throw new RuntimeException("Error parsing descriptor for buildpack '" + path + "'", ex);
		}
	}

	@Override
	public BuildpackCoordinates getCoordinates() {
		return this.coordinates;
	}

	@Override
	public void apply(IOConsumer<Layer> layers) throws IOException {
		layers.accept(Layer.fromTarArchive(this::copyAndRebaseEntries));
	}

	private void copyAndRebaseEntries(OutputStream outputStream) throws IOException {
		String id = this.coordinates.getSanitizedId();
		Path basePath = Paths.get("/cnb/buildpacks/", id, this.coordinates.getVersion());
		try (TarArchiveInputStream tar = new TarArchiveInputStream(
				new GzipCompressorInputStream(Files.newInputStream(this.path)));
				TarArchiveOutputStream output = new TarArchiveOutputStream(outputStream)) {
			writeBasePathEntries(output, basePath);
			TarArchiveEntry entry = tar.getNextTarEntry();
			while (entry != null) {
				entry.setName(basePath + "/" + entry.getName());
				output.putArchiveEntry(entry);
				StreamUtils.copy(tar, output);
				output.closeArchiveEntry();
				entry = tar.getNextTarEntry();
			}
			output.finish();
		}
	}

	private void writeBasePathEntries(TarArchiveOutputStream output, Path basePath) throws IOException {
		int pathCount = basePath.getNameCount();
		for (int pathIndex = 1; pathIndex < pathCount + 1; pathIndex++) {
			String name = "/" + basePath.subpath(0, pathIndex) + "/";
			TarArchiveEntry entry = new TarArchiveEntry(name);
			output.putArchiveEntry(entry);
			output.closeArchiveEntry();
		}
	}

	/**
	 * A {@link BuildpackResolver} compatible method to resolve tar-gzip buildpacks.
	 * @param context the resolver context
	 * @param reference the buildpack reference
	 * @return the resolved {@link Buildpack} or {@code null}
	 */
	static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
		Path path = reference.asPath();
		if (path != null && Files.exists(path) && Files.isRegularFile(path)) {
			return new TarGzipBuildpack(path);
		}
		return null;
	}

}
