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

package org.springframework.boot.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Load a {@link ConfigurationMetadataRepository} from the content of arbitrary
 * resource(s).
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public final class ConfigurationMetadataRepositoryJsonBuilder {

	private Charset defaultCharset = StandardCharsets.UTF_8;

	private final JsonReader reader = new JsonReader();

	private final List<SimpleConfigurationMetadataRepository> repositories = new ArrayList<>();

	/**
     * Constructs a new ConfigurationMetadataRepositoryJsonBuilder with the specified default charset.
     *
     * @param defaultCharset the default charset to be used
     */
    private ConfigurationMetadataRepositoryJsonBuilder(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Add the content of a {@link ConfigurationMetadataRepository} defined by the
	 * specified {@link InputStream} json document using the default charset. If this
	 * metadata repository holds items that were loaded previously, these are ignored.
	 * <p>
	 * Leaves the stream open when done.
	 * @param inputStream the source input stream
	 * @return this builder
	 * @throws IOException in case of I/O errors
	 */
	public ConfigurationMetadataRepositoryJsonBuilder withJsonResource(InputStream inputStream) throws IOException {
		return withJsonResource(inputStream, this.defaultCharset);
	}

	/**
	 * Add the content of a {@link ConfigurationMetadataRepository} defined by the
	 * specified {@link InputStream} json document using the specified {@link Charset}. If
	 * this metadata repository holds items that were loaded previously, these are
	 * ignored.
	 * <p>
	 * Leaves the stream open when done.
	 * @param inputStream the source input stream
	 * @param charset the charset of the input
	 * @return this builder
	 * @throws IOException in case of I/O errors
	 */
	public ConfigurationMetadataRepositoryJsonBuilder withJsonResource(InputStream inputStream, Charset charset)
			throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream must not be null.");
		}
		this.repositories.add(add(inputStream, charset));
		return this;
	}

	/**
	 * Build a {@link ConfigurationMetadataRepository} with the current state of this
	 * builder.
	 * @return this builder
	 */
	public ConfigurationMetadataRepository build() {
		SimpleConfigurationMetadataRepository result = new SimpleConfigurationMetadataRepository();
		for (SimpleConfigurationMetadataRepository repository : this.repositories) {
			result.include(repository);
		}
		return result;
	}

	/**
     * Adds a new configuration metadata repository by reading the metadata from the provided input stream.
     * 
     * @param in the input stream containing the configuration metadata
     * @param charset the character set used to read the input stream
     * @return the newly created SimpleConfigurationMetadataRepository
     * @throws IllegalStateException if failed to read the configuration metadata
     */
    private SimpleConfigurationMetadataRepository add(InputStream in, Charset charset) {
		try {
			RawConfigurationMetadata metadata = this.reader.read(in, charset);
			return create(metadata);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to read configuration metadata", ex);
		}
	}

	/**
     * Creates a new SimpleConfigurationMetadataRepository object based on the provided RawConfigurationMetadata.
     * 
     * @param metadata The RawConfigurationMetadata object containing the metadata information.
     * @return A new SimpleConfigurationMetadataRepository object.
     */
    private SimpleConfigurationMetadataRepository create(RawConfigurationMetadata metadata) {
		SimpleConfigurationMetadataRepository repository = new SimpleConfigurationMetadataRepository();
		repository.add(metadata.getSources());
		for (ConfigurationMetadataItem item : metadata.getItems()) {
			ConfigurationMetadataSource source = metadata.getSource(item);
			repository.add(item, source);
		}
		Map<String, ConfigurationMetadataProperty> allProperties = repository.getAllProperties();
		for (ConfigurationMetadataHint hint : metadata.getHints()) {
			ConfigurationMetadataProperty property = allProperties.get(hint.getId());
			if (property != null) {
				addValueHints(property, hint);
			}
			else {
				String id = hint.resolveId();
				property = allProperties.get(id);
				if (property != null) {
					if (hint.isMapKeyHints()) {
						addMapHints(property, hint);
					}
					else {
						addValueHints(property, hint);
					}
				}
			}
		}
		return repository;
	}

	/**
     * Adds value hints to the given property based on the provided hint.
     * 
     * @param property the ConfigurationMetadataProperty to add value hints to
     * @param hint the ConfigurationMetadataHint containing the value hints to be added
     */
    private void addValueHints(ConfigurationMetadataProperty property, ConfigurationMetadataHint hint) {
		property.getHints().getValueHints().addAll(hint.getValueHints());
		property.getHints().getValueProviders().addAll(hint.getValueProviders());
	}

	/**
     * Adds the value hints and providers from the given hint to the key hints and providers of the specified property.
     * 
     * @param property the ConfigurationMetadataProperty to add the hints to
     * @param hint the ConfigurationMetadataHint containing the value hints and providers to be added
     */
    private void addMapHints(ConfigurationMetadataProperty property, ConfigurationMetadataHint hint) {
		property.getHints().getKeyHints().addAll(hint.getValueHints());
		property.getHints().getKeyProviders().addAll(hint.getValueProviders());
	}

	/**
	 * Create a new builder instance using {@link StandardCharsets#UTF_8} as the default
	 * charset and the specified json resource.
	 * @param inputStreams the source input streams
	 * @return a new {@link ConfigurationMetadataRepositoryJsonBuilder} instance.
	 * @throws IOException on error
	 */
	public static ConfigurationMetadataRepositoryJsonBuilder create(InputStream... inputStreams) throws IOException {
		ConfigurationMetadataRepositoryJsonBuilder builder = create();
		for (InputStream inputStream : inputStreams) {
			builder = builder.withJsonResource(inputStream);
		}
		return builder;
	}

	/**
	 * Create a new builder instance using {@link StandardCharsets#UTF_8} as the default
	 * charset.
	 * @return a new {@link ConfigurationMetadataRepositoryJsonBuilder} instance.
	 */
	public static ConfigurationMetadataRepositoryJsonBuilder create() {
		return create(StandardCharsets.UTF_8);
	}

	/**
	 * Create a new builder instance using the specified default {@link Charset}.
	 * @param defaultCharset the default charset to use
	 * @return a new {@link ConfigurationMetadataRepositoryJsonBuilder} instance.
	 */
	public static ConfigurationMetadataRepositoryJsonBuilder create(Charset defaultCharset) {
		return new ConfigurationMetadataRepositoryJsonBuilder(defaultCharset);
	}

}
