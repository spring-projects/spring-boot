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

package org.springframework.boot.configurationprocessor.tests;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the configuration metadata annotation processor.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationProcessorIntegrationTests {

	private static ConfigurationMetadataRepository repository;

	@BeforeClass
	public static void readMetadata() throws IOException {
		Resource resource = new ClassPathResource("META-INF/spring-configuration-metadata.json");
		assertThat(resource.exists()).isTrue();
		// Make sure the right file is detected
		assertThat(resource.getURL().toString()).contains("spring-boot-configuration-processor-tests");
		repository = ConfigurationMetadataRepositoryJsonBuilder.create(resource.getInputStream()).build();

	}

	@Test
	public void extractTypeFromAnnotatedGetter() {
		ConfigurationMetadataProperty property = repository.getAllProperties().get("annotated.name");
		assertThat(property).isNotNull();
		assertThat(property.getType()).isEqualTo("java.lang.String");
	}

}
