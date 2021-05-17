/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.jersey.JerseyEndpointResourceFactory;
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ClassUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

/**
 * {@link TestTemplateInvocationContextProvider} for
 * {@link WebEndpointTest @WebEndpointTest}.
 *
 * @author Andy Wilkinson
 */
class WebEndpointTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
			ExtensionContext extensionContext) {
		return Stream.of(
				new WebEndpointsInvocationContext("Jersey",
						WebEndpointTestInvocationContextProvider::createJerseyContext),
				new WebEndpointsInvocationContext("WebMvc",
						WebEndpointTestInvocationContextProvider::createWebMvcContext),
				new WebEndpointsInvocationContext("WebFlux",
						WebEndpointTestInvocationContextProvider::createWebFluxContext));
	}

	private static ConfigurableApplicationContext createJerseyContext(List<Class<?>> classes) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		classes.add(JerseyEndpointConfiguration.class);
		context.register(ClassUtils.toClassArray(classes));
		context.refresh();
		return context;
	}

	private static ConfigurableApplicationContext createWebMvcContext(List<Class<?>> classes) {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		classes.add(WebMvcEndpointConfiguration.class);
		context.register(ClassUtils.toClassArray(classes));
		context.refresh();
		return context;
	}

	private static ConfigurableApplicationContext createWebFluxContext(List<Class<?>> classes) {
		AnnotationConfigReactiveWebServerApplicationContext context = new AnnotationConfigReactiveWebServerApplicationContext();
		classes.add(WebFluxEndpointConfiguration.class);
		context.register(ClassUtils.toClassArray(classes));
		context.refresh();
		return context;
	}

	static class WebEndpointsInvocationContext
			implements TestTemplateInvocationContext, BeforeEachCallback, AfterEachCallback, ParameterResolver {

		private static final Duration TIMEOUT = Duration.ofMinutes(6);

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
					.of(extensionContext.getRequiredTestClass().getDeclaredClasses()).filter(this::isConfiguration)
					.collect(Collectors.toList());
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
			return WebTestClient.bindToServer().uriBuilderFactory(uriBuilderFactory).responseTimeout(TIMEOUT)
					.codecs((codecs) -> codecs.defaultCodecs().maxInMemorySize(-1)).filter((request, next) -> {
						if (HttpMethod.GET == request.method()) {
							return next.exchange(request).retry(10);
						}
						return next.exchange(request);
					}).build();
		}

		private int determinePort() {
			if (this.context instanceof AnnotationConfigServletWebServerApplicationContext) {
				return ((AnnotationConfigServletWebServerApplicationContext) this.context).getWebServer().getPort();
			}
			return this.context.getBean(PortHolder.class).getPort();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, JerseyAutoConfiguration.class })
	static class JerseyEndpointConfiguration {

		private final ApplicationContext applicationContext;

		JerseyEndpointConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

		@Bean
		ResourceConfigCustomizer webEndpointRegistrar() {
			return this::customize;
		}

		private void customize(ResourceConfig config) {
			EndpointMediaTypes endpointMediaTypes = EndpointMediaTypes.DEFAULT;
			WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(this.applicationContext,
					new ConversionServiceParameterValueMapper(), endpointMediaTypes, null, Collections.emptyList(),
					Collections.emptyList());
			Collection<Resource> resources = new JerseyEndpointResourceFactory().createEndpointResources(
					new EndpointMapping("/actuator"), discoverer.getEndpoints(), endpointMediaTypes,
					new EndpointLinksResolver(discoverer.getEndpoints()), true);
			config.registerResources(new HashSet<>(resources));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, WebFluxAutoConfiguration.class })
	static class WebFluxEndpointConfiguration implements ApplicationListener<WebServerInitializedEvent> {

		private final ApplicationContext applicationContext;

		private final PortHolder portHolder = new PortHolder();

		WebFluxEndpointConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		PortHolder portHolder() {
			return this.portHolder;
		}

		@Override
		public void onApplicationEvent(WebServerInitializedEvent event) {
			this.portHolder.setPort(event.getWebServer().getPort());
		}

		@Bean
		HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		WebFluxEndpointHandlerMapping webEndpointReactiveHandlerMapping() {
			EndpointMediaTypes endpointMediaTypes = EndpointMediaTypes.DEFAULT;
			WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(this.applicationContext,
					new ConversionServiceParameterValueMapper(), endpointMediaTypes, null, Collections.emptyList(),
					Collections.emptyList());
			return new WebFluxEndpointHandlerMapping(new EndpointMapping("/actuator"), discoverer.getEndpoints(),
					endpointMediaTypes, new CorsConfiguration(), new EndpointLinksResolver(discoverer.getEndpoints()),
					true);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
	static class WebMvcEndpointConfiguration {

		private final ApplicationContext applicationContext;

		WebMvcEndpointConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping() {
			EndpointMediaTypes endpointMediaTypes = EndpointMediaTypes.DEFAULT;
			WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(this.applicationContext,
					new ConversionServiceParameterValueMapper(), endpointMediaTypes, null, Collections.emptyList(),
					Collections.emptyList());
			return new WebMvcEndpointHandlerMapping(new EndpointMapping("/actuator"), discoverer.getEndpoints(),
					endpointMediaTypes, new CorsConfiguration(), new EndpointLinksResolver(discoverer.getEndpoints()),
					true);
		}

	}

}
