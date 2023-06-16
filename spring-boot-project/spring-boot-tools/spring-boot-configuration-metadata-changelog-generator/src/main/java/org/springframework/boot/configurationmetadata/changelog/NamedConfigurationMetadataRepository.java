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

package org.springframework.boot.configurationmetadata.changelog;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;

/**
 * A {@link ConfigurationMetadataRepository} with a name.
 *
 * @author Andy Wilkinson
 */
class NamedConfigurationMetadataRepository implements ConfigurationMetadataRepository {

	private final String name;

	private final ConfigurationMetadataRepository delegate;

	NamedConfigurationMetadataRepository(String name, ConfigurationMetadataRepository delegate) {
		this.name = name;
		this.delegate = delegate;
	}

	/**
	 * The name of the metadata held in the repository.
	 * @return the name of the metadata
	 */
	String getName() {
		return this.name;
	}

	@Override
	public Map<String, ConfigurationMetadataGroup> getAllGroups() {
		return this.delegate.getAllGroups();
	}

	@Override
	public Map<String, ConfigurationMetadataProperty> getAllProperties() {
		return this.delegate.getAllProperties();
	}

	static NamedConfigurationMetadataRepository from(File metadataDir) {
		ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
		for (File jar : metadataDir.listFiles()) {
			try (JarFile jarFile = new JarFile(jar)) {
				JarEntry jsonMetadata = jarFile.getJarEntry("META-INF/spring-configuration-metadata.json");
				if (jsonMetadata != null) {
					builder.withJsonResource(jarFile.getInputStream(jsonMetadata));
				}
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return new NamedConfigurationMetadataRepository(metadataDir.getName(), builder.build());
	}

}
