/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.servlet;

import jakarta.servlet.Registration.Dynamic;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicRegistrationBean}.
 *
 * @author Moritz Halbritter
 */
class DynamicRegistrationBeanTests {

	@Test
	void shouldUseNameIfSet() {
		DynamicRegistrationBean<?> bean = createBean();
		bean.setName("givenName");
		assertThat(bean.getOrDeduceName("dummy")).isEqualTo("givenName");
	}

	@Test
	void shouldUseBeanNameIfNameIsNotSet() {
		DynamicRegistrationBean<?> bean = createBean();
		bean.setBeanName("beanName");
		assertThat(bean.getOrDeduceName("dummy")).isEqualTo("beanName");
	}

	@Test
	void shouldUseConventionBasedNameIfNoNameOrBeanNameIsSet() {
		DynamicRegistrationBean<?> bean = createBean();
		assertThat(bean.getOrDeduceName("dummy")).isEqualTo("string");
	}

	private static DynamicRegistrationBean<?> createBean() {
		return new DynamicRegistrationBean<>() {
			@Override
			protected @Nullable Dynamic addRegistration(String description, ServletContext servletContext) {
				return null;
			}

			@Override
			protected String getDescription() {
				return "";
			}
		};
	}

}
