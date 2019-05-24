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

package org.springframework.boot.autoconfigure.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.RequestContextFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JerseyAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JerseyAutoConfiguration.class))
			.withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.INFO))
			.withUserConfiguration(ResourceConfigConfiguration.class);

	@Test
	void requestContextFilterRegistrationIsAutoConfigured() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
			assertThat(registration.getFilter()).isInstanceOf(RequestContextFilter.class);
		});
	}

	@Test
	void whenUserDefinesARequestContextFilterTheAutoConfiguredRegistrationBacksOff() {
		this.contextRunner.withUserConfiguration(RequestContextFilterConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
			assertThat(context).hasSingleBean(RequestContextFilter.class);
		});
	}

	@Test
	void whenUserDefinesARequestContextFilterRegistrationTheAutoConfiguredRegistrationBacksOff() {
		this.contextRunner.withUserConfiguration(RequestContextFilterRegistrationConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			assertThat(context).hasBean("customRequestContextFilterRegistration");
		});
	}

	@Test
	void whenJaxbIsAvailableTheObjectMapperIsCustomizedWithAnAnnotationIntrospector() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class)).run((context) -> {
			ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
			assertThat(objectMapper.getSerializationConfig().getAnnotationIntrospector().allIntrospectors().stream()
					.filter(JaxbAnnotationIntrospector.class::isInstance)).hasSize(1);
		});
	}

	@Test
	void whenJaxbIsNotAvailableTheObjectMapperCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader("javax.xml.bind.annotation")).run((context) -> {
					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
					assertThat(objectMapper.getSerializationConfig().getAnnotationIntrospector().allIntrospectors()
							.stream().filter(JaxbAnnotationIntrospector.class::isInstance)).isEmpty();
				});
	}

	@Test
	void whenJacksonJaxbModuleIsNotAvailableTheObjectMapperCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
				.withClassLoader(new FilteredClassLoader(JaxbAnnotationIntrospector.class)).run((context) -> {
					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
					assertThat(objectMapper.getSerializationConfig().getAnnotationIntrospector().allIntrospectors()
							.stream().filter(JaxbAnnotationIntrospector.class::isInstance)).isEmpty();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfigConfiguration {

		@Bean
		public ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextFilterConfiguration {

		@Bean
		public RequestContextFilter requestContextFilter() {
			return new RequestContextFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextFilterRegistrationConfiguration {

		@Bean
		public FilterRegistrationBean<RequestContextFilter> customRequestContextFilterRegistration() {
			return new FilterRegistrationBean<>(new RequestContextFilter());
		}

	}

}
