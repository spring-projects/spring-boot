/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

/**
 * Load a {@link ConfigurationMetadataRepository} from the content of arbitrary resource(s).
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ConfigurationMetadataRepositoryJsonBuilder {

	public static final Charset UTF_8 = Charset.forName("UTF-8");

	private Charset defaultCharset = UTF_8;

	private final JsonReader reader = new JsonReader();

	private final List<SimpleConfigurationMetadataRepository> repositories
			= new ArrayList<SimpleConfigurationMetadataRepository>();


	/**
	 * Create a new builder instance using {@link #UTF_8} as the default charset.
	 */
	public static ConfigurationMetadataRepositoryJsonBuilder create() {
		return create(UTF_8);
	}

	/**
	 * Create a new builder instance using the specified default {@link Charset}.
	 */
	public static ConfigurationMetadataRepositoryJsonBuilder create(Charset defaultCharset) {
		return new ConfigurationMetadataRepositoryJsonBuilder(defaultCharset);
	}

	private ConfigurationMetadataRepositoryJsonBuilder(Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Add the content of a {@link ConfigurationMetadataRepository} defined by the specified
	 * {@link InputStream} json document using the default charset. If this metadata
	 * repository holds items that were loaded previously, these are ignored.
	 * <p>Leave the stream open when done.
	 */
	public ConfigurationMetadataRepositoryJsonBuilder withJsonResource(InputStream in)
			throws IOException {
		return withJsonResource(in, defaultCharset);
	}

	/**
	 * Add the content of a {@link ConfigurationMetadataRepository} defined by the specified
	 * {@link InputStream} json document using the specified {@link Charset}. If this metadata
	 * repository holds items that were loaded previously, these are ignored.
	 * <p>Leave the stream open when done.
	 */
	public ConfigurationMetadataRepositoryJsonBuilder withJsonResource(InputStream inputstream, Charset charset)
			throws IOException {
		if (inputstream == null) {
			throw new IllegalArgumentException("InputStream must not be null.");
		}
		repositories.add(add(inputstream, charset));
		return this;
	}

	/**
	 * Build a {@link ConfigurationMetadataRepository} with the current state of this builder.
	 */
	public ConfigurationMetadataRepository build() {
		SimpleConfigurationMetadataRepository result = new SimpleConfigurationMetadataRepository();
		for (SimpleConfigurationMetadataRepository repository : repositories) {
			result.include(repository);
		}
		return result;
	}

	private SimpleConfigurationMetadataRepository add(InputStream in, Charset charset) throws IOException {
		try {
			RawConfigurationMetadata metadata = this.reader.read(in, charset);
			return create(metadata);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Failed to read configuration metadata", e);
		}
		catch (JSONException e) {
			throw new IllegalArgumentException("Invalid configuration metadata document", e);
		}
	}

	private SimpleConfigurationMetadataRepository create(RawConfigurationMetadata metadata) {
		SimpleConfigurationMetadataRepository repository = new SimpleConfigurationMetadataRepository();
		repository.add(metadata.getSources());
		for (ConfigurationMetadataItem item : metadata.getItems()) {
			ConfigurationMetadataSource source = null;
			String sourceType = item.getSourceType();
			if (sourceType != null) {
				source = metadata.getSource(sourceType);
			}
			repository.add(item, source);
		}
		Map<String, ConfigurationMetadataProperty> allProperties = repository.getAllProperties();
		for (ConfigurationMetadataHint hint : metadata.getHints()) {
			ConfigurationMetadataProperty property = allProperties.get(hint.getId());
			if (property != null) {
				property.getValueHints().addAll(hint.getValueHints());
				property.getValueProviders().addAll(hint.getValueProviders());
			}
		}
		return repository;
	}

}

