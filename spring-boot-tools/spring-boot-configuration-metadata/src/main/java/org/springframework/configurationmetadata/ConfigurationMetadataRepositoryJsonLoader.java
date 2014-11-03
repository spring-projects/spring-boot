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

package org.springframework.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;

import org.json.JSONException;

/**
 * Load a {@link ConfigurationMetadataRepository} from the content of arbitrary resource(s).
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigurationMetadataRepositoryJsonLoader {

	public static final Charset UTF_8 = Charset.forName("UTF-8");

	private JsonReader reader = new JsonReader();


	/**
	 * Create a {@link ConfigurationMetadataRepository} with the metadata defined by
	 * the specified {@code resources} using {@link #UTF_8}. If the same config
	 * metadata items is held within different resources, the first that is loaded is kept.
	 */
	public ConfigurationMetadataRepository loadAll(Collection<InputStream> resources) throws IOException {
		return loadAll(resources, UTF_8);
	}

	/**
	 * Create a {@link ConfigurationMetadataRepository} with the metadata defined by
	 * the specified {@code resources} using the specified {@link Charset}. If the
	 * same config metadata items is held within different resources, the first that
	 * is loaded is kept.
	 */
	public ConfigurationMetadataRepository loadAll(Collection<InputStream> resources, Charset charset) throws IOException {
		if (resources == null) {
			throw new IllegalArgumentException("Resources must not be null.");
		}
		if (resources.size() == 1) {
			return load(resources.iterator().next(), charset);
		}

		SimpleConfigurationMetadataRepository repository = new SimpleConfigurationMetadataRepository();
		for (InputStream resource : resources) {
			SimpleConfigurationMetadataRepository repo = load(resource, charset);
			repository.include(repo);
		}
		return repository;
	}

	private SimpleConfigurationMetadataRepository load(InputStream stream, Charset charset) throws IOException {
		try {
			RawConfigurationMetadata metadata = this.reader.read(stream, charset);
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
		return repository;
	}

}

