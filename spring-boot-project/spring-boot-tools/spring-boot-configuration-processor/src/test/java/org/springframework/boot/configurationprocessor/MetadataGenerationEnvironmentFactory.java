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

package org.springframework.boot.configurationprocessor;

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
		return new MetadataGenerationEnvironment(environment,
				TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.NESTED_CONFIGURATION_PROPERTY_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.DEFAULT_VALUE_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
				TestConfigurationMetadataAnnotationProcessor.READ_OPERATION_ANNOTATION);
	}

}
