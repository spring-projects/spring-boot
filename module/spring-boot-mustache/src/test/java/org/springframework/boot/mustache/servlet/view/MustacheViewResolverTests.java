/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mustache.servlet.view;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Van Vo
 */
class MustacheViewResolverTests {

	private final Mustache.Compiler compiler = spy(Mustache.compiler());

	private final MustacheViewResolver resolver = new MustacheViewResolver(this.compiler);

	@BeforeEach
	void init() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		this.resolver.setApplicationContext(applicationContext);
		this.resolver.setServletContext(new MockServletContext());
		this.resolver.setPrefix("classpath:");
		this.resolver.setSuffix(".html");
		this.resolver.setExposeSpringMacroHelpers(false);
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
	void cacheEnabledCachesCompiledTemplate() throws Exception {
		this.resolver.setCache(true);
		assertThat(render("Spring")).isEqualTo("Hello Spring");
		assertThat(render("Boot")).isEqualTo("Hello Boot");
		then(this.compiler).should().compile(any(Reader.class));
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void cacheDisabledCompilesTemplateForEachView() throws Exception {
		this.resolver.setCache(false);
		assertThat(render("Spring")).isEqualTo("Hello Spring");
		assertThat(render("Boot")).isEqualTo("Hello Boot");
		then(this.compiler).should(times(2)).compile(any(Reader.class));
	}

	@Test
	void cachedTemplateSurvivesResourceRemoval(@TempDir Path directory) throws Exception {
		Path template = writeTemplate(directory, "Hello {{World}}");
		this.resolver.setCache(true);
		assertThat(render("Spring")).isEqualTo("Hello Spring");
		Files.delete(template);
		assertThat(render("Boot")).isEqualTo("Hello Boot");
		then(this.compiler).should().compile(any(Reader.class));
	}

	@Test
	void cacheDisabledReflectsTemplateChanges(@TempDir Path directory) throws Exception {
		Path template = writeTemplate(directory, "Hello {{World}}");
		this.resolver.setCache(false);
		assertThat(render("Spring")).isEqualTo("Hello Spring");
		Files.writeString(template, "Updated {{World}}");
		assertThat(render("Boot")).isEqualTo("Updated Boot");
		then(this.compiler).should(times(2)).compile(any(Reader.class));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void evictingViewReloadsCompiledTemplate(boolean clearAll, @TempDir Path directory) throws Exception {
		Path template = writeTemplate(directory, "Hello {{World}}");
		this.resolver.setCache(true);
		assertThat(render("Spring")).isEqualTo("Hello Spring");
		Files.writeString(template, "Updated {{World}}");
		assertThat(render("Boot")).isEqualTo("Hello Boot");
		if (clearAll) {
			this.resolver.clearCache();
		}
		else {
			this.resolver.removeFromCache("template", Locale.ROOT);
		}
		assertThat(render("Boot")).isEqualTo("Updated Boot");
		then(this.compiler).should(times(2)).compile(any(Reader.class));
	}

	@Test
	void failedCompilationCanBeRetried(@TempDir Path directory) throws Exception {
		Path template = writeTemplate(directory, "{{#unclosed}}");
		this.resolver.setCache(true);
		assertThatExceptionOfType(MustacheException.class).isThrownBy(() -> render("Spring"));
		Files.writeString(template, "Hello {{World}}");
		assertThat(render("Spring")).isEqualTo("Hello Spring");
		assertThat(render("Boot")).isEqualTo("Hello Boot");
		then(this.compiler).should(times(2)).compile(any(Reader.class));
	}

	@Test
	void concurrentRenderingCompilesOnceAndKeepsModelsSeparate(@TempDir Path directory) throws Exception {
		writeTemplate(directory, "Hello {{World}}");
		this.resolver.setCache(true);
		CountDownLatch rendering = new CountDownLatch(8);
		willAnswer((invocation) -> {
			assertThat(rendering.await(10, TimeUnit.SECONDS)).isTrue();
			return invocation.callRealMethod();
		}).given(this.compiler).compile(any(Reader.class));
		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Future<String>> results = new ArrayList<>();
			for (int i = 0; i < 8; i++) {
				String world = "world-" + i;
				results.add(executor.submit(() -> {
					rendering.countDown();
					return render(world);
				}));
			}
			for (int i = 0; i < results.size(); i++) {
				assertThat(results.get(i).get(10, TimeUnit.SECONDS)).isEqualTo("Hello world-" + i);
			}
			then(this.compiler).should().compile(any(Reader.class));
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	@WithResource(name = "template.html", content = "Hello {{World}}")
	void setsContentType() throws Exception {
		this.resolver.setContentType("application/octet-stream");
		View view = this.resolver.resolveViewName("template", Locale.ROOT);
		assertThat(view).isNotNull();
		assertThat(view.getContentType()).isEqualTo("application/octet-stream");

	}

	private String render(String world) throws Exception {
		View view = this.resolver.resolveViewName("template", Locale.ROOT);
		assertThat(view).isNotNull();
		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(Map.of("World", world), new MockHttpServletRequest(), response);
		return response.getContentAsString();
	}

	private Path writeTemplate(Path directory, String content) throws Exception {
		this.resolver.setPrefix(directory.toUri().toString());
		Path template = directory.resolve("template.html");
		Files.writeString(template, content);
		return template;
	}

}
