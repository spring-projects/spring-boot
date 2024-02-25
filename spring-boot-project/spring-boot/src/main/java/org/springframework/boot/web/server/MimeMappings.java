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

package org.springframework.boot.web.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;

/**
 * Simple server-independent abstraction for mime mappings. Roughly equivalent to the
 * {@literal &lt;mime-mapping&gt;} element traditionally found in web.xml.
 *
 * @author Phillip Webb
 * @author Guirong Hu
 * @since 2.0.0
 */
public sealed class MimeMappings implements Iterable<MimeMappings.Mapping> {

	/**
	 * Default mime mapping commonly used.
	 */
	public static final MimeMappings DEFAULT = new DefaultMimeMappings();

	private final Map<String, Mapping> map;

	/**
	 * Create a new empty {@link MimeMappings} instance.
	 */
	public MimeMappings() {
		this.map = new LinkedHashMap<>();
	}

	/**
	 * Create a new {@link MimeMappings} instance from the specified mappings.
	 * @param mappings the source mappings
	 */
	public MimeMappings(MimeMappings mappings) {
		this(mappings, true);
	}

	/**
	 * Create a new {@link MimeMappings} from the specified mappings.
	 * @param mappings the source mappings with extension as the key and mime-type as the
	 * value
	 */
	public MimeMappings(Map<String, String> mappings) {
		Assert.notNull(mappings, "Mappings must not be null");
		this.map = new LinkedHashMap<>();
		mappings.forEach(this::add);
	}

	/**
	 * Internal constructor.
	 * @param mappings source mappings
	 * @param mutable if the new object should be mutable.
	 */
	MimeMappings(MimeMappings mappings, boolean mutable) {
		Assert.notNull(mappings, "Mappings must not be null");
		this.map = (mutable ? new LinkedHashMap<>(mappings.map) : Collections.unmodifiableMap(mappings.map));
	}

	/**
	 * Add a new mime mapping.
	 * @param extension the file extension (excluding '.')
	 * @param mimeType the mime type to map
	 * @return any previous mapping or {@code null}
	 */
	public String add(String extension, String mimeType) {
		Assert.notNull(extension, "Extension must not be null");
		Assert.notNull(mimeType, "MimeType must not be null");
		Mapping previous = this.map.put(extension.toLowerCase(Locale.ENGLISH), new Mapping(extension, mimeType));
		return (previous != null) ? previous.getMimeType() : null;
	}

	/**
	 * Remove an existing mapping.
	 * @param extension the file extension (excluding '.')
	 * @return the removed mime mapping or {@code null} if no item was removed
	 */
	public String remove(String extension) {
		Assert.notNull(extension, "Extension must not be null");
		Mapping previous = this.map.remove(extension.toLowerCase(Locale.ENGLISH));
		return (previous != null) ? previous.getMimeType() : null;
	}

	/**
	 * Get a mime mapping for the given extension.
	 * @param extension the file extension (excluding '.')
	 * @return a mime mapping or {@code null}
	 */
	public String get(String extension) {
		Assert.notNull(extension, "Extension must not be null");
		Mapping mapping = this.map.get(extension.toLowerCase(Locale.ENGLISH));
		return (mapping != null) ? mapping.getMimeType() : null;
	}

	/**
	 * Returns all defined mappings.
	 * @return the mappings.
	 */
	public Collection<Mapping> getAll() {
		return this.map.values();
	}

	/**
	 * Returns an iterator over the elements in this MimeMappings collection.
	 * @return an iterator over the elements in this MimeMappings collection
	 */
	@Override
	public final Iterator<Mapping> iterator() {
		return getAll().iterator();
	}

	/**
	 * Compares this MimeMappings object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this MimeMappings object, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof MimeMappings other) {
			return getMap().equals(other.map);
		}
		return false;
	}

	/**
	 * Returns the hash code value for the MimeMappings object.
	 * @return the hash code value for the MimeMappings object
	 */
	@Override
	public int hashCode() {
		return getMap().hashCode();
	}

	/**
	 * Returns the map of string to mapping objects.
	 * @return the map of string to mapping objects
	 */
	Map<String, Mapping> getMap() {
		return this.map;
	}

	/**
	 * Create a new unmodifiable view of the specified mapping. Methods that attempt to
	 * modify the returned map will throw {@link UnsupportedOperationException}s.
	 * @param mappings the mappings
	 * @return an unmodifiable view of the specified mappings.
	 */
	public static MimeMappings unmodifiableMappings(MimeMappings mappings) {
		Assert.notNull(mappings, "Mappings must not be null");
		return new MimeMappings(mappings, false);
	}

	/**
	 * Create a new lazy copy of the given mappings that will only copy entries if the
	 * mappings are mutated.
	 * @param mappings the source mappings
	 * @return a new mappings instance
	 * @since 3.0.0
	 */
	public static MimeMappings lazyCopy(MimeMappings mappings) {
		Assert.notNull(mappings, "Mappings must not be null");
		return new LazyMimeMappingsCopy(mappings);
	}

