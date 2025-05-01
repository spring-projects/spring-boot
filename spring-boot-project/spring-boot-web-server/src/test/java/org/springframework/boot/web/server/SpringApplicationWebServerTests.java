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

package org.springframework.boot.web.server;

import java.util.Iterator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.boot.web.context.reactive.StandardReactiveWebEnvironment;
import org.springframework.boot.web.server.reactive.MockReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.servlet.MockServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplication} with a {@link WebServer}.
 *
 * @author Andy wilkinson
 */
class SpringApplicationWebServerTests {

	private String headlessProperty;

	private ConfigurableApplicationContext context;

	@BeforeEach
	void storeAndClearHeadlessProperty() {
		this.headlessProperty = System.getProperty("java.awt.headless");
		System.clearProperty("java.awt.headless");
	}

	@AfterEach
	void reinstateHeadlessProperty() {
		if (this.headlessProperty == null) {
			System.clearProperty("java.awt.headless");
		}
		else {
			System.setProperty("java.awt.headless", this.headlessProperty);
		}
	}

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("spring.main.banner-mode");
	}

	@Test
	void defaultApplicationContextForWeb() {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigServletWebServerApplicationContext.class);
	}

	@Test
	void defaultApplicationContextForReactiveWeb() {
		SpringApplication application = new SpringApplication(ExampleReactiveWebConfig.class);
		application.setWebApplicationType(WebApplicationType.REACTIVE);
		this.context = application.run();
		assertThat(this.context).isInstanceOf(AnnotationConfigReactiveWebServerApplicationContext.class);
	}

	@Test
	void environmentForWeb() {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		this.context = application.run();
		assertThat(this.context.getEnvironment()).isInstanceOf(StandardServletEnvironment.class);
		assertThat(this.context.getEnvironment().getClass().getName()).endsWith("ApplicationServletEnvironment");
	}

	@Test
	void environmentForReactiveWeb() {
		SpringApplication application = new SpringApplication(ExampleReactiveWebConfig.class);
		application.setWebApplicationType(WebApplicationType.REACTIVE);
		this.context = application.run();
		assertThat(this.context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
		assertThat(this.context.getEnvironment().getClass().getName()).endsWith("ApplicationReactiveWebEnvironment");
	}

	@Test
	void webApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleWebConfig.class)
			.run("--spring.main.web-application-type=servlet");
		assertThat(context).isInstanceOf(WebApplicationContext.class);
		assertThat(context.getEnvironment()).isInstanceOf(StandardServletEnvironment.class);
		assertThat(context.getEnvironment().getClass().getName()).endsWith("ApplicationServletEnvironment");
	}

	@Test
	void reactiveApplicationConfiguredViaAPropertyHasTheCorrectTypeOfContextAndEnvironment() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleReactiveWebConfig.class)
			.run("--spring.main.web-application-type=reactive");
		assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
		assertThat(context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
		assertThat(context.getEnvironment().getClass().getName()).endsWith("ApplicationReactiveWebEnvironment");
	}

	@Test
	@WithResource(name = "application-withwebapplicationtype.properties",
			content = "spring.main.web-application-type=reactive")
	void environmentIsConvertedIfTypeDoesNotMatch() {
		ConfigurableApplicationContext context = new SpringApplication(ExampleReactiveWebConfig.class)
			.run("--spring.profiles.active=withwebapplicationtype");
		assertThat(context).isInstanceOf(ReactiveWebApplicationContext.class);
		assertThat(context.getEnvironment()).isInstanceOf(StandardReactiveWebEnvironment.class);
		assertThat(context.getEnvironment().getClass().getName()).endsWith("ApplicationReactiveWebEnvironment");
	}

	@Test
	void webApplicationSwitchedOffInListener() {
		SpringApplication application = new SpringApplication(ExampleWebConfig.class);
		application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) (event) -> {
			assertThat(event.getEnvironment().getClass().getName()).endsWith("ApplicationServletEnvironment");
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(event.getEnvironment(), "foo=bar");
			event.getSpringApplication().setWebApplicationType(WebApplicationType.NONE);
		});
		this.context = application.run();
		assertThat(this.context.getEnvironment()).isNotInstanceOf(StandardServletEnvironment.class);
		assertThat(this.context.getEnvironment().getProperty("foo")).isEqualTo("bar");
		Iterator<PropertySource<?>> iterator = this.context.getEnvironment().getPropertySources().iterator();
		assertThat(iterator.next().getName()).isEqualTo("configurationProperties");
		assertThat(iterator.next().getName())
			.isEqualTo(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleWebConfig {

		@Bean
		MockServletWebServerFactory webServer() {
			return new MockServletWebServerFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExampleReactiveWebConfig {

		@Bean
		MockReactiveWebServerFactory webServerFactory() {
			return new MockReactiveWebServerFactory();
		}

		@Bean
		HttpHandler httpHandler() {
			return (serverHttpRequest, serverHttpResponse) -> Mono.empty();
		}

	}

}
