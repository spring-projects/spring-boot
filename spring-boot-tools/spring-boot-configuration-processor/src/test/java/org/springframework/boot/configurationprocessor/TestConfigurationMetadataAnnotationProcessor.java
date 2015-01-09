/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.boot.configurationprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 */
@SupportedAnnotationTypes({ "*" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestConfigurationMetadataAnnotationProcessor extends
		ConfigurationMetadataAnnotationProcessor {

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot.configurationsample.ConfigurationProperties";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.configurationsample.NestedConfigurationProperty";

	private ConfigurationMetadata metadata;

	private final File outputLocation;

	public TestConfigurationMetadataAnnotationProcessor(File outputLocation) {
		this.outputLocation = outputLocation;
	}

	@Override
	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	@Override
	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	@Override
	protected ConfigurationMetadata writeMetaData(ConfigurationMetadata metadata) {
		super.writeMetaData(metadata);
		try {
			File metadataFile = new File(this.outputLocation,
					"META-INF/spring-configuration-metadata.json");
			if (metadataFile.isFile()) {
				this.metadata = new JsonMarshaller().read(new FileInputStream(
						metadataFile));
			}
			else {
				this.metadata = metadata;
			}
			return this.metadata;
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read metadata from disk", e);
		}
	}

	public ConfigurationMetadata getMetadata() {
		return this.metadata;
	}

	public Set<String> getProcessedTypes() {
		return this.processedSourceTypes;
	}

	@Override
	public boolean isIncremental() {
		return super.isIncremental();
	}
}