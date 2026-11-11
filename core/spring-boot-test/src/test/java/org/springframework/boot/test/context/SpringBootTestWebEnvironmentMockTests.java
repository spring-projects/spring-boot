/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} configured with
 * {@link WebEnvironment#MOCK}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest("value=123")
@DirtiesContext
class SpringBootTestWebEnvironmentMockTests {

	@Value("${value}")
	private int value;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ServletContext servletContext;

	@Test
	void annotationAttributesOverridePropertiesFile() {
		assertThat(this.value).isEqualTo(123);
	}

	@Test
	void validateWebApplicationContextIsSet() {
		WebApplicationContext fromServletContext = WebApplicationContextUtils
			.getWebApplicationContext(this.servletContext);
		assertThat(fromServletContext).isSameAs(this.context);
	}

	@Test
	void setsRequestContextHolder() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		assertThat(attributes).isNotNull();
	}

	@Test
	void resourcePath() {
		assertThat(this.servletContext).hasFieldOrPropertyWithValue("resourceBasePath", "src/main/webapp");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class Config {

		@Bean
		static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
