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

import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.annotation.XmlElement;
import org.glassfish.jersey.jackson3.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule.Priority;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration.JerseyWebApplicationInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.filter.RequestContextFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Kristoffer Larsen Hopland
 */
class JerseyAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JerseyAutoConfiguration.class, JerseyJacksonAutoConfiguration.class))
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
	void jacksonCustomizationBacksOffWithoutApplicationOrManagementResources() {
		new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(JerseyJacksonAutoConfiguration.class, JacksonAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(JsonMapper.class);
				assertThat(context).doesNotHaveBean(ResourceConfigCustomizer.class);
			});
	}

	@Test
	void jsonMapperIsRegisteredWithJersey() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class)).run((context) -> {
			ResourceConfig config = context.getBean(ResourceConfig.class);
			assertThat(config.isRegistered(JacksonFeature.class)).isTrue();
			ContextResolver<?> resolver = (ContextResolver<?>) config.getInstances()
				.stream()
				.filter(ContextResolver.class::isInstance)
				.findFirst()
				.orElseThrow();
			assertThat(resolver.getContext(Object.class)).isSameAs(context.getBean(JsonMapper.class));
		});
	}

	@Test
	void jackson3ProviderIsSelectedWithoutJsonMapper() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
			assertThat(context.getBean(ResourceConfig.class).getInstances())
				.noneMatch(ContextResolver.class::isInstance);
		});
	}

	@Test
	void jsonMapperCustomizerBacksOffWithoutJacksonFeature() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(JacksonFeature.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ResourceConfigCustomizer.class));
	}

	@Test
	void jackson3ProviderIsSelectedWithMultipleJsonMappers() {
		this.contextRunner.withUserConfiguration(MultipleJsonMappersConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
			assertThat(context.getBean(ResourceConfig.class).getInstances())
				.noneMatch(ContextResolver.class::isInstance);
		});
	}

	@Test
	void primaryJsonMapperIsRegisteredWithJersey() {
		this.contextRunner
			.withUserConfiguration(MultipleJsonMappersConfiguration.class, PrimaryJsonMapperConfiguration.class)
			.run((context) -> {
				ResourceConfig config = context.getBean(ResourceConfig.class);
				ContextResolver<?> resolver = (ContextResolver<?>) config.getInstances()
					.stream()
					.filter(ContextResolver.class::isInstance)
					.findFirst()
					.orElseThrow();
				JsonMapper mapper = (JsonMapper) resolver.getContext(Object.class);
				assertThat(mapper).isNotSameAs(context.getBean("primaryJsonMapper"));
				assertThat(mapper.writeValueAsString(new JaxbAnnotatedBean())).isEqualTo("{\"jackson\":\"value\"}");
				assertThat(mapper.serializationConfig().getAnnotationIntrospector().allIntrospectors())
					.anyMatch(JakartaXmlBindAnnotationIntrospector.class::isInstance);
			});
	}

	@Test
	void jsonMapperIsRegisteredWithoutJacksonAutoConfigurationModule() {
		this.contextRunner.withUserConfiguration(PrimaryJsonMapperConfiguration.class)
			.withClassLoader(new FilteredClassLoader("org.springframework.boot.jackson"))
			.run((context) -> {
				assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
				assertThat(context.getBean(ResourceConfig.class).isRegistered(JacksonFeature.class)).isTrue();
			});
	}

	@Test
	void whenJaxbIsAvailableTheJsonMapperIsCustomizedWithAnAnnotationIntrospector() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class)).run((context) -> {
			JsonMapper jsonMapper = context.getBean(JsonMapper.class);
			assertThat(jsonMapper.serializationConfig()
				.getAnnotationIntrospector()
				.allIntrospectors()
				.stream()
				.filter(JakartaXmlBindAnnotationIntrospector.class::isInstance)).hasSize(1);
		});
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void userProvidedJaxbModuleIsUsed(boolean findAndAddModules) {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withUserConfiguration(CustomJaxbModuleConfiguration.class)
			.withPropertyValues("spring.jackson.find-and-add-modules=" + findAndAddModules)
			.run((context) -> {
				JsonMapper jsonMapper = context.getBean(JsonMapper.class);
				assertThat(jsonMapper.writeValueAsString(new JaxbAnnotatedBean())).isEqualTo("{\"jaxb\":\"value\"}");
			});
	}

	@Test
	void whenJaxbIsNotAvailableTheJsonMapperCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withPropertyValues("spring.jackson.find-and-add-modules=false")
			.withClassLoader(new FilteredClassLoader("jakarta.xml.bind.annotation"))
			.run((context) -> {
				JsonMapper jsonMapper = context.getBean(JsonMapper.class);
				assertThat(jsonMapper.serializationConfig()
					.getAnnotationIntrospector()
					.allIntrospectors()
					.stream()
					.filter(JakartaXmlBindAnnotationIntrospector.class::isInstance)).isEmpty();
			});
	}

	@Test
	void whenJacksonJaxbModuleIsNotAvailableTheJsonMapperCustomizationBacksOff() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withPropertyValues("spring.jackson.find-and-add-modules=false")
			.withClassLoader(new FilteredClassLoader("tools.jackson.module.jakarta.xmlbind"))
			.run((context) -> {
				JsonMapper jsonMapper = context.getBean(JsonMapper.class);
				assertThat(jsonMapper.serializationConfig()
					.getAnnotationIntrospector()
					.allIntrospectors()
					.stream()
					.filter(JakartaXmlBindAnnotationIntrospector.class::isInstance)).isEmpty();
			});
	}

	@Test
	void webApplicationInitializerDisablesJerseyWebApplicationInitializer() throws ServletException {
		ServletContext context = new MockServletContext();
		new JerseyWebApplicationInitializer().onStartup(context);
		assertThat(context.getInitParameter("contextConfigLocation")).isEqualTo("<NONE>");
	}

	@Test
	@ClassPathExclusions("jersey-spring6-*.jar")
	void webApplicationInitializerHasNoEffectWhenJerseyIsAbsent() throws ServletException {
		ServletContext context = new MockServletContext();
		new JerseyWebApplicationInitializer().onStartup(context);
		assertThat(Collections.list(context.getInitParameterNames())).isEmpty();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJaxbModuleConfiguration {

		@Bean
		JacksonModule customJaxbModule() {
			return new JakartaXmlBindAnnotationModule().setPriority(Priority.PRIMARY);
		}

	}

	static class JaxbAnnotatedBean {

		@JsonProperty("jackson")
		@XmlElement(name = "jaxb")
		String getValue() {
			return "value";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleJsonMappersConfiguration {

		@Bean
		JsonMapper firstJsonMapper() {
			return JsonMapper.builder().build();
		}

		@Bean
		JsonMapper secondJsonMapper() {
			return JsonMapper.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PrimaryJsonMapperConfiguration {

		@Bean
		@Primary
		JsonMapper primaryJsonMapper() {
			return JsonMapper.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextFilterConfiguration {

		@Bean
		RequestContextFilter requestContextFilter() {
			return new RequestContextFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextFilterRegistrationConfiguration {

		@Bean
		FilterRegistrationBean<RequestContextFilter> customRequestContextFilterRegistration() {
			return new FilterRegistrationBean<>(new RequestContextFilter());
		}

	}

}