	/**
	 * A single mime mapping.
	 */
	public static final class Mapping {

		private final String extension;

		private final String mimeType;

		/**
		 * Constructs a new Mapping object with the specified extension and mimeType.
		 * @param extension the file extension associated with the mapping (must not be
		 * null)
		 * @param mimeType the MIME type associated with the mapping (must not be null)
		 * @throws IllegalArgumentException if either extension or mimeType is null
		 */
		public Mapping(String extension, String mimeType) {
			Assert.notNull(extension, "Extension must not be null");
			Assert.notNull(mimeType, "MimeType must not be null");
			this.extension = extension;
			this.mimeType = mimeType;
		}

		/**
		 * Returns the extension of the mapping.
		 * @return the extension of the mapping
		 */
		public String getExtension() {
			return this.extension;
		}

		/**
		 * Returns the MIME type of the mapping.
		 * @return the MIME type of the mapping
		 */
		public String getMimeType() {
			return this.mimeType;
		}

		/**
		 * Compares this Mapping object to the specified object for equality.
		 * @param obj the object to compare to
		 * @return true if the specified object is equal to this Mapping object, false
		 * otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj instanceof Mapping other) {
				return this.extension.equals(other.extension) && this.mimeType.equals(other.mimeType);
			}
			return false;
		}

		/**
		 * Returns the hash code value for this Mapping object.
		 * @return the hash code value for this Mapping object
		 */
		@Override
		public int hashCode() {
			return this.extension.hashCode();
		}

		/**
		 * Returns a string representation of the Mapping object.
		 * @return a string representation of the Mapping object
		 */
		@Override
		public String toString() {
			return "Mapping [extension=" + this.extension + ", mimeType=" + this.mimeType + "]";
		}

	}

	/**
	 * {@link MimeMappings} implementation used for {@link MimeMappings#DEFAULT}. Provides
	 * in-memory access for common mappings and lazily loads the complete set when
	 * necessary.
	 */
	static final class DefaultMimeMappings extends MimeMappings {

		static final String MIME_MAPPINGS_PROPERTIES = "mime-mappings.properties";

		private static final MimeMappings COMMON;

		static {
			MimeMappings mappings = new MimeMappings();
			mappings.add("avi", "video/x-msvideo");
			mappings.add("bin", "application/octet-stream");
			mappings.add("body", "text/html");
			mappings.add("class", "application/java");
			mappings.add("css", "text/css");
			mappings.add("dtd", "application/xml-dtd");
			mappings.add("gif", "image/gif");
			mappings.add("gtar", "application/x-gtar");
			mappings.add("gz", "application/x-gzip");
			mappings.add("htm", "text/html");
			mappings.add("html", "text/html");
			mappings.add("jar", "application/java-archive");
			mappings.add("java", "text/x-java-source");
			mappings.add("jnlp", "application/x-java-jnlp-file");
			mappings.add("jpe", "image/jpeg");
			mappings.add("jpeg", "image/jpeg");
			mappings.add("jpg", "image/jpeg");
			mappings.add("js", "text/javascript");
			mappings.add("json", "application/json");
			mappings.add("otf", "font/otf");
			mappings.add("pdf", "application/pdf");
			mappings.add("png", "image/png");
			mappings.add("ps", "application/postscript");
			mappings.add("tar", "application/x-tar");
			mappings.add("tif", "image/tiff");
			mappings.add("tiff", "image/tiff");
			mappings.add("ttf", "font/ttf");
			mappings.add("txt", "text/plain");
			mappings.add("xht", "application/xhtml+xml");
			mappings.add("xhtml", "application/xhtml+xml");
			mappings.add("xls", "application/vnd.ms-excel");
			mappings.add("xml", "application/xml");
			mappings.add("xsl", "application/xml");
			mappings.add("xslt", "application/xslt+xml");
			mappings.add("wasm", "application/wasm");
			mappings.add("zip", "application/zip");
			COMMON = unmodifiableMappings(mappings);
		}

		private volatile Map<String, Mapping> loaded;

		/**
		 * Constructs a new instance of DefaultMimeMappings with a default MimeMappings
		 * object and sets the boolean value to false.
		 */
		DefaultMimeMappings() {
			super(new MimeMappings(), false);
		}

		/**
		 * Retrieves all the mappings from the loaded mime mappings.
		 * @return a collection of all the mappings
		 */
		@Override
		public Collection<Mapping> getAll() {
			return load().values();
		}

		/**
		 * Retrieves the MIME type associated with the given file extension.
		 * @param extension the file extension (e.g. "txt", "jpg")
		 * @return the corresponding MIME type, or null if not found
		 * @throws IllegalArgumentException if the extension is null
		 */
		@Override
		public String get(String extension) {
			Assert.notNull(extension, "Extension must not be null");
			extension = extension.toLowerCase(Locale.ENGLISH);
			Map<String, Mapping> loaded = this.loaded;
			if (loaded != null) {
				return get(loaded, extension);
			}
			String commonMimeType = COMMON.get(extension);
			if (commonMimeType != null) {
				return commonMimeType;
			}
			loaded = load();
			return get(loaded, extension);
		}

