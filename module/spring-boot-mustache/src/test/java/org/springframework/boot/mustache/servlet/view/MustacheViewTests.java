/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.servlet.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link MustacheView}.
 *
 * @author Dave Syer
 * @author Van Vo
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

	@AfterEach
	void closeContext() {
		this.context.close();
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
	void directViewDoesNotCacheByDefault(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "Hello {{World}}");
		MustacheView view = createView(template);
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		Files.writeString(template, "Updated {{World}}");
		assertThat(render(view, "Boot")).isEqualTo("Updated Boot");
	}

	@Test
	void disablingAndEnablingCacheReloadsTemplate(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "Hello {{World}}");
		MustacheView view = createView(template);
		view.setCache(true);
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		Files.writeString(template, "Updated {{World}}");
		view.setCache(false);
		assertThat(render(view, "Boot")).isEqualTo("Updated Boot");
		Files.writeString(template, "Final {{World}}");
		view.setCache(true);
		assertThat(render(view, "Spring")).isEqualTo("Final Spring");
		Files.delete(template);
		assertThat(render(view, "Boot")).isEqualTo("Final Boot");
	}

	@Test
	void changingCompilerInvalidatesCachedTemplate(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "{{World}}");
		MustacheView view = createView(template);
		view.setCache(true);
		assertThat(render(view, "<b>")).isEqualTo("&lt;b&gt;");
		view.setCompiler(Mustache.compiler().escapeHTML(false));
		assertThat(render(view, "<b>")).isEqualTo("<b>");
	}

	@Test
	void changingCharsetInvalidatesCachedTemplate(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "Grüße {{World}}", StandardCharsets.UTF_16LE);
		MustacheView view = createView(template);
		view.setCharset(StandardCharsets.UTF_16LE);
		view.setCache(true);
		assertThat(render(view, "Spring")).isEqualTo("Grüße Spring");
		Files.writeString(template, "Bonjour {{World}}", StandardCharsets.UTF_8);
		view.setCharset(StandardCharsets.UTF_8);
		assertThat(render(view, "Boot")).isEqualTo("Bonjour Boot");
	}

	@Test
	void changingUrlInvalidatesCachedTemplate(@TempDir Path directory) throws Exception {
		Path first = directory.resolve("first.html");
		Path second = directory.resolve("second.html");
		Files.writeString(first, "Hello {{World}}");
		Files.writeString(second, "Goodbye {{World}}");
		MustacheView view = createView(first);
		view.setCache(true);
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		view.setUrl(second.toUri().toString());
		assertThat(render(view, "Boot")).isEqualTo("Goodbye Boot");
	}

	@Test
	void cachedTemplateReusesPartialsWithEachModel(@TempDir Path directory) throws Exception {
		Path template = directory.resolve("template.html");
		Files.writeString(template, "Hello {{> partial}}");
		AtomicInteger partialLoads = new AtomicInteger();
		MustacheView view = createView(template);
		view.setCompiler(Mustache.compiler().withLoader((name) -> {
			assertThat(name).isEqualTo("partial");
			partialLoads.incrementAndGet();
			return new StringReader("{{World}}");
		}));
		view.setCache(true);
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		assertThat(render(view, "Boot")).isEqualTo("Hello Boot");
		assertThat(partialLoads).hasValue(1);
	}

	@Test
	void failedResourceReadCanBeRetried() throws Exception {
		Resource resource = mock(Resource.class);
		given(resource.exists()).willReturn(true);
		given(resource.getInputStream()).willThrow(new IOException("Cannot read template"))
			.willReturn(new ByteArrayInputStream("Hello {{World}}".getBytes(StandardCharsets.UTF_8)));
		this.context.addProtocolResolver((location, loader) -> "test:template".equals(location) ? resource : null);
		MustacheView view = new MustacheView();
		view.setCompiler(Mustache.compiler());
		view.setUrl("test:template");
		view.setApplicationContext(this.context);
		view.setCache(true);
		assertThatIOException().isThrownBy(() -> render(view, "Spring"));
		assertThat(render(view, "Spring")).isEqualTo("Hello Spring");
		assertThat(render(view, "Boot")).isEqualTo("Hello Boot");
		then(resource).should(times(2)).getInputStream();
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
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		view.render(Map.of("World", world), this.request, response);
		return response.getContentAsString();
	}

}
