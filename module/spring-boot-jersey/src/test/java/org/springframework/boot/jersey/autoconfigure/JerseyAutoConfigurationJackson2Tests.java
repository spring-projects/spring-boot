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

package org.springframework.boot.jersey.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import jakarta.ws.rs.ext.ContextResolver;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyAutoConfiguration} when using Jersey's Jackson 2 support.
 *
 * @author Andy Wilkinson
 * @author Rene Schakmann
 */
@ClassPathExclusions("jersey-media-json-jackson3-*.jar")
@ClassPathOverrides("org.glassfish.jersey.media:jersey-media-json-jackson:4.0.3")
@SuppressWarnings("removal")
class JerseyAutoConfigurationJackson2Tests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JerseyAutoConfiguration.class,
				org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
		.withUserConfiguration(ResourceConfigConfiguration.class);

	@Test
	void jacksonFeatureAndObjectMapperContextResolverAreRegistered() {
		this.contextRunner.run((context) -> {
			ResourceConfig config = context.getBean(ResourceConfig.class);
			assertThat(config.isRegistered(JacksonFeature.class)).isTrue();
			assertThat(config.getInstances()).anyMatch(ContextResolver.class::isInstance);
		});
	}

	@Test
	void whenJaxbIsAvailableTheObjectMapperIsCustomizedWithAnAnnotationIntrospector() {
		this.contextRunner.run((context) -> {
			ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
			assertThat(objectMapper.getSerializationConfig()
				.getAnnotationIntrospector()
				.allIntrospectors()
				.stream()
				.filter(JakartaXmlBindAnnotationIntrospector.class::isInstance)).hasSize(1);
		});
	}

	@Test
	void whenJaxbIsNotAvailableTheObjectMapperCustomizationBacksOff() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("jakarta.xml.bind.annotation")).run((context) -> {
			ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
			assertThat(objectMapper.getSerializationConfig()
				.getAnnotationIntrospector()
				.allIntrospectors()
				.stream()
				.filter(JakartaXmlBindAnnotationIntrospector.class::isInstance)).isEmpty();
		});
	}

	@Test
	void whenJacksonJaxbModuleIsNotAvailableTheObjectMapperCustomizationBacksOff() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JakartaXmlBindAnnotationIntrospector.class))
			.run((context) -> {
				ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
				assertThat(objectMapper.getSerializationConfig()
					.getAnnotationIntrospector()
					.allIntrospectors()
					.stream()
					.filter(JakartaXmlBindAnnotationIntrospector.class::isInstance)).isEmpty();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

	}

}
