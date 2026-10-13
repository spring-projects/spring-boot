/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link WebMvcProperties}.
 *
 * @author Stephane Nicoll
 */
class WebMvcPropertiesTests {

	private final WebMvcProperties properties = new WebMvcProperties();

	@Test
	void servletPathWhenEndsWithSlashHasValidMappingAndPrefix() {
		bind("spring.mvc.servlet.path", "/foo/");
		assertThat(this.properties.getServlet().getServletMapping()).isEqualTo("/foo/*");
		assertThat(this.properties.getServlet().getServletPrefix()).isEqualTo("/foo");
	}

	@Test
	void servletPathWhenDoesNotEndWithSlashHasValidMappingAndPrefix() {
		bind("spring.mvc.servlet.path", "/foo");
		assertThat(this.properties.getServlet().getServletMapping()).isEqualTo("/foo/*");
		assertThat(this.properties.getServlet().getServletPrefix()).isEqualTo("/foo");
	}

	@Test
	void servletPathWhenHasWildcardThrowsException() {
		assertThatExceptionOfType(BindException.class).isThrownBy(() -> bind("spring.mvc.servlet.path", "/*"))
			.withRootCauseInstanceOf(IllegalArgumentException.class)
			.satisfies((ex) -> assertThat(Throwables.getRootCause(ex)).hasMessage("'path' must not contain wildcards"));
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.mvc", Bindable.ofInstance(this.properties));
	}

}
