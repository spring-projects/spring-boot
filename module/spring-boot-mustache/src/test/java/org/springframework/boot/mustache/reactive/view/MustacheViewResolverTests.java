/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.reactive.view;

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Brian Clozel
 */
class MustacheViewResolverTests {

	private final MustacheViewResolver resolver = new MustacheViewResolver();

	@BeforeEach
	void init() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		this.resolver.setApplicationContext(applicationContext);
		this.resolver.setPrefix("classpath:");
		this.resolver.setSuffix(".html");
	}

	@Test
	void resolveNonExistent() {
		assertThat(this.resolver.resolveViewName("bar", Locale.ROOT).block(Duration.ofSeconds(30))).isNull();
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void resolveExisting() {
		assertThat(this.resolver.resolveViewName("template", Locale.ROOT).block(Duration.ofSeconds(30))).isNotNull();
	}

}