		/**
		 * Retrieves the MIME type associated with the given file extension from the
		 * provided mappings.
		 * @param mappings the map containing the file extension to MIME type mappings
		 * @param extension the file extension for which the MIME type is to be retrieved
		 * @return the MIME type associated with the given file extension, or null if no
		 * mapping is found
		 */
		private String get(Map<String, Mapping> mappings, String extension) {
			Mapping mapping = mappings.get(extension);
			return (mapping != null) ? mapping.getMimeType() : null;
		}

		/**
		 * Retrieves the map of mime mappings.
		 * @return the map of mime mappings
		 */
		@Override
		Map<String, Mapping> getMap() {
			return load();
		}

		/**
		 * Loads the MIME mappings from the properties file.
		 * @return a map containing the loaded MIME mappings
		 * @throws IllegalArgumentException if unable to load the default MIME types
		 */
		private Map<String, Mapping> load() {
			Map<String, Mapping> loaded = this.loaded;
			if (loaded != null) {
				return loaded;
			}
			try {
				loaded = new LinkedHashMap<>();
				for (Entry<?, ?> entry : PropertiesLoaderUtils
					.loadProperties(new ClassPathResource(MIME_MAPPINGS_PROPERTIES, getClass()))
					.entrySet()) {
					loaded.put((String) entry.getKey(),
							new Mapping((String) entry.getKey(), (String) entry.getValue()));
				}
				loaded = Collections.unmodifiableMap(loaded);
				this.loaded = loaded;
				return loaded;
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Unable to load the default MIME types", ex);
			}
		}

	}

	/**
	 * {@link MimeMappings} implementation used to create a lazy copy only when the
	 * mappings are mutated.
	 */
	static final class LazyMimeMappingsCopy extends MimeMappings {

		private final MimeMappings source;

		private final AtomicBoolean copied = new AtomicBoolean();

		/**
		 * Creates a new instance of the LazyMimeMappingsCopy class with the specified
		 * MimeMappings source.
		 * @param source the MimeMappings object to be used as the source for copying
		 */
		LazyMimeMappingsCopy(MimeMappings source) {
			this.source = source;
		}

		/**
		 * Adds a new mapping for a file extension and its corresponding MIME type. If
		 * necessary, copies the mappings from the parent class before adding the new
		 * mapping.
		 * @param extension the file extension to be mapped
		 * @param mimeType the MIME type to be associated with the file extension
		 * @return the updated mappings after adding the new mapping
		 */
		@Override
		public String add(String extension, String mimeType) {
			copyIfNecessary();
			return super.add(extension, mimeType);
		}

		/**
		 * Removes the specified extension from the lazy MIME mappings copy.
		 * @param extension the extension to be removed
		 * @return the removed extension
		 */
		@Override
		public String remove(String extension) {
			copyIfNecessary();
			return super.remove(extension);
		}

		/**
		 * Copies the mime mappings from the source to the current instance if necessary.
		 * <p>
		 * This method checks if the mime mappings have already been copied. If not, it
		 * atomically sets the copied flag to true and proceeds to copy the mime mappings
		 * from the source to the current instance.
		 * </p>
		 * <p>
		 * The copying is done by iterating over each mapping in the source and adding it
		 * to the current instance using the extension as the key and the mime type as the
		 * value.
		 * </p>
		 */
		private void copyIfNecessary() {
			if (this.copied.compareAndSet(false, true)) {
				this.source.forEach((mapping) -> add(mapping.getExtension(), mapping.getMimeType()));
			}
		}

		/**
		 * Retrieves the MIME mapping for the specified file extension.
		 * @param extension the file extension for which the MIME mapping is to be
		 * retrieved
		 * @return the MIME mapping for the specified file extension
		 */
		@Override
		public String get(String extension) {
			return !this.copied.get() ? this.source.get(extension) : super.get(extension);
		}

		/**
		 * Returns a collection of all the mappings. If the mappings have not been copied,
		 * it returns the mappings from the source. If the mappings have been copied, it
		 * returns the mappings from the superclass.
		 * @return a collection of all the mappings
		 */
		@Override
		public Collection<Mapping> getAll() {
			return !this.copied.get() ? this.source.getAll() : super.getAll();
		}

		/**
		 * Returns the map of mime mappings.
		 * @return the map of mime mappings
		 */
		@Override
		Map<String, Mapping> getMap() {
			return !this.copied.get() ? this.source.getMap() : super.getMap();
		}

	}

	/**
	 * MimeMappingsRuntimeHints class.
	 */
	static class MimeMappingsRuntimeHints implements RuntimeHintsRegistrar {

		/**
		 * Registers hints for runtime mime mappings.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to use for loading resources
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources()
				.registerPattern("org/springframework/boot/web/server/" + DefaultMimeMappings.MIME_MAPPINGS_PROPERTIES);
		}

	}

}
