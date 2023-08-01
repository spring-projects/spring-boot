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

package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.test.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Sample test for {@link DataElasticsearchTest @DataElasticsearchTest}
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataElasticsearchTest
@Testcontainers(disabledWithoutDocker = true)
class DataElasticsearchTestIntegrationTests {

	@Container
	@ServiceConnection
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageNames.elasticsearch())
		.withEnv("ES_JAVA_OPTS", "-Xms32m -Xmx512m")
		.withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10));

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void didNotInjectExampleService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Test
	void testRepository() {
		ExampleDocument document = new ExampleDocument();
		document.setText("Look, new @DataElasticsearchTest!");
		String id = UUID.randomUUID().toString();
		document.setId(id);
		ExampleDocument savedDocument = this.exampleRepository.save(document);
		ExampleDocument getDocument = this.elasticsearchTemplate.get(id, ExampleDocument.class);
		assertThat(getDocument).isNotNull();
		assertThat(getDocument.getId()).isNotNull();
		assertThat(getDocument.getId()).isEqualTo(savedDocument.getId());
		this.exampleRepository.deleteAll();
	}

	@Test
	void serviceConnectionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(ServiceConnectionAutoConfiguration.class));
	}

}
