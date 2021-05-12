/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.util.Throwables;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.IncompatibleConfigurationException;
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
				.withRootCauseInstanceOf(IllegalArgumentException.class).satisfies(
						(ex) -> assertThat(Throwables.getRootCause(ex)).hasMessage("Path must not contain wildcards"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void incompatiblePathMatchSuffixConfig() {
		this.properties.getPathmatch().setMatchingStrategy(WebMvcProperties.MatchingStrategy.PATH_PATTERN_PARSER);
		this.properties.getPathmatch().setUseSuffixPattern(true);
		assertThatExceptionOfType(IncompatibleConfigurationException.class)
				.isThrownBy(this.properties::checkConfiguration);
	}

	@Test
	@SuppressWarnings("deprecation")
	void incompatiblePathMatchRegisteredSuffixConfig() {
		this.properties.getPathmatch().setMatchingStrategy(WebMvcProperties.MatchingStrategy.PATH_PATTERN_PARSER);
		this.properties.getPathmatch().setUseRegisteredSuffixPattern(true);
		assertThatExceptionOfType(IncompatibleConfigurationException.class)
				.isThrownBy(this.properties::checkConfiguration);
	}

	@Test
	void incompatiblePathMatchServletPathConfig() {
		this.properties.getPathmatch().setMatchingStrategy(WebMvcProperties.MatchingStrategy.PATH_PATTERN_PARSER);
		this.properties.getServlet().setPath("/test");
		assertThatExceptionOfType(IncompatibleConfigurationException.class)
				.isThrownBy(this.properties::checkConfiguration);
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.mvc", Bindable.ofInstance(this.properties));
	}

}
