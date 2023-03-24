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

package org.springframework.boot.configurationprocessor.tests;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the configuration metadata annotation processor.
 *
 * @author Stephane Nicoll
 */
class ConfigurationProcessorIntegrationTests {

	private static ConfigurationMetadataRepository repository;

	@BeforeAll
	static void readMetadata() throws IOException {
		repository = ConfigurationMetadataRepositoryJsonBuilder.create(getResource().openStream()).build();
	}

	private static URL getResource() throws IOException {
		ClassLoader classLoader = ConfigurationProcessorIntegrationTests.class.getClassLoader();
		List<URL> urls = new ArrayList<>();
		CollectionUtils.toIterator(classLoader.getResources("META-INF/spring-configuration-metadata.json"))
			.forEachRemaining(urls::add);
		for (URL url : urls) {
			if (url.toString().contains("spring-boot-configuration-processor-tests")) {
				return url;
			}
		}
		throw new IllegalStateException("Unable to find correct configuration-metadata resource from " + urls);
	}

	@Test
	void extractTypeFromAnnotatedGetter() {
		ConfigurationMetadataProperty property = repository.getAllProperties().get("annotated.name");
		assertThat(property).isNotNull();
		assertThat(property.getType()).isEqualTo("java.lang.String");
	}

}
