/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

/**
 * {@link TestTemplateInvocationContextProvider} for
 * {@link WebEndpointTest @WebEndpointTest}.
 *
 * @author Andy Wilkinson`
 * @author Stephane Nicoll
 */
class WebEndpointTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

	private final Map<Infrastructure, List<Class<?>>> infrastructures;

	private final Map<Infrastructure, Function<List<Class<?>>, ConfigurableApplicationContext>> contextFactories;

	WebEndpointTestInvocationContextProvider() {
		this.infrastructures = new HashMap<>();
		List<WebEndpointInfrastructureProvider> providers = SpringFactoriesLoader
			.loadFactories(WebEndpointInfrastructureProvider.class, getClass().getClassLoader());
		Stream.of(Infrastructure.values())
			.forEach((infrastructure) -> providers.stream()
				.filter((provider) -> provider.supports(infrastructure))
				.findFirst()
				.ifPresent((provider) -> this.infrastructures.put(infrastructure,
						provider.getInfrastructureConfiguration(infrastructure))));
		this.contextFactories = Map.of(Infrastructure.JERSEY, this::createJerseyContext, Infrastructure.MVC,
				this::createWebMvcContext, Infrastructure.WEBFLUX, this::createWebFluxContext);
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
			ExtensionContext extensionContext) {
		WebEndpointTest webEndpointTest = AnnotationUtils
			.findAnnotation(extensionContext.getRequiredTestMethod(), WebEndpointTest.class)
			.orElseThrow(() -> new IllegalStateException("Unable to find WebEndpointTest annotation on %s"
				.formatted(extensionContext.getRequiredTestMethod())));
		return Stream.of(webEndpointTest.infrastructure()).distinct().map(this::createInvocationContext);
	}

	private WebEndpointsInvocationContext createInvocationContext(Infrastructure infrastructure) {
		return infrastructure.createInvocationContext(this.contextFactories.get(infrastructure));
	}

	private ConfigurableApplicationContext createJerseyContext(List<Class<?>> classes) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		classes.addAll(getEndpointInfrastructureConfiguration(Infrastructure.JERSEY));
		context.register(ClassUtils.toClassArray(classes));
		context.refresh();
		return context;
	}

	private ConfigurableApplicationContext createWebMvcContext(List<Class<?>> classes) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		classes.addAll(getEndpointInfrastructureConfiguration(Infrastructure.MVC));
		context.register(ClassUtils.toClassArray(classes));
		context.refresh();
		return context;
	}

	private ConfigurableApplicationContext createWebFluxContext(List<Class<?>> classes) {
		AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
		classes.addAll(getEndpointInfrastructureConfiguration(Infrastructure.WEBFLUX));
		context.register(ClassUtils.toClassArray(classes));
		context.refresh();
		return context;
	}

	private List<Class<?>> getEndpointInfrastructureConfiguration(Infrastructure infrastructure) {
		List<Class<?>> configurationClasses = new ArrayList<>();
		configurationClasses.add(EndpointBaseConfiguration.class);
		List<Class<?>> endpointConfiguration = this.infrastructures.get(infrastructure);
		if (endpointConfiguration == null) {
			throw new IllegalStateException(
					"No endpoint infrastructure configuration found for " + infrastructure.name());
		}
		configurationClasses.addAll(endpointConfiguration);
		return configurationClasses;
	}

	static class WebEndpointsInvocationContext
			implements TestTemplateInvocationContext, BeforeEachCallback, AfterEachCallback, ParameterResolver {

		private static final Duration TIMEOUT = Duration.ofMinutes(5);

		private final String name;

		private final Function<List<Class<?>>, ConfigurableApplicationContext> contextFactory;

		private ConfigurableApplicationContext context;

		<T extends ConfigurableApplicationContext & AnnotationConfigRegistry> WebEndpointsInvocationContext(String name,
				Function<List<Class<?>>, ConfigurableApplicationContext> contextFactory) {
			this.name = name;
			this.contextFactory = contextFactory;
		}

		@Override
		public void beforeEach(ExtensionContext extensionContext) throws Exception {
			List<Class<?>> configurationClasses = Stream
				.of(extensionContext.getRequiredTestClass().getDeclaredClasses())
				.filter(this::isConfiguration)
				.collect(Collectors.toCollection(ArrayList::new));
			this.context = this.contextFactory.apply(configurationClasses);
		}

		private boolean isConfiguration(Class<?> candidate) {
			return MergedAnnotations.from(candidate, SearchStrategy.TYPE_HIERARCHY).isPresent(Configuration.class);
		}

		@Override
		public void afterEach(ExtensionContext context) throws Exception {
			if (this.context != null) {
				this.context.close();
			}
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			Class<?> type = parameterContext.getParameter().getType();
			return type.equals(WebTestClient.class) || type.isAssignableFrom(ConfigurableApplicationContext.class);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			Class<?> type = parameterContext.getParameter().getType();
			if (type.equals(WebTestClient.class)) {
				return createWebTestClient();
			}
			else {
				return this.context;
			}
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return Collections.singletonList(this);
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return this.name;
		}

		private WebTestClient createWebTestClient() {
			DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(
					"http://localhost:" + determinePort());
			uriBuilderFactory.setEncodingMode(EncodingMode.NONE);
			return WebTestClient.bindToServer()
				.uriBuilderFactory(uriBuilderFactory)
				.responseTimeout(TIMEOUT)
				.codecs((codecs) -> codecs.defaultCodecs().maxInMemorySize(-1))
				.filter((request, next) -> {
					if (HttpMethod.GET == request.method()) {
						return next.exchange(request).retry(10);
					}
					return next.exchange(request);
				})
				.build();
		}

		private int determinePort() {
			return this.context.getBean(PortHolder.class).getPort();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointBaseConfiguration implements ApplicationListener<WebServerInitializedEvent> {

		private final PortHolder portHolder = new PortHolder();

		@Bean
		PortHolder portHolder() {
			return this.portHolder;
		}

		@Override
		public void onApplicationEvent(WebServerInitializedEvent event) {
			this.portHolder.setPort(event.getWebServer().getPort());
		}

	}

	private static final class PortHolder {

		private int port;

		private int getPort() {
			return this.port;
		}

		private void setPort(int port) {
			this.port = port;
		}

	}

}
