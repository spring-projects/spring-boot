/*
 * Copyright 2012-present the original author or authors.
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
			if (url.toString().contains("spring-boot-configuration-processor-integration-tests")) {
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
