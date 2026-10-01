/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;

/**
 * Annotation {@link Processor} that writes meta-data file for
 * {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Kris De Volder
 * @author Jonas Keßler
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 1.2.0
 */
@SupportedAnnotationTypes({ ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.AUTO_CONFIGURATION_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONFIGURATION_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION })
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String ADDITIONAL_METADATA_LOCATIONS_OPTION = "org.springframework.boot.configurationprocessor.additionalMetadataLocations";

	static final String DESCRIPTION_CACHE_LOCATION_OPTION = "org.springframework.boot.configurationprocessor.descriptionCacheLocation";

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationProperties";

	static final String CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationPropertiesSource";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.NestedConfigurationProperty";

	static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot.context.properties.DeprecatedConfigurationProperty";

	static final String CONSTRUCTOR_BINDING_ANNOTATION = "org.springframework.boot.context.properties.bind.ConstructorBinding";

	static final String AUTOWIRED_ANNOTATION = "org.springframework.beans.factory.annotation.Autowired";

	static final String DEFAULT_VALUE_ANNOTATION = "org.springframework.boot.context.properties.bind.DefaultValue";

	static final String AUTO_CONFIGURATION_ANNOTATION = "org.springframework.boot.autoconfigure.AutoConfiguration";

	static final String CONFIGURATION_ANNOTATION = "org.springframework.context.annotation.Configuration";

	static final String CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint";

	static final String ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.Endpoint";

	static final String JMX_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint";

	static final String REST_CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint";

	static final String SERVLET_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint";

	static final String WEB_ENDPOINT_ANNOTATION = "org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint";

	static final String READ_OPERATION_ANNOTATION = "org.springframework.boot.actuate.endpoint.annotation.ReadOperation";

	static final String NAME_ANNOTATION = "org.springframework.boot.context.properties.bind.Name";

	static final String ENDPOINT_ACCESS_ENUM = "org.springframework.boot.actuate.endpoint.Access";

	static final Set<String> ENDPOINT_ANNOTATIONS = Set.of(CONTROLLER_ENDPOINT_ANNOTATION, ENDPOINT_ANNOTATION,
			JMX_ENDPOINT_ANNOTATION, REST_CONTROLLER_ENDPOINT_ANNOTATION, SERVLET_ENDPOINT_ANNOTATION,
			WEB_ENDPOINT_ANNOTATION);

	private static final Set<String> SUPPORTED_OPTIONS = Set.of(ADDITIONAL_METADATA_LOCATIONS_OPTION,
			DESCRIPTION_CACHE_LOCATION_OPTION);

	private JavaProcessingContext context;

	private ConfigurationMetadataGenerator generator;

	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	protected String configurationPropertiesSourceAnnotation() {
		return CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION;
	}

	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	protected String deprecatedConfigurationPropertyAnnotation() {
		return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	protected String constructorBindingAnnotation() {
		return CONSTRUCTOR_BINDING_ANNOTATION;
	}

	protected String autowiredAnnotation() {
		return AUTOWIRED_ANNOTATION;
	}

	protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	protected Set<String> endpointAnnotations() {
		return ENDPOINT_ANNOTATIONS;
	}

	protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	protected String nameAnnotation() {
		return NAME_ANNOTATION;
	}

	protected String endpointAccessEnum() {
		return ENDPOINT_ACCESS_ENUM;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public Set<String> getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		this.context = new JavaProcessingContext(env);
		MetadataGenerationEnvironment metadataEnv = new MetadataGenerationEnvironment(this.context,
				configurationPropertiesAnnotation(), configurationPropertiesSourceAnnotation(),
				nestedConfigurationPropertyAnnotation(), deprecatedConfigurationPropertyAnnotation(),
				constructorBindingAnnotation(), autowiredAnnotation(), defaultValueAnnotation(), endpointAnnotations(),
				readOperationAnnotation(), nameAnnotation());
		this.generator = new ConfigurationMetadataGenerator(this.context, metadataEnv, endpointAccessEnum());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		this.generator.process(new JavaProcessingRound(this.context, roundEnv));
		if (roundEnv.processingOver()) {
			try {
				writeSourceMetadata();
				writeMetadata();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	protected void writeSourceMetadata() throws Exception {
		this.generator.writeSourceMetadata();
	}

	protected ConfigurationMetadata writeMetadata() throws Exception {
		return this.generator.writeMetadata();
	}

}
