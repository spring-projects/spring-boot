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
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.FilePermissions;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.io.Layout;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.util.Assert;

/**
 * A {@link Buildpack} that references a buildpack in a directory on the local file
 * system.
 *
 * The file system must contain a buildpack descriptor named {@code buildpack.toml} in the
 * root of the directory. The contents of the directory tree will be provided as a single
 * layer to be included in the builder image.
 *
 * @author Scott Frederick
 */
final class DirectoryBuildpack implements Buildpack {

	private final Path path;

	private final BuildpackCoordinates coordinates;

	private DirectoryBuildpack(Path path) {
		this.path = path;
		this.coordinates = findBuildpackCoordinates(path);
	}

	private BuildpackCoordinates findBuildpackCoordinates(Path path) {
		Path buildpackToml = path.resolve("buildpack.toml");
		Assert.isTrue(Files.exists(buildpackToml),
				() -> "Buildpack descriptor 'buildpack.toml' is required in buildpack '" + path + "'");
		try {
			try (InputStream inputStream = Files.newInputStream(buildpackToml)) {
				return BuildpackCoordinates.fromToml(inputStream, path);
			}
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Error parsing descriptor for buildpack '" + path + "'", ex);
		}
	}

	@Override
	public BuildpackCoordinates getCoordinates() {
		return this.coordinates;
	}

	@Override
	public void apply(IOConsumer<Layer> layers) throws IOException {
		layers.accept(Layer.of(this::addLayerContent));
	}

	private void addLayerContent(Layout layout) throws IOException {
		String id = this.coordinates.getSanitizedId();
		Path cnbPath = Paths.get("/cnb/buildpacks/", id, this.coordinates.getVersion());
		writeBasePathEntries(layout, cnbPath);
		Files.walkFileTree(this.path, new LayoutFileVisitor(this.path, cnbPath, layout));
	}

	private void writeBasePathEntries(Layout layout, Path basePath) throws IOException {
		int pathCount = basePath.getNameCount();
		for (int pathIndex = 1; pathIndex < pathCount + 1; pathIndex++) {
			String name = "/" + basePath.subpath(0, pathIndex) + "/";
			layout.directory(name, Owner.ROOT);
		}
	}

	/**
	 * A {@link BuildpackResolver} compatible method to resolve directory buildpacks.
	 * @param context the resolver context
	 * @param reference the buildpack reference
	 * @return the resolved {@link Buildpack} or {@code null}
	 */
	static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
		Path path = reference.asPath();
		if (path != null && Files.exists(path) && Files.isDirectory(path)) {
			return new DirectoryBuildpack(path);
		}
		return null;
	}

	/**
	 * {@link SimpleFileVisitor} to used to create the {@link Layout}.
	 */
	private static class LayoutFileVisitor extends SimpleFileVisitor<Path> {

		private final Path basePath;

		private final Path layerPath;

		private final Layout layout;

		LayoutFileVisitor(Path basePath, Path layerPath, Layout layout) {
			this.basePath = basePath;
			this.layerPath = layerPath;
			this.layout = layout;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!dir.equals(this.basePath)) {
				this.layout.directory(relocate(dir), Owner.ROOT, getMode(dir));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			this.layout.file(relocate(file), Owner.ROOT, getMode(file), Content.of(file.toFile()));
			return FileVisitResult.CONTINUE;
		}

		private int getMode(Path path) throws IOException {
			try {
				return FilePermissions.umaskForPath(path);
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException(
						"Buildpack content in a directory is not supported on this operating system");
			}
		}

		private String relocate(Path path) {
			Path node = path.subpath(this.basePath.getNameCount(), path.getNameCount());
			return Paths.get(this.layerPath.toString(), node.toString()).toString();
		}

	}

}
