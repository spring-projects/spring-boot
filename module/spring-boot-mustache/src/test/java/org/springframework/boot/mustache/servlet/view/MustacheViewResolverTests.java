/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.servlet.view;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class MustacheViewResolverTests {

	private final MustacheViewResolver resolver = new MustacheViewResolver();

	@BeforeEach
	void init() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		this.resolver.setApplicationContext(applicationContext);
		this.resolver.setServletContext(new MockServletContext());
		this.resolver.setPrefix("classpath:");
		this.resolver.setSuffix(".html");
	}

	@Test
	void resolveNonExistent() throws Exception {
		assertThat(this.resolver.resolveViewName("bar", Locale.ROOT)).isNull();
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void resolveExisting() throws Exception {
		assertThat(this.resolver.resolveViewName("template", Locale.ROOT)).isNotNull();
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void setsContentType() throws Exception {
		this.resolver.setContentType("application/octet-stream");
		View view = this.resolver.resolveViewName("template", Locale.ROOT);
		assertThat(view).isNotNull();
		assertThat(view.getContentType()).isEqualTo("application/octet-stream");

	}

}
