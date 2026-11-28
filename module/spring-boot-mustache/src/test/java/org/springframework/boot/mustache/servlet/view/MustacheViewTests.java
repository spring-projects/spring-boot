/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.servlet.view;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheView}.
 *
 * @author Dave Syer
 */
class MustacheViewTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();

	@BeforeEach
	void init() {
		this.context.refresh();
		MockServletContext servletContext = new MockServletContext();
		this.context.setServletContext(servletContext);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void viewResolvesHandlebars() throws Exception {
		MustacheView view = new MustacheView();
		view.setCompiler(Mustache.compiler());
		view.setUrl("classpath:template.html");
		view.setApplicationContext(this.context);
		view.render(Collections.singletonMap("World", "Spring"), this.request, this.response);
		assertThat(this.response.getContentAsString().trim()).isEqualTo("Hello Spring");
	}

	@Test
	void templateIsNotCachedByDefault(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "Hello {{World}}");
		MustacheView view = createView(template);
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		Files.writeString(template, "Updated {{World}}");
		assertThat(render(view, "Boot")).isEqualTo("Updated Boot");
	}

	@Test
	void cachedTemplateAndPartialsAreReused(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "Hello {{> partial}}");
		AtomicInteger partialLoads = new AtomicInteger();
		MustacheView view = createView(template);
		view.setCompiler(Mustache.compiler().withLoader((name) -> {
			partialLoads.incrementAndGet();
			return new StringReader("{{World}}");
		}));
		view.setCache(true);
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		assertThat(render(view, "Boot")).isEqualTo("Hello Boot");
		assertThat(partialLoads).hasValue(1);
	}

	private MustacheView createView(Path template) {
		MustacheView view = new MustacheView();
		view.setCompiler(Mustache.compiler());
		view.setUrl(template.toUri().toString());
		view.setApplicationContext(this.context);
		return view;
	}

	private String render(MustacheView view, String world) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(Map.of("World", world), this.request, response);
		return response.getContentAsString();
	}

}
