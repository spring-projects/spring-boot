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

	/**
     * Constructs a new DirectoryBuildpack object with the specified path.
     * 
     * @param path the path to the directory containing the buildpack
     */
    private DirectoryBuildpack(Path path) {
		this.path = path;
		this.coordinates = findBuildpackCoordinates(path);
	}

	/**
     * Finds the buildpack coordinates for a given path.
     * 
     * @param path The path to the buildpack directory.
     * @return The buildpack coordinates.
     * @throws IllegalArgumentException If there is an error parsing the buildpack descriptor.
     * @throws AssertException If the buildpack descriptor 'buildpack.toml' is not found.
     */
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

	/**
     * Returns the coordinates of the buildpack.
     * 
     * @return the coordinates of the buildpack
     */
    @Override
	public BuildpackCoordinates getCoordinates() {
		return this.coordinates;
	}

	/**
     * Applies the given layers to the DirectoryBuildpack.
     * 
     * @param layers the layers to be applied
     * @throws IOException if an I/O error occurs
     */
    @Override
	public void apply(IOConsumer<Layer> layers) throws IOException {
		layers.accept(Layer.of(this::addLayerContent));
	}

	/**
     * Adds the content of a layer to the given layout.
     * 
     * @param layout the layout to add the layer content to
     * @throws IOException if an I/O error occurs while accessing the files
     */
    private void addLayerContent(Layout layout) throws IOException {
		String id = this.coordinates.getSanitizedId();
		Path cnbPath = Paths.get("/cnb/buildpacks/", id, this.coordinates.getVersion());
		writeBasePathEntries(layout, cnbPath);
		Files.walkFileTree(this.path, new LayoutFileVisitor(this.path, cnbPath, layout));
	}

	/**
     * Writes base path entries to the layout.
     * 
     * @param layout The layout to write the entries to.
     * @param basePath The base path to generate entries from.
     * @throws IOException If an I/O error occurs.
     */
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

		/**
         * Constructs a new LayoutFileVisitor with the specified base path, layer path, and layout.
         * 
         * @param basePath the base path for the visitor
         * @param layerPath the layer path for the visitor
         * @param layout the layout for the visitor
         */
        LayoutFileVisitor(Path basePath, Path layerPath, Layout layout) {
			this.basePath = basePath;
			this.layerPath = layerPath;
			this.layout = layout;
		}

		/**
         * This method is called before visiting a directory during the file visiting process.
         * It creates a directory in the layout if the directory is not the base path.
         * 
         * @param dir The path of the directory being visited.
         * @param attrs The basic file attributes of the directory.
         * @return The file visit result indicating whether to continue or terminate the visiting process.
         * @throws IOException If an I/O error occurs during the directory creation.
         */
        @Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!dir.equals(this.basePath)) {
				this.layout.directory(relocate(dir), Owner.ROOT, getMode(dir));
			}
			return FileVisitResult.CONTINUE;
		}

		/**
         * Visits a file and performs an operation on it.
         * 
         * @param file The path of the file to be visited.
         * @param attrs The basic file attributes of the file.
         * @return The result of the file visit.
         * @throws IOException If an I/O error occurs while visiting the file.
         */
        @Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			this.layout.file(relocate(file), Owner.ROOT, getMode(file), Content.of(file.toFile()));
			return FileVisitResult.CONTINUE;
		}

		/**
         * Returns the mode of the specified file or directory.
         * 
         * @param path the path of the file or directory
         * @return the mode of the file or directory
         * @throws IOException if an I/O error occurs
         * @throws IllegalStateException if buildpack content in a directory is not supported on this operating system
         */
        private int getMode(Path path) throws IOException {
			try {
				return FilePermissions.umaskForPath(path);
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException(
						"Buildpack content in a directory is not supported on this operating system");
			}
		}

		/**
         * Relocates a given path by appending it to the layer path.
         * 
         * @param path the path to be relocated
         * @return the relocated path as a string
         */
        private String relocate(Path path) {
			Path node = path.subpath(this.basePath.getNameCount(), path.getNameCount());
			return Paths.get(this.layerPath.toString(), node.toString()).toString();
		}

	}

}
