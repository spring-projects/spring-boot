/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.configurationprocessor.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import org.springframework.boot.configurationprocessor.ConfigurationMetadataAnnotationProcessor;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 * Test {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 */
@SupportedAnnotationTypes({ "*" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestConfigurationMetadataAnnotationProcessor
		extends ConfigurationMetadataAnnotationProcessor {

	public static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot.configurationsample.ConfigurationProperties";

	public static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.configurationsample.NestedConfigurationProperty";

	public static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.configurationsample.DeprecatedConfigurationProperty";

	public static final String DEFAULT_VALUE_ANNOTATION = "org.springframework.boot.configurationsample.DefaultValue";

	public static final String ENDPOINT_ANNOTATION = "org.springframework.boot.configurationsample.Endpoint";

	public static final String READ_OPERATION_ANNOTATION = "org.springframework.boot.configurationsample.ReadOperation";

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
	protected String deprecatedConfigurationPropertyAnnotation() {
		return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	@Override
	protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	@Override
	protected String endpointAnnotation() {
		return ENDPOINT_ANNOTATION;
	}

	@Override
	protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	@Override
	protected ConfigurationMetadata writeMetaData() throws Exception {
		super.writeMetaData();
		try {
			File metadataFile = new File(this.outputLocation,
					"META-INF/spring-configuration-metadata.json");
			if (metadataFile.isFile()) {
				this.metadata = new JsonMarshaller()
						.read(new FileInputStream(metadataFile));
			}
			else {
				this.metadata = new ConfigurationMetadata();
			}
			return this.metadata;
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read metadata from disk", ex);
		}
	}

	public ConfigurationMetadata getMetadata() {
		return this.metadata;
	}

}
