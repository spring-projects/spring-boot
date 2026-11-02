/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.ThrowingConsumer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WelcomePageHandlerMapping}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Diego Magalhães
 */
@ExtendWith(OutputCaptureExtension.class)
class WelcomePageHandlerMappingTests {

	private static final String INVALID_ACCEPT_HEADER_WARNING = "Received invalid Accept header. Assuming all media types are accepted";

	private static final String LOGGER_NAME = "org.springframework.boot.webmvc.autoconfigure.WelcomePageHandlerMapping";

	private static final String WELCOME_PAGE_LOCATION = "spring.web.resources.static-locations:classpath:/welcome-page/";

	private static final String WELCOME_PAGE_VIEW_NAME = "forward:index.html";

	private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

	private static final String STYLESHEET_ACCEPT = "text/css,*/*;q=0.1";

	private static final String IMAGE_ACCEPT = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8";

	private static final String JSON_ACCEPT = "application/json";

	private static final String LONG_ACCEPT_PREFIX = longAcceptPrefix();

	private static final String LONG_HTML_ACCEPT = LONG_ACCEPT_PREFIX + "text/html";

	private static final String LONG_JSON_ACCEPT = LONG_ACCEPT_PREFIX + "application/json";

	private static final String LONG_INVALID_ACCEPT = LONG_HTML_ACCEPT + ",*/*q=0.8";

	private static final List<String> VALID_ACCEPT_HEADERS = List.of("", " ", "*/*", "text/html", "TEXT/HTML", "text/*",
			"text/html;q=0", BROWSER_ACCEPT, STYLESHEET_ACCEPT, IMAGE_ACCEPT, "application/json, text/html",
			JSON_ACCEPT, "application/*", ",", LONG_HTML_ACCEPT, LONG_JSON_ACCEPT);

	private static final String[] CONCURRENT_PATHS = { "/", "/x" };

	private static final int CONCURRENT_THREADS = 16;

	private static final int CONCURRENT_CALLS = 10000;

	private static final String[] GENERATED_SEPARATORS = { ",", ", ", " ,", " , ", ",  " };

