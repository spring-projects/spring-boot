/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.processing.ProcessingEnvironment;

import org.springframework.boot.configurationprocessor.test.TestConfigurationMetadataAnnotationProcessor;

/**
 * A factory for {@link MetadataGenerationEnvironment} against test annotations.
 *
 * @author Stephane Nicoll
 */
class MetadataGenerationEnvironmentFactory implements Function<ProcessingEnvironment, MetadataGenerationEnvironment> {

	@Override
	public MetadataGenerationEnvironment apply(ProcessingEnvironment environment) {
		Set<String> endpointAnnotations = new HashSet<>(
				Arrays.asList(TestConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
						TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
						TestConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
						TestConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
						TestConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION));
		return new MetadataGenerationEnvironment(environment,
				TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_SOURCE_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.NESTED_CONFIGURATION_PROPERTY_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.CONSTRUCTOR_BINDING_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.AUTOWIRED_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.DEFAULT_VALUE_ANNOTATION, endpointAnnotations,
				TestConfigurationMetadataAnnotationProcessor.READ_OPERATION_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.NAME_ANNOTATION);
	}

}
