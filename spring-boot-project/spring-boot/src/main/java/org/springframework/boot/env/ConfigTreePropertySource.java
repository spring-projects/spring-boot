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

package org.springframework.boot.env;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

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

	/**
	 * Constructs a new ConfigTreePropertySource with the given name, source directory,
	 * and options.
	 * @param name the name of the property source
	 * @param sourceDirectory the path to the source directory
	 * @param options the set of options to be applied
	 * @throws IllegalArgumentException if the source directory does not exist or is not a
	 * directory
	 */
	private ConfigTreePropertySource(String name, Path sourceDirectory, Set<Option> options) {
		super(name, sourceDirectory);
		Assert.isTrue(Files.exists(sourceDirectory), () -> "Directory '" + sourceDirectory + "' does not exist");
		Assert.isTrue(Files.isDirectory(sourceDirectory), () -> "File '" + sourceDirectory + "' is not a directory");
		this.propertyFiles = PropertyFile.findAll(sourceDirectory, options);
		this.options = options;
		this.names = StringUtils.toStringArray(this.propertyFiles.keySet());
	}

	/**
	 * Returns an array of property names.
	 * @return the array of property names
	 */
	@Override
	public String[] getPropertyNames() {
		return this.names.clone();
	}

	/**
	 * Retrieves the value of a property with the specified name.
	 * @param name the name of the property to retrieve
	 * @return the value of the property, or null if the property does not exist
	 */
	@Override
	public Value getProperty(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getContent() : null;
	}

	/**
	 * Retrieves the origin of a property file with the given name.
	 * @param name the name of the property file
	 * @return the origin of the property file, or null if the property file does not
	 * exist
	 */
	@Override
	public Origin getOrigin(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getOrigin() : null;
	}

	/**
	 * Returns a boolean value indicating whether the ConfigTreePropertySource is
	 * immutable.
	 * @return {@code true} if the ConfigTreePropertySource is immutable, {@code false}
	 * otherwise.
	 */
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
		USE_LOWERCASE_NAMES,

		/**
		 * Automatically attempt trim trailing new-line characters.
		 */
		AUTO_TRIM_TRAILING_NEW_LINE

	}

	/**
	 * A value returned from the property source which exposes the contents of the
	 * property file. Values can either be treated as {@link CharSequence} or as an
	 * {@link InputStreamSource}.
	 */
	public interface Value extends CharSequence, InputStreamSource {

	}

	/**
	 * A single property file that was found when the source was created.
	 */
	private static final class PropertyFile {

		private static final Location START_OF_FILE = new Location(0, 0);

		private final Path path;

		private final PathResource resource;

		private final Origin origin;

		private final PropertyFileContent cachedContent;

		private final boolean autoTrimTrailingNewLine;

		/**
		 * Constructs a new PropertyFile object with the given path and options.
		 * @param path the path of the property file
		 * @param options the set of options for the property file
		 */
		private PropertyFile(Path path, Set<Option> options) {
			this.path = path;
			this.resource = new PathResource(path);
			this.origin = new TextResourceOrigin(this.resource, START_OF_FILE);
			this.autoTrimTrailingNewLine = options.contains(Option.AUTO_TRIM_TRAILING_NEW_LINE);
			this.cachedContent = options.contains(Option.ALWAYS_READ) ? null
					: new PropertyFileContent(path, this.resource, this.origin, true, this.autoTrimTrailingNewLine);
		}

		/**
		 * Retrieves the content of the property file.
		 * @return The content of the property file as a PropertyFileContent object. If
		 * the content has been previously cached, the cached content is returned.
		 * Otherwise, a new PropertyFileContent object is created and returned. The new
		 * object is initialized with the path, resource, origin, autoTrimTrailingNewLine,
		 * and false values.
		 */
		PropertyFileContent getContent() {
			if (this.cachedContent != null) {
				return this.cachedContent;
			}
			return new PropertyFileContent(this.path, this.resource, this.origin, false, this.autoTrimTrailingNewLine);
		}

		/**
		 * Returns the origin of the PropertyFile.
		 * @return the origin of the PropertyFile
		 */
		Origin getOrigin() {
			return this.origin;
		}

		/**
		 * Finds all property files in the specified source directory.
		 * @param sourceDirectory the directory to search for property files
		 * @param options a set of options to customize the search behavior
		 * @return a map of property file names to PropertyFile objects
		 * @throws IllegalStateException if unable to find files in the source directory
		 */
		static Map<String, PropertyFile> findAll(Path sourceDirectory, Set<Option> options) {
			try {
				Map<String, PropertyFile> propertyFiles = new TreeMap<>();
				try (Stream<Path> pathStream = Files.find(sourceDirectory, MAX_DEPTH, PropertyFile::isPropertyFile,
						FileVisitOption.FOLLOW_LINKS)) {
					pathStream.forEach((path) -> {
						String name = getName(sourceDirectory.relativize(path));
						if (StringUtils.hasText(name)) {
							if (options.contains(Option.USE_LOWERCASE_NAMES)) {
								name = name.toLowerCase();
							}
							propertyFiles.put(name, new PropertyFile(path, options));
						}
					});
				}
				return Collections.unmodifiableMap(propertyFiles);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to find files in '" + sourceDirectory + "'", ex);
			}
		}

		/**
		 * Checks if the given path is a property file.
		 * @param path the path to check
		 * @param attributes the attributes of the file
		 * @return true if the path is a property file, false otherwise
		 */
		private static boolean isPropertyFile(Path path, BasicFileAttributes attributes) {
			return !hasHiddenPathElement(path) && (attributes.isRegularFile() || attributes.isSymbolicLink());
		}

		/**
		 * Checks if the given path contains a hidden path element.
		 * @param path the path to be checked
		 * @return true if the path contains a hidden path element, false otherwise
		 */
		private static boolean hasHiddenPathElement(Path path) {
			for (Path element : path) {
				if (element.toString().startsWith("..")) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Returns the name of the file or directory represented by the given relative
		 * path.
		 * @param relativePath the relative path to the file or directory
		 * @return the name of the file or directory
		 */
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

		private final Lock resourceLock = new ReentrantLock();

		private final Resource resource;

		private final Origin origin;

		private final boolean cacheContent;

		private final boolean autoTrimTrailingNewLine;

		private volatile byte[] content;

		/**
		 * Constructs a new PropertyFileContent object with the specified parameters.
		 * @param path the path of the property file
		 * @param resource the resource associated with the property file
		 * @param origin the origin of the property file
		 * @param cacheContent true if the content should be cached, false otherwise
		 * @param autoTrimTrailingNewLine true if trailing new lines should be
		 * automatically trimmed, false otherwise
		 */
		private PropertyFileContent(Path path, Resource resource, Origin origin, boolean cacheContent,
				boolean autoTrimTrailingNewLine) {
			this.path = path;
			this.resource = resource;
			this.origin = origin;
			this.cacheContent = cacheContent;
			this.autoTrimTrailingNewLine = autoTrimTrailingNewLine;
		}

		/**
		 * Returns the origin of the PropertyFileContent.
		 * @return the origin of the PropertyFileContent
		 */
		@Override
		public Origin getOrigin() {
			return this.origin;
		}

		/**
		 * Returns the length of the string representation of the PropertyFileContent
		 * object.
		 * @return the length of the string representation
		 */
		@Override
		public int length() {
			return toString().length();
		}

		/**
		 * Returns the character at the specified index in the content of the property
		 * file.
		 * @param index the index of the character to be returned
		 * @return the character at the specified index
		 * @throws IndexOutOfBoundsException if the index is out of range (index < 0 ||
		 * index >= length())
		 */
		@Override
		public char charAt(int index) {
			return toString().charAt(index);
		}

		/**
		 * Returns a new CharSequence that is a subsequence of this sequence. The
		 * subsequence starts with the character at the specified index and ends with the
		 * character at index end - 1. The length of the returned sequence is end - start.
		 * @param start the start index, inclusive
		 * @param end the end index, exclusive
		 * @return the specified subsequence
		 * @throws IndexOutOfBoundsException if start or end are negative, if end is
		 * greater than length(), or if start is greater than end
		 */
		@Override
		public CharSequence subSequence(int start, int end) {
			return toString().subSequence(start, end);
		}

		/**
		 * Returns a string representation of the PropertyFileContent object.
		 * @return the string representation of the PropertyFileContent object
		 */
		@Override
		public String toString() {
			String string = new String(getBytes());
			if (this.autoTrimTrailingNewLine) {
				string = autoTrimTrailingNewLine(string);
			}
			return string;
		}

		/**
		 * Removes trailing new line characters from the given string.
		 * @param string the string to be processed
		 * @return the string without trailing new line characters
		 */
		private String autoTrimTrailingNewLine(String string) {
			if (!string.endsWith("\n")) {
				return string;
			}
			int numberOfLines = 0;
			for (int i = 0; i < string.length(); i++) {
				char ch = string.charAt(i);
				if (ch == '\n') {
					numberOfLines++;
				}
			}
			if (numberOfLines > 1) {
				return string;
			}
			return (string.endsWith("\r\n")) ? string.substring(0, string.length() - 2)
					: string.substring(0, string.length() - 1);
		}

		/**
		 * Returns an input stream for reading the content of the property file.
		 * @return an input stream for reading the content of the property file
		 * @throws IOException if an I/O error occurs
		 */
		@Override
		public InputStream getInputStream() throws IOException {
			if (!this.cacheContent) {
				assertStillExists();
				return this.resource.getInputStream();
			}
			return new ByteArrayInputStream(getBytes());
		}

		/**
		 * Retrieves the content of the property file as a byte array.
		 * @return the content of the property file as a byte array
		 * @throws IllegalStateException if an I/O error occurs
		 */
		private byte[] getBytes() {
			try {
				if (!this.cacheContent) {
					assertStillExists();
					return FileCopyUtils.copyToByteArray(this.resource.getInputStream());
				}
				if (this.content == null) {
					assertStillExists();
					this.resourceLock.lock();
					try {
						if (this.content == null) {
							this.content = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
						}
					}
					finally {
						this.resourceLock.unlock();
					}
				}
				return this.content;
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Asserts that the property file still exists.
		 * @throws IllegalStateException if the property file no longer exists
		 */
		private void assertStillExists() {
			Assert.state(Files.exists(this.path), () -> "The property file '" + this.path + "' no longer exists");
		}

	}

}
