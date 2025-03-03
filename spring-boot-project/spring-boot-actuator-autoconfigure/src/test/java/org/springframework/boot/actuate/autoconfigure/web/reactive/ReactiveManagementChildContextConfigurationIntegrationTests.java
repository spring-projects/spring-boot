/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.catalina.Valve;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.tomcat.TomcatReactiveManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.server.reactive.tomcat.TomcatReactiveWebServerAutoConfiguration;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ReactiveManagementChildContextConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ReactiveManagementChildContextConfigurationIntegrationTests {

	private final List<WebServer> webServers = new ArrayList<>();

	private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner(
			AnnotationConfigReactiveWebServerApplicationContext::new)
		.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
				TomcatReactiveWebServerAutoConfiguration.class, TomcatReactiveManagementContextAutoConfiguration.class,
				WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
				WebFluxAutoConfiguration.class))
		.withUserConfiguration(SucceedingEndpoint.class)
		.withInitializer(new ServerPortInfoApplicationContextInitializer())
		.withInitializer((context) -> context.addApplicationListener(
				(ApplicationListener<WebServerInitializedEvent>) (event) -> this.webServers.add(event.getWebServer())))
		.withPropertyValues("server.port=0", "management.server.port=0", "management.endpoints.web.exposure.include=*");

	@TempDir
	Path temp;

	@Test
	void endpointsAreBeneathActuatorByDefault() {
		this.runner.withPropertyValues("management.server.port:0").run(withWebTestClient((client) -> {
			String body = client.get()
				.uri("actuator/success")
				.accept(MediaType.APPLICATION_JSON)
				.exchangeToMono((response) -> response.bodyToMono(String.class))
				.block();
			assertThat(body).isEqualTo("Success");
		}));
	}

	@Test
	void whenManagementServerBasePathIsConfiguredThenEndpointsAreBeneathThatPath() {
		this.runner.withPropertyValues("management.server.port:0", "management.server.base-path:/manage")
			.run(withWebTestClient((client) -> {
				String body = client.get()
					.uri("manage/actuator/success")
					.accept(MediaType.APPLICATION_JSON)
					.exchangeToMono((response) -> response.bodyToMono(String.class))
					.block();
				assertThat(body).isEqualTo("Success");
			}));
	}

	@Test // gh-32941
	void whenManagementServerPortLoadedFromConfigTree() {
		this.runner.withInitializer(this::addConfigTreePropertySource)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void accessLogHasManagementServerSpecificPrefix() {
		this.runner.withPropertyValues("server.tomcat.accesslog.enabled=true").run((context) -> {
			AccessLogValve accessLogValve = findAccessLogValve();
			assertThat(accessLogValve).isNotNull();
			assertThat(accessLogValve.getPrefix()).isEqualTo("management_access_log");
		});
	}

	private AccessLogValve findAccessLogValve() {
		assertThat(this.webServers).hasSize(2);
		Tomcat tomcat = ((TomcatWebServer) this.webServers.get(1)).getTomcat();
		for (Valve valve : tomcat.getEngine().getPipeline().getValves()) {
			if (valve instanceof AccessLogValve accessLogValve) {
				return accessLogValve;
			}
		}
		return null;
	}

	private void addConfigTreePropertySource(ConfigurableApplicationContext applicationContext) {
		try {
			applicationContext.getEnvironment()
				.setConversionService((ConfigurableConversionService) ApplicationConversionService.getSharedInstance());
			Path configtree = this.temp.resolve("configtree");
			Path file = configtree.resolve("management/server/port");
			file.toFile().getParentFile().mkdirs();
			FileCopyUtils.copy("0".getBytes(StandardCharsets.UTF_8), file.toFile());
			ConfigTreePropertySource source = new ConfigTreePropertySource("configtree", configtree);
			applicationContext.getEnvironment().getPropertySources().addFirst(source);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> withWebTestClient(Consumer<WebClient> webClient) {
		return (context) -> {
			String port = context.getEnvironment().getProperty("local.management.port");
			WebClient client = WebClient.create("http://localhost:" + port);
			webClient.accept(client);
		};
	}

	@Endpoint(id = "success")
	static class SucceedingEndpoint {

		@ReadOperation
		String fail() {
			return "Success";
		}

	}

}
