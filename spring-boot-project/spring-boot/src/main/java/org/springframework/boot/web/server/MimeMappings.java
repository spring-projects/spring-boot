/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;

/**
 * Simple server-independent abstraction for mime mappings. Roughly equivalent to the
 * {@literal &lt;mime-mapping&gt;} element traditionally found in web.xml.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class MimeMappings implements Iterable<MimeMappings.Mapping> {

	/**
	 * Default mime mapping commonly used.
	 */
	public static final MimeMappings DEFAULT;

	static {
		MimeMappings mappings = new MimeMappings();
		try {
			Properties defaultMimeMappings = PropertiesLoaderUtils
					.loadProperties(new ClassPathResource("mime-mappings.properties", MimeMappings.class));
			for (String extension : defaultMimeMappings.stringPropertyNames()) {
				mappings.add(extension, defaultMimeMappings.getProperty(extension));
			}
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load the default MIME types", ex);
		}
		DEFAULT = unmodifiableMappings(mappings);
	}

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
	private MimeMappings(MimeMappings mappings, boolean mutable) {
		Assert.notNull(mappings, "Mappings must not be null");
		this.map = (mutable ? new LinkedHashMap<>(mappings.map) : Collections.unmodifiableMap(mappings.map));
	}

	@Override
	public Iterator<Mapping> iterator() {
		return getAll().iterator();
	}

	/**
	 * Returns all defined mappings.
	 * @return the mappings.
	 */
	public Collection<Mapping> getAll() {
		return this.map.values();
	}

	/**
	 * Add a new mime mapping.
	 * @param extension the file extension (excluding '.')
	 * @param mimeType the mime type to map
	 * @return any previous mapping or {@code null}
	 */
	public String add(String extension, String mimeType) {
		Mapping previous = this.map.put(extension, new Mapping(extension, mimeType));
		return (previous != null) ? previous.getMimeType() : null;
	}

	/**
	 * Get a mime mapping for the given extension.
	 * @param extension the file extension (excluding '.')
	 * @return a mime mapping or {@code null}
	 */
	public String get(String extension) {
		Mapping mapping = this.map.get(extension);
		return (mapping != null) ? mapping.getMimeType() : null;
	}

	/**
	 * Remove an existing mapping.
	 * @param extension the file extension (excluding '.')
	 * @return the removed mime mapping or {@code null} if no item was removed
	 */
	public String remove(String extension) {
		Mapping previous = this.map.remove(extension);
		return (previous != null) ? previous.getMimeType() : null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof MimeMappings other) {
			return this.map.equals(other.map);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.map.hashCode();
	}

	/**
	 * Create a new unmodifiable view of the specified mapping. Methods that attempt to
	 * modify the returned map will throw {@link UnsupportedOperationException}s.
	 * @param mappings the mappings
	 * @return an unmodifiable view of the specified mappings.
	 */
	public static MimeMappings unmodifiableMappings(MimeMappings mappings) {
		return new MimeMappings(mappings, false);
	}

	/**
	 * A single mime mapping.
	 */
	public static final class Mapping {

		private final String extension;

		private final String mimeType;

		public Mapping(String extension, String mimeType) {
			Assert.notNull(extension, "Extension must not be null");
			Assert.notNull(mimeType, "MimeType must not be null");
			this.extension = extension;
			this.mimeType = mimeType;
		}

		public String getExtension() {
			return this.extension;
		}

		public String getMimeType() {
			return this.mimeType;
		}

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

		@Override
		public int hashCode() {
			return this.extension.hashCode();
		}

		@Override
		public String toString() {
			return "Mapping [extension=" + this.extension + ", mimeType=" + this.mimeType + "]";
		}

	}

}
