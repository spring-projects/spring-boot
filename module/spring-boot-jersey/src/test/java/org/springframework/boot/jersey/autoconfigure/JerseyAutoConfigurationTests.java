/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure;

import java.util.Collections;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration.JerseyWebApplicationInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
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
