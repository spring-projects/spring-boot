/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchDataAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Artur Konczak
 * @author Stephane Nicoll
 */
public class ElasticsearchDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void templateBackOffWithNoClient() {
		this.context = new AnnotationConfigApplicationContext(
				ElasticsearchDataAutoConfiguration.class);
		assertThat(this.context.getBeanNamesForType(ElasticsearchTemplate.class))
				.isEmpty();
	}

	@Test
	public void templateExists() {
		load("spring.data.elasticsearch.properties.path.data:target/data",
				"spring.data.elasticsearch.properties.path.logs:target/logs");
		assertThat(this.context.getBeanNamesForType(ElasticsearchTemplate.class))
				.hasSize(1);
	}

	@Test
	public void mappingContextExists() {
		load("spring.data.elasticsearch.properties.path.data:target/data",
				"spring.data.elasticsearch.properties.path.logs:target/logs");
		assertThat(
				this.context.getBeanNamesForType(SimpleElasticsearchMappingContext.class))
						.hasSize(1);
	}

	@Test
	public void converterExists() {
		load("spring.data.elasticsearch.properties.path.data:target/data",
				"spring.data.elasticsearch.properties.path.logs:target/logs");
		assertThat(this.context.getBeanNamesForType(ElasticsearchConverter.class))
				.hasSize(1);
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment);
		context.register(PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class,
				ElasticsearchDataAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

}
