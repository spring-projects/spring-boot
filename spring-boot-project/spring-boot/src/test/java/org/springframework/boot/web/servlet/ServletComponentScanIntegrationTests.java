/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.testcomponents.TestMultipartServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletComponentScan @ServletComponentScan}
 *
 * @author Andy Wilkinson
 */
class ServletComponentScanIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@TempDir
	File temp;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("testConfiguration")
	void componentsAreRegistered(String serverName, Class<?> configuration) {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(configuration);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		String port = this.context.getEnvironment().getProperty("local.server.port");
		String response = new RestTemplate().getForObject("http://localhost:" + port + "/test", String.class);
		assertThat(response).isEqualTo("alpha bravo charlie");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("testConfiguration")
	void indexedComponentsAreRegistered(String serverName, Class<?> configuration) throws IOException {
		writeIndex(this.temp);
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { this.temp.toURI().toURL() },
				getClass().getClassLoader())) {
			this.context.setClassLoader(classLoader);
			this.context.register(configuration);
			new ServerPortInfoApplicationContextInitializer().initialize(this.context);
			this.context.refresh();
			String port = this.context.getEnvironment().getProperty("local.server.port");
			String response = new RestTemplate().getForObject("http://localhost:" + port + "/test", String.class);
			assertThat(response).isEqualTo("alpha bravo charlie");
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("testConfiguration")
	void multipartConfigIsHonoured(String serverName, Class<?> configuration) {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(configuration);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		@SuppressWarnings("rawtypes")
		Map<String, ServletRegistrationBean> beans = this.context.getBeansOfType(ServletRegistrationBean.class);
		ServletRegistrationBean<?> servletRegistrationBean = beans.get(TestMultipartServlet.class.getName());
		assertThat(servletRegistrationBean).isNotNull();
		MultipartConfigElement multipartConfig = servletRegistrationBean.getMultipartConfig();
		assertThat(multipartConfig).isNotNull();
		assertThat(multipartConfig.getLocation()).isEqualTo("test");
		assertThat(multipartConfig.getMaxRequestSize()).isEqualTo(2048);
		assertThat(multipartConfig.getMaxFileSize()).isEqualTo(1024);
		assertThat(multipartConfig.getFileSizeThreshold()).isEqualTo(512);
	}

	private void writeIndex(File temp) throws IOException {
		File metaInf = new File(temp, "META-INF");
		metaInf.mkdirs();
		Properties index = new Properties();
		index.setProperty("org.springframework.boot.web.servlet.testcomponents.TestFilter", WebFilter.class.getName());
		index.setProperty("org.springframework.boot.web.servlet.testcomponents.TestListener",
				WebListener.class.getName());
		index.setProperty("org.springframework.boot.web.servlet.testcomponents.TestServlet",
				WebServlet.class.getName());
		try (FileWriter writer = new FileWriter(new File(metaInf, "spring.components"))) {
			index.store(writer, null);
		}
	}

	static Stream<Arguments> testConfiguration() {
		return Stream.of(Arguments.of("Jetty", JettyTestConfiguration.class),
				Arguments.of("Tomcat", TomcatTestConfiguration.class),
				Arguments.of("Undertow", UndertowTestConfiguration.class));
	}

	@ServletComponentScan(basePackages = "org.springframework.boot.web.servlet.testcomponents")
	abstract static class AbstractTestConfiguration {

		@Bean
		protected ServletWebServerFactory webServerFactory(ObjectProvider<WebListenerRegistrar> webListenerRegistrars) {
			ConfigurableServletWebServerFactory factory = createWebServerFactory();
			webListenerRegistrars.orderedStream().forEach((registrar) -> registrar.register(factory));
			return factory;
		}

		abstract ConfigurableServletWebServerFactory createWebServerFactory();

	}

	@Configuration(proxyBeanMethods = false)
	static class JettyTestConfiguration extends AbstractTestConfiguration {

		@Override
		ConfigurableServletWebServerFactory createWebServerFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatTestConfiguration extends AbstractTestConfiguration {

		@Override
		ConfigurableServletWebServerFactory createWebServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UndertowTestConfiguration extends AbstractTestConfiguration {

		@Override
		ConfigurableServletWebServerFactory createWebServerFactory() {
			return new UndertowServletWebServerFactory(0);
		}

	}

}
