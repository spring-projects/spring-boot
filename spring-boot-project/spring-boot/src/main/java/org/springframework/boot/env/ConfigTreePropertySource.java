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

package org.springframework.boot.env;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} backed by a directory tree that contains files for each value.
 * The {@link PropertySource} will recursively scan a given source directory and expose a
 * property for each file found. The property name will be the filename, and the property
 * value will be the contents of the file.
 * <p>
 * Directories are only scanned when the source is first created. The directory is not
 * monitored for updates, so files should not be added or removed. However, the contents
 * of a file can be updated as long as the property source was created with a
 * {@link Option#ALWAYS_READ} option. Nested directories are included in the source, but
 * with a {@code '.'} rather than {@code '/'} used as the path separator.
 * <p>
 * Property values are returned as {@link Value} instances which allows them to be treated
 * either as an {@link InputStreamSource} or as a {@link CharSequence}. In addition, if
 * used with an {@link Environment} configured with an
 * {@link ApplicationConversionService}, property values can be converted to a
 * {@code String} or {@code byte[]}.
 * <p>
 * This property source is typically used to read Kubernetes {@code configMap} volume
 * mounts.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class ConfigTreePropertySource extends EnumerablePropertySource<Path> implements OriginLookup<String> {

	private static final int MAX_DEPTH = 100;

	private final Map<String, PropertyFile> propertyFiles;

	private final String[] names;

	private final Set<Option> options;

	/**
	 * Create a new {@link ConfigTreePropertySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 */
	public ConfigTreePropertySource(String name, Path sourceDirectory) {
		this(name, sourceDirectory, EnumSet.noneOf(Option.class));
	}

	/**
	 * Create a new {@link ConfigTreePropertySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 * @param options the property source options
	 */
	public ConfigTreePropertySource(String name, Path sourceDirectory, Option... options) {
		this(name, sourceDirectory, EnumSet.copyOf(Arrays.asList(options)));
	}

	private ConfigTreePropertySource(String name, Path sourceDirectory, Set<Option> options) {
		super(name, sourceDirectory);
		Assert.isTrue(Files.exists(sourceDirectory), () -> "Directory '" + sourceDirectory + "' does not exist");
		Assert.isTrue(Files.isDirectory(sourceDirectory), () -> "File '" + sourceDirectory + "' is not a directory");
		this.propertyFiles = PropertyFile.findAll(sourceDirectory, options);
		this.options = options;
		this.names = StringUtils.toStringArray(this.propertyFiles.keySet());
	}

	@Override
	public String[] getPropertyNames() {
		return this.names.clone();
	}

	@Override
	public Value getProperty(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getContent() : null;
	}

	@Override
	public Origin getOrigin(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getOrigin() : null;
	}

	@Override
	public boolean isImmutable() {
		return !this.options.contains(Option.ALWAYS_READ);
	}

	/**
	 * Property source options.
	 */
	public enum Option {

		/**
		 * Always read the value of the file when accessing the property value. When this
		 * option is not set the property source will cache the value when it's first
		 * read.
		 */
		ALWAYS_READ,

		/**
		 * Convert file and directory names to lowercase.
		 */
		USE_LOWERCASE_NAMES

	}

	/**
	 * A value returned from the property source which exposes the contents of the
	 * property file. Values can either be treated as {@link CharSequence} or as an
	 * {@link InputStreamSource}.
	 */
	public interface Value extends CharSequence, InputStreamSource {

	}

	/**
	 * A single property file that was found when when the source was created.
	 */
	private static final class PropertyFile {

		private static final Location START_OF_FILE = new Location(0, 0);

		private final Path path;

		private final PathResource resource;

		private final Origin origin;

		private final PropertyFileContent cachedContent;

		private PropertyFile(Path path, Set<Option> options) {
			this.path = path;
			this.resource = new PathResource(path);
			this.origin = new TextResourceOrigin(this.resource, START_OF_FILE);
			this.cachedContent = options.contains(Option.ALWAYS_READ) ? null
					: new PropertyFileContent(path, this.resource, this.origin, true);
		}

		PropertyFileContent getContent() {
			return (this.cachedContent != null) ? this.cachedContent
					: new PropertyFileContent(this.path, this.resource, this.origin, false);
		}

		Origin getOrigin() {
			return this.origin;
		}

		static Map<String, PropertyFile> findAll(Path sourceDirectory, Set<Option> options) {
			try {
				Map<String, PropertyFile> propertyFiles = new TreeMap<>();
				Files.find(sourceDirectory, MAX_DEPTH, PropertyFile::isRegularFile).forEach((path) -> {
					String name = getName(sourceDirectory.relativize(path));
					if (StringUtils.hasText(name)) {
						if (options.contains(Option.USE_LOWERCASE_NAMES)) {
							name = name.toLowerCase();
						}
						propertyFiles.put(name, new PropertyFile(path, options));
					}
				});
				return Collections.unmodifiableMap(propertyFiles);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to find files in '" + sourceDirectory + "'", ex);
			}
		}

		private static boolean isRegularFile(Path path, BasicFileAttributes attributes) {
			return attributes.isRegularFile();
		}

		private static String getName(Path relativePath) {
			int nameCount = relativePath.getNameCount();
			if (nameCount == 1) {
				return relativePath.toString();
			}
			StringBuilder name = new StringBuilder();
			for (int i = 0; i < nameCount; i++) {
				name.append((i != 0) ? "." : "");
				name.append(relativePath.getName(i));
			}
			return name.toString();
		}

	}

	/**
	 * The contents of a found property file.
	 */
	private static final class PropertyFileContent implements Value, OriginProvider {

		private final Path path;

		private final Resource resource;

		private final boolean cacheContent;

		private volatile byte[] content;

		private final Origin origin;

		private PropertyFileContent(Path path, Resource resource, Origin origin, boolean cacheContent) {
			this.path = path;
			this.resource = resource;
			this.origin = origin;
			this.cacheContent = cacheContent;
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

		@Override
		public int length() {
			return toString().length();
		}

		@Override
		public char charAt(int index) {
			return toString().charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return toString().subSequence(start, end);
		}

		@Override
		public String toString() {
			return new String(getBytes());
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (!this.cacheContent) {
				assertStillExists();
				return this.resource.getInputStream();
			}
			return new ByteArrayInputStream(getBytes());
		}

		private byte[] getBytes() {
			try {
				if (!this.cacheContent) {
					assertStillExists();
					return FileCopyUtils.copyToByteArray(this.resource.getInputStream());
				}
				if (this.content == null) {
					assertStillExists();
					synchronized (this.resource) {
						if (this.content == null) {
							this.content = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
						}
					}
				}
				return this.content;
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private void assertStillExists() {
			Assert.state(Files.exists(this.path), () -> "The property file '" + this.path + "' no longer exists");
		}

	}

}