	private static final String[] GENERATED_INVALID_ATOMS = { "*/*q=0.8", "text/html;q=abc", "text/html;q=2", "text/",
			"text/\u00e9" };

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HandlerMappingConfiguration.class)
		.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class));

	private final WebApplicationContextRunner autoConfiguredContextRunner = new WebApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
						HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

	@Test
	void isOrderedAtLowPriority() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class).run((context) -> {
			WelcomePageHandlerMapping handler = context.getBean(WelcomePageHandlerMapping.class);
			assertThat(handler.getOrder()).isEqualTo(2);
		});
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML)).hasStatusOk()
				.hasForwardedUrl("index.html")));
	}

	@Test
	void handlesRequestForStaticPageThatAcceptsAll() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.ALL)).hasStatusOk()
				.hasForwardedUrl("index.html")));
	}

	@Test
	void doesNotHandleRequestThatDoesNotAcceptTextHtml() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.APPLICATION_JSON))
				.hasStatus(HttpStatus.NOT_FOUND)));
	}

	@Test
	void handlesRequestWithNoAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/")).hasStatusOk().hasForwardedUrl("index.html")));
	}

	@Test
	void handlesRequestWithEmptyAcceptHeader() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").header(HttpHeaders.ACCEPT, "")).hasStatusOk()
				.hasForwardedUrl("index.html")));
	}

	@Test
	void rootHandlerIsNotRegisteredWhenStaticPathPatternIsNotSlashStarStar() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class)
			.withPropertyValues("static-path-pattern=/foo/**")
			.run((context) -> assertThat(context.getBean(WelcomePageHandlerMapping.class).getRootHandler()).isNull());
	}

	@Test
	void producesNotFoundResponseWhenThereIsNoWelcomePage() {
		this.contextRunner.run(testWith(
				(mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML)).hasStatus(HttpStatus.NOT_FOUND)));
	}

	@Test
	void handlesRequestForTemplateThatAcceptsTextHtml() {
		this.contextRunner.withUserConfiguration(TemplateConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML)).hasStatusOk()
				.hasBodyTextEqualTo("index template")));
	}

	@Test
	void handlesRequestForTemplateThatAcceptsAll() {
		this.contextRunner.withUserConfiguration(TemplateConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.ALL)).hasStatusOk()
				.hasBodyTextEqualTo("index template")));
	}

	@Test
	void prefersAStaticResourceToATemplate() {
		this.contextRunner.withUserConfiguration(StaticResourceConfiguration.class, TemplateConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept(MediaType.ALL)).hasStatusOk()
				.hasForwardedUrl("index.html")));
	}

	@Test
	void logsInvalidAcceptHeader(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TemplateConfiguration.class)
			.run(testWith((mvc) -> assertThat(mvc.get().uri("/").accept("*/*q=0.8")).hasStatusOk()
				.hasBodyTextEqualTo("index template")));
		assertThat(output).contains("Received invalid Accept header. Assuming all media types are accepted");
	}

	@Test
	void whenNoWelcomePageUnmatchedRequestIsNotFoundForAnyAcceptHeader(CapturedOutput output) {
		this.autoConfiguredContextRunner.run(testWith((mvc) -> {
			for (String path : List.of("/missing", "/")) {
				for (int i = 0; i < 3; i++) {
					assertThat(get(mvc, path, null)).hasStatus(HttpStatus.NOT_FOUND);
				}
				for (String accept : VALID_ACCEPT_HEADERS) {
					for (int i = 0; i < 3; i++) {
						assertThat(get(mvc, path, accept)).as(path + " " + accept).hasStatus(HttpStatus.NOT_FOUND);
					}
				}
			}
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isZero();
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void whenWelcomePageRepeatedAcceptHeadersGiveSameResult(CapturedOutput output) {
		this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run(testWith((mvc) -> {
			assertWelcomePage(mvc, BROWSER_ACCEPT, 2);
			assertNotAcceptable(mvc, JSON_ACCEPT, 2);
			assertWelcomePage(mvc, BROWSER_ACCEPT, 2);
			assertNotAcceptable(mvc, JSON_ACCEPT, 2);
			assertWelcomePage(mvc, "*/*", 2);
			assertWelcomePage(mvc, STYLESHEET_ACCEPT, 2);
			assertWelcomePage(mvc, "TEXT/HTML", 2);
			assertWelcomePage(mvc, "text/html;q=0", 2);
			assertWelcomePage(mvc, "application/json, text/html", 2);
			assertNotAcceptable(mvc, "application/*", 2);
			assertNotAcceptable(mvc, ",", 2);
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isZero();
	}

	@Test
	void whenNoWelcomePageInvalidAcceptHeaderLogsWarningOnEveryRequest(CapturedOutput output) {
		this.autoConfiguredContextRunner.run(testWith((mvc) -> {
			assertThat(get(mvc, "/missing", "*/*q=0.8")).hasStatus(HttpStatus.NOT_FOUND);
			assertThat(get(mvc, "/missing", "*/*q=0.8")).hasStatus(HttpStatus.NOT_FOUND);
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isEqualTo(2);
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void whenWelcomePageInvalidAcceptHeaderLogsWarningOnEveryRequest(CapturedOutput output) {
		this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run(testWith((mvc) -> {
			assertWelcomePage(mvc, BROWSER_ACCEPT, 1);
			assertWelcomePage(mvc, "*/*q=0.8", 1);
			assertNotAcceptable(mvc, JSON_ACCEPT, 1);
			assertWelcomePage(mvc, "*/*q=0.8", 1);
			assertWelcomePage(mvc, "text/html;q=abc", 1);
			assertWelcomePage(mvc, "text/html;q=2", 1);
			assertWelcomePage(mvc, "text/", 1);
			assertWelcomePage(mvc, "text/\u00e9", 1);
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isEqualTo(6);
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void whenAcceptHeaderIsLongResultIsUnchanged(CapturedOutput output) {
		assertThat(LONG_HTML_ACCEPT).hasSize(345);
		assertThat(LONG_JSON_ACCEPT).hasSize(352);
		assertThat(LONG_INVALID_ACCEPT).hasSize(354);
		this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run(testWith((mvc) -> {
			assertWelcomePage(mvc, LONG_HTML_ACCEPT, 2);
			assertNotAcceptable(mvc, LONG_JSON_ACCEPT, 2);
			assertWelcomePage(mvc, LONG_INVALID_ACCEPT, 2);
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isEqualTo(2);
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void whenAcceptHeaderIsMissingOrBlankWelcomePageIsServed(CapturedOutput output) {
		this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run(testWith((mvc) -> {
			assertThat(get(mvc, "/", null)).hasStatusOk().hasForwardedUrl("index.html");
			assertWelcomePage(mvc, "", 1);
			assertWelcomePage(mvc, " ", 1);
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isZero();
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void getHandlerFromManyThreadsGivesSameResults() {
		withWelcomePageLoggerAtInfo(
				() -> this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run((context) -> {
					HandlerMapping mapping = context.getBean("welcomePageHandlerMapping", HandlerMapping.class);
					Map<ConcurrentInput, @Nullable String> expected = new HashMap<>();
					for (int thread = 0; thread < CONCURRENT_THREADS; thread++) {
						for (int call = 0; call < CONCURRENT_CALLS; call++) {
							ConcurrentInput input = concurrentInput(thread, call);
							if (!expected.containsKey(input)) {
								String viewName = getHandlerViewName(mapping, input.path(), input.accept());
								assertThat(viewName).as(input.toString()).isEqualTo(expectedViewName(input));
								expected.put(input, viewName);
							}
						}
					}
					assertThat(expected).hasSize(CONCURRENT_THREADS * 128 * CONCURRENT_PATHS.length + 12);
					ConcurrentLinkedQueue<String> mismatches = new ConcurrentLinkedQueue<>();
					CountDownLatch ready = new CountDownLatch(CONCURRENT_THREADS);
					List<Callable<Integer>> workers = new ArrayList<>();
					for (int thread = 0; thread < CONCURRENT_THREADS; thread++) {
						int workerThread = thread;
						workers.add(() -> {
							ready.countDown();
							ready.await();
							for (int call = 0; call < CONCURRENT_CALLS; call++) {
								ConcurrentInput input = concurrentInput(workerThread, call);
								String viewName = getHandlerViewName(mapping, input.path(), input.accept());
								if (!Objects.equals(viewName, expected.get(input))) {
									mismatches
										.add(workerThread + ":" + call + ":" + input.path() + ":" + input.accept());
								}
							}
							return CONCURRENT_CALLS;
						});
					}
					ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
					try {
						List<Future<Integer>> results = executor.invokeAll(workers, 60, TimeUnit.SECONDS);
						for (Future<Integer> result : results) {
							assertThat(result.get()).isEqualTo(CONCURRENT_CALLS);
						}
					}
					finally {
						executor.shutdown();
					}
					assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
					assertThat(mismatches).isEmpty();
				}));
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void whenManyDistinctAcceptHeadersAreSentResultsAreUnchanged(CapturedOutput output) {
		withWelcomePageLoggerAtInfo(
				() -> this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run(testWith((mvc) -> {
					for (int n = 0; n < 80; n++) {
						assertDistinctAcceptHeader(mvc, n);
					}
					for (int n = 0; n < 10; n++) {
						assertDistinctAcceptHeader(mvc, n);
					}
				})));
		assertThat(invalidAcceptHeaderWarnings(output)).isZero();
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	void getHandlerMatchesMediaTypeParsingForGeneratedHeaders(CapturedOutput output) {
		Random random = new Random(37118);
		List<String> pool = new ArrayList<>();
		for (int i = 0; i < 600; i++) {
			pool.add(generatedAcceptHeader(random));
		}
		List<String> headers = new ArrayList<>();
		for (int i = 0; i < 10000; i++) {
			headers.add(pool.get(random.nextInt((i % 2 == 0) ? 16 : pool.size())));
		}
		List<Boolean> expected = new ArrayList<>();
		int expectedWarnings = 0;
		for (String header : headers) {
			if (!StringUtils.hasText(header)) {
				expected.add(true);
				continue;
			}
			try {
				expected.add(includesTextHtml(MediaType.parseMediaTypes(header)));
			}
			catch (InvalidMediaTypeException ex) {
				expected.add(true);
				expectedWarnings++;
			}
		}
		assertThat(expectedWarnings).isPositive();
		assertThat(expected).contains(true, false);
		assertThat(headers).anyMatch((header) -> header.length() > 256);
		List<String> mismatches = new ArrayList<>();
		withWelcomePageLoggerAtInfo(
				() -> this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run((context) -> {
					HandlerMapping mapping = context.getBean("welcomePageHandlerMapping", HandlerMapping.class);
					for (int i = 0; i < headers.size(); i++) {
						String header = headers.get(i);
						boolean actual = getHandlerViewName(mapping, "/", header) != null;
						if (actual != expected.get(i)) {
							mismatches.add(i + ":" + header + ":" + expected.get(i) + ":" + actual);
						}
					}
				}));
		assertThat(mismatches).isEmpty();
		assertThat(invalidAcceptHeaderWarnings(output)).isEqualTo(expectedWarnings);
	}

	@Test
	@WithResource(name = "welcome-page/index.html", content = "welcome-page-static")
	@WithResource(name = "welcome-page/app.css", content = "app-css")
	void whenStaticResourceIsRequestedAcceptHeaderIsStillChecked(CapturedOutput output) {
		this.autoConfiguredContextRunner.withPropertyValues(WELCOME_PAGE_LOCATION).run(testWith((mvc) -> {
			assertThat(get(mvc, "/app.css", STYLESHEET_ACCEPT)).hasStatusOk().hasBodyTextEqualTo("app-css");
			assertThat(get(mvc, "/app.css", BROWSER_ACCEPT)).hasStatusOk().hasBodyTextEqualTo("app-css");
			assertThat(get(mvc, "/app.css", "*/*q=0.8")).hasStatusOk().hasBodyTextEqualTo("app-css");
		}));
		assertThat(invalidAcceptHeaderWarnings(output)).isOne();
	}

	private ContextConsumer<AssertableWebApplicationContext> testWith(ThrowingConsumer<MockMvcTester> mvc) {
		return (context) -> mvc.accept(MockMvcTester.from(context));
	}

	private void withWelcomePageLoggerAtInfo(Runnable action) {
		LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
		loggingSystem.setLogLevel(LOGGER_NAME, LogLevel.INFO);
		try {
			action.run();
		}
		finally {
			loggingSystem.setLogLevel(LOGGER_NAME, null);
		}
	}

	private static MockMvcRequestBuilder get(MockMvcTester mvc, String path, @Nullable String accept) {
		MockMvcRequestBuilder request = mvc.get().uri(path);
		return (accept != null) ? request.header(HttpHeaders.ACCEPT, accept) : request;
	}

	private static void assertWelcomePage(MockMvcTester mvc, String accept, int times) {
		for (int i = 0; i < times; i++) {
			assertThat(get(mvc, "/", accept)).as(accept).hasStatusOk().hasForwardedUrl("index.html");
		}
	}

	private static void assertNotAcceptable(MockMvcTester mvc, String accept, int times) {
		for (int i = 0; i < times; i++) {
			assertThat(get(mvc, "/", accept)).as(accept).hasStatus(HttpStatus.NOT_ACCEPTABLE);
		}
	}

	private static void assertDistinctAcceptHeader(MockMvcTester mvc, int n) {
		if (n % 2 == 0) {
			assertNotAcceptable(mvc, "application/x-t" + n, 1);
		}
		else {
			assertWelcomePage(mvc, "text/html;level=" + n, 1);
		}
	}

	private static int invalidAcceptHeaderWarnings(CapturedOutput output) {
		return StringUtils.countOccurrencesOf(output.toString(), INVALID_ACCEPT_HEADER_WARNING);
	}

	private static @Nullable String getHandlerViewName(HandlerMapping mapping, String path, @Nullable String accept)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		if (accept != null) {
			request.addHeader(HttpHeaders.ACCEPT, accept);
		}
		ServletRequestPathUtils.parseAndCache(request);
		HandlerExecutionChain chain = mapping.getHandler(request);
		return (chain != null) ? ((ParameterizableViewController) chain.getHandler()).getViewName() : null;
	}

	private static ConcurrentInput concurrentInput(int thread, int call) {
		int index = call / 3;
		if (call % 3 == 2) {
			String path = CONCURRENT_PATHS[(index / 128) % 2];
			String suffix = thread + "-" + ((index / 2) % 64);
			return new ConcurrentInput(path,
					(index % 2 == 0) ? "application/x-" + suffix : "text/html;level=" + suffix);
		}
		int combination = (call - index + 5 * thread) % 12;
		String path = CONCURRENT_PATHS[combination % 2];
		String accept = switch (combination / 2) {
			case 0 -> BROWSER_ACCEPT;
			case 1 -> JSON_ACCEPT;
			case 2 -> "*/*";
			case 3 -> STYLESHEET_ACCEPT;
			case 4 -> null;
			default -> ",";
		};
		return new ConcurrentInput(path, accept);
	}

	private static @Nullable String expectedViewName(ConcurrentInput input) {
		String accept = input.accept();
		boolean acceptsHtml = accept == null || accept.equals(BROWSER_ACCEPT) || accept.equals("*/*")
				|| accept.equals(STYLESHEET_ACCEPT) || accept.startsWith("text/html;level=");
		return ("/".equals(input.path()) && acceptsHtml) ? WELCOME_PAGE_VIEW_NAME : null;
	}

	private static String generatedAcceptHeader(Random random) {
		StringBuilder header = new StringBuilder();
		if (random.nextInt(25) == 0) {
			int count = 12 + random.nextInt(4);
			for (int i = 0; i < count; i++) {
				int n = random.nextInt(100);
				header.append("application/x-perf-").append((n < 10) ? "0" : "").append(n).append(";q=0.5,");
			}
			return header.append(generatedAcceptAtom(random)).toString();
		}
		if (random.nextInt(10) == 0) {
			header.append(" ");
		}
		int atoms = 1 + random.nextInt(4);
		for (int i = 0; i < atoms; i++) {
			if (i > 0) {
				header.append(GENERATED_SEPARATORS[random.nextInt(GENERATED_SEPARATORS.length)]);
			}
			header.append(generatedAcceptAtom(random));
		}
		if (random.nextInt(10) == 0) {
			header.append(" ");
		}
		return header.toString();
	}

	private static String generatedAcceptAtom(Random random) {
		String atom;
		if (random.nextInt(10) == 0) {
			atom = GENERATED_INVALID_ATOMS[random.nextInt(GENERATED_INVALID_ATOMS.length)];
		}
		else {
			int kind = random.nextInt(14);
			atom = switch (kind) {
				case 0 -> BROWSER_ACCEPT;
				case 1 -> STYLESHEET_ACCEPT;
				case 2 -> IMAGE_ACCEPT;
				case 3 -> ",";
				case 4 -> "*/*";
				case 5 -> "text/*";
				case 6 -> "text/html";
				case 7 -> JSON_ACCEPT;
				case 8 -> "application/*";
				case 9 -> "application/xhtml+xml";
				case 10 -> "application/vnd.perf+json";
				case 11 -> "text/plain;charset=utf-8";
				case 12 -> "text/html;level=" + random.nextInt(1000);
				default -> "application/x-perf-" + random.nextInt(1000);
			};
			if (kind >= 4 && random.nextInt(4) == 0) {
				atom = atom + ";q=0." + random.nextInt(10);
			}
		}
		if (random.nextInt(4) == 0) {
			atom = atom.toUpperCase(Locale.ROOT);
		}
		return atom;
	}

	private static boolean includesTextHtml(List<MediaType> mediaTypes) {
		for (MediaType mediaType : mediaTypes) {
			if (mediaType.includes(MediaType.TEXT_HTML)) {
				return true;
			}
		}
		return false;
	}

	private static String longAcceptPrefix() {
		StringBuilder prefix = new StringBuilder();
		for (int i = 0; i < 12; i++) {
			prefix.append("application/x-perf-").append((i < 10) ? "0" : "").append(i).append(";q=0.5,");
		}
		return prefix.toString();
	}

	@Configuration(proxyBeanMethods = false)
	static class HandlerMappingConfiguration {

		@Bean
		WelcomePageHandlerMapping handlerMapping(ApplicationContext applicationContext,
				ObjectProvider<TemplateAvailabilityProviders> templateAvailabilityProviders,
				ObjectProvider<Resource> staticIndexPage,
				@Value("${static-path-pattern:/**}") String staticPathPattern) {
			return new WelcomePageHandlerMapping(
					templateAvailabilityProviders
						.getIfAvailable(() -> new TemplateAvailabilityProviders(applicationContext)),
					applicationContext, staticIndexPage.getIfAvailable(), staticPathPattern);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StaticResourceConfiguration {

		@Bean
		Resource staticIndexPage() {
			return new ByteArrayResource("welcome-page-static".getBytes(StandardCharsets.UTF_8));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TemplateConfiguration {

		@Bean
		TemplateAvailabilityProviders templateAvailabilityProviders() {
			return new TestTemplateAvailabilityProviders(
					(view, environment, classLoader, resourceLoader) -> view.equals("index"));
		}

		@Bean
		ViewResolver viewResolver() {
			return (name, locale) -> {
				if (name.startsWith("forward:")) {
					return new InternalResourceView(name.substring("forward:".length()));
				}
				return new AbstractView() {

					@Override
					protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
							HttpServletResponse response) throws Exception {
						response.getWriter().print(name + " template");
					}

				};
			};
		}

	}

	static class TestTemplateAvailabilityProviders extends TemplateAvailabilityProviders {

		TestTemplateAvailabilityProviders(TemplateAvailabilityProvider provider) {
			super(Collections.singletonList(provider));
		}

	}

	record ConcurrentInput(String path, @Nullable String accept) {

	}

}
